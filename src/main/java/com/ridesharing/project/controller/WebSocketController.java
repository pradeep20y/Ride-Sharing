package com.ridesharing.project.controller;

import com.ridesharing.project.dto.request.LocationUpdateRequest;
import com.ridesharing.project.dto.request.RideAcceptRequest;
import com.ridesharing.project.dto.request.RideRejectRequest;
import com.ridesharing.project.entity.Ride;
import com.ridesharing.project.service.LocationService;
import com.ridesharing.project.service.RideMatchingService;
import com.ridesharing.project.service.RideNotificationService;
import com.ridesharing.project.service.RideService;
import jakarta.persistence.OptimisticLockException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

// Handles all inbound WebSocket (STOMP) messages from connected drivers.
// Delegates entirely to service-layer classes — no repositories, no Redis access,
// no business logic lives here. Each handler is a thin bridge between the STOMP
// message and the appropriate service method.
// Interacts with: RideMatchingService (accept/reject flow), RideNotificationService
// (passenger notification and offer-expired feedback), LocationService and RideService
// (live location tracking during active rides).
@Controller
public class WebSocketController {

    // RideMatchingService handles the full accept and reject state transitions,
    // including creating the Ride record and updating driver status on accept
    @Autowired
    private RideMatchingService rideMatchingService;

    // RideNotificationService sends all outbound WebSocket messages — used here to
    // notify the passenger when a driver accepts and to notify the driver if their
    // accept arrived too late (OptimisticLockException from @Version conflict)
    @Autowired
    private RideNotificationService rideNotificationService;

    // LocationService updates the Redis GEO index when a driver sends a position update
    @Autowired
    private LocationService locationService;

    // RideService verifies the ride is still active before processing a location update
    @Autowired
    private RideService rideService;

    // Receives a driver's acceptance of a ride offer sent to /app/ride/accept.
    // Delegates the full accept transaction to RideMatchingService — this includes
    // validating the offer is still active, marking the request as MATCHED, creating
    // the Ride record, and setting the driver to ON_TRIP.
    // If an OptimisticLockException is caught, another thread (expiry listener or
    // another accept from a different driver) already committed a state change first —
    // the @Version field on RideRequest detected the conflict. The driver who arrived
    // late is immediately notified that their offer is no longer available.
    @MessageMapping("/ride/accept")
    public void acceptRide(RideAcceptRequest request) {
        try {
            // driverAccepted() returns the created Ride so we can notify the passenger
            // with the full ride details (rideId, driver info, vehicle type, etc.)
            Ride ride = rideMatchingService.driverAccepted(
                    request.getRequestId(), request.getDriverId());

            // Notify the passenger that a driver has been assigned — sent AFTER driverAccepted()
            // commits to MySQL so the rideId in the notification is guaranteed to exist
            rideNotificationService.notifyPassengerDriverAssigned(
                    ride.getPassenger().getId(), ride, ride.getDriver());

            // NEW: tell the driver their accept succeeded and give them the rideId
            rideNotificationService.notifyDriverRideConfirmed(request.getDriverId(), ride);
            
        } catch (OptimisticLockException e) {
            // The @Version conflict means this driver's accept arrived after another transaction
            // (an expiry event or another accept) already changed the RideRequest state.
            // Inform the driver their offer window has closed so the app can update its UI.
            rideNotificationService.notifyDriverOfferExpired(request.getDriverId());
        }
    }

    // Receives a driver's explicit rejection of a ride offer sent to /app/ride/reject.
    // Explicit rejection is handled the same as a timeout but without waiting for the
    // 10-second Redis TTL — the offer moves immediately to the next eligible driver.
    // No response is sent back to the rejecting driver; they simply stop seeing the offer.
    // The next driver will receive a new offer via their own /topic/driver/{id} subscription.
    @MessageMapping("/ride/reject")
    public void rejectRide(RideRejectRequest request) {
        // driverRejected() is idempotent for stale/mismatched reject messages —
        // it silently returns if the offer is no longer in OFFER_PENDING state
        rideMatchingService.driverRejected(request.getRequestId(), request.getDriverId());
    }

    @MessageMapping("/ride/cancel")
    public void cancelRide (RideRejectRequest request) {
        rideMatchingService.driverCancel(request.getRequestId(),request.getDriverId());
    }



    // Receives a live GPS coordinate update from a driver during an active ride,
    // sent to /app/ride/{rideId}/location.
    // Verifies the ride is in an active state (Assigned or InProgress) via RideService,
    // stores the new coordinates in the Redis GEO index via LocationService, then
    // pushes the update to the passenger tracking this ride via WebSocket.
    @MessageMapping("/ride/{rideId}/location")
    public void updateRideLocation(@DestinationVariable String rideId, LocationUpdateRequest location) {
        // Confirms the ride exists and is in an active state before processing the update
        Ride ride = rideService.getRideIfActive(rideId);

        // Update the driver's position in the Redis GEO index so future nearby-driver
        // queries reflect their current location even while they are ON_TRIP
        locationService.updateDriverLocation(
                ride.getDriver().getId(), location.getLatitude(), location.getLongitude());

        // Push the coordinate to the passenger who is tracking this specific ride
        rideNotificationService.pushLocationToPassenger(
                rideId, location.getLatitude(), location.getLongitude());
    }
}

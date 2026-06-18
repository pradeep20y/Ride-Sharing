package com.ridesharing.project.service;

import com.ridesharing.project.dto.response.DriverAssignedResponse;
import com.ridesharing.project.dto.response.LocationTrackingResponse;
import com.ridesharing.project.dto.response.RideConfirmedResponse;
import com.ridesharing.project.dto.response.RideOfferResponse;
import com.ridesharing.project.entity.Driver;
import com.ridesharing.project.entity.Ride;
import com.ridesharing.project.entity.RideRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

// Central service for all outbound WebSocket push notifications in the ride-sharing system.
// Every message sent to connected clients (drivers and passengers) goes through this service
// so that topic paths and payload shapes are defined in one place and cannot drift.
// Interacts with: WebSocketConfig (broker and topic configuration), WebSocketController
// (passenger assignment notification on accept), RideMatchingService (driver offer and
// cancellation notifications), and SimpMessagingTemplate (STOMP message delivery).
@Service
public class RideNotificationService {

    // Spring's STOMP messaging gateway — delivers messages to all sessions subscribed
    // to a given topic via the in-memory broker configured in WebSocketConfig
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // Sends a ride offer to a single specific driver via their personal topic.
    // Called by RideMatchingService.offerToNextDriver() as the final step after MySQL
    // and Redis have already been updated — if this push fails the state is still
    // consistent and OfferExpirationJob will recover the offer within 30 seconds.
    // distanceFromDriverKm is included so the driver can see how far they are from pickup.
    public void notifyDriver(String driverId, RideRequest request, double distanceFromDriverKm) {
        RideOfferResponse offer = RideOfferResponse.builder()
                .requestId(request.getId())
                .pickupAddress(request.getPickupAddress())
                .dropoffAddress(request.getDropoffAddress())
                .distanceFromDriverKm(distanceFromDriverKm)
                .estimatedFare(request.getEstimatedFare())
                .offerExpiresInSeconds(10) // Must match OFFER_EXPIRY_SECONDS in RideMatchingService
                .build();

        // Driver's personal topic — only the driver subscribed to this exact path receives it
        messagingTemplate.convertAndSend("/topic/driver/" + driverId, offer);
    }

    // Sends a ride offer to multiple drivers simultaneously (legacy broadcast path).
    // Retained for compatibility with any existing code that still calls it.
    // The new matching algorithm uses notifyDriver() for single-driver sequential offers instead.
    public void notifyNearbyDrivers(java.util.List<com.ridesharing.project.dto.response.NearbyDriverResponse> nearbyDrivers,
                                    String requestId,
                                    String pickupAddress,
                                    String dropoffAddress,
                                    Double estimatedFare) {
        for (com.ridesharing.project.dto.response.NearbyDriverResponse driver : nearbyDrivers) {
            RideOfferResponse offer = RideOfferResponse.builder()
                    .requestId(requestId)
                    .pickupAddress(pickupAddress)
                    .dropoffAddress(dropoffAddress)
                    .distanceFromDriverKm(driver.getDistanceInKm())
                    .estimatedFare(estimatedFare)
                    .offerExpiresInSeconds(10)
                    .build();

            messagingTemplate.convertAndSend("/topic/driver/" + driver.getDriverId(), offer);
        }
    }

    // Notifies a passenger that a driver has been assigned to their ride request.
    // Called by WebSocketController after RideMatchingService.driverAccepted() returns
    // the created Ride. Sends to the passenger's personal topic so only they receive it.
    public void notifyPassengerDriverAssigned(String passengerId, Ride ride, Driver driver) {
        DriverAssignedResponse response = DriverAssignedResponse.builder()
                .rideId(ride.getId())
                .driverName(driver.getUser().getName())
                .licensePlate(driver.getLicensePlate())
                .vehicleType(driver.getVehicleType())
                .driverRating(driver.getRating())
                .driverLatitude(driver.getCurrentLatitude())
                .driverLongitude(driver.getCurrentLongitude())
                .build();

        // Passenger's personal topic — only they receive the driver assignment details
        messagingTemplate.convertAndSend("/topic/passenger/" + passengerId, response);
    }

    public void notifyDriverRideConfirmed(String driverId, Ride ride) {
        RideConfirmedResponse response = RideConfirmedResponse.builder()
                .status("RIDE_CONFIRMED")
                .rideId(ride.getId())
                .pickupLatitude(ride.getRideRequest().getPickupLatitude())
                .pickupLongitude(ride.getRideRequest().getPickupLongitude())
                .pickupAddress(ride.getRideRequest().getPickupAddress())
                .dropoffLatitude(ride.getRideRequest().getDropoffLatitude())
                .dropoffLongitude(ride.getRideRequest().getDropoffLongitude())
                .dropoffAddress(ride.getRideRequest().getDropoffAddress())
                .build();

            messagingTemplate.convertAndSend("/topic/driver/" + driverId, response);
    }

    // Pushes a live GPS coordinate update to the passenger tracking a specific ride.
    // Called by WebSocketController when the driver sends a location update during the trip.
    // The passenger subscribes to the ride-specific tracking topic after driver assignment.
    public void pushLocationToPassenger(String rideId, Double latitude, Double longitude) {
        LocationTrackingResponse tracking = LocationTrackingResponse.builder()
                .rideId(rideId)
                .latitude(latitude)
                .longitude(longitude)
                .timestamp(LocalDateTime.now())
                .build();

        // Ride-specific tracking topic — passenger subscribes to this after driver is assigned
        messagingTemplate.convertAndSend("/topic/ride/" + rideId + "/tracking", tracking);
    }

    // Notifies a passenger that their ride request has been cancelled.
    // Called by RideMatchingService when all MAX_OFFER_ATTEMPTS drivers reject or timeout,
    // or when no drivers are found in the area.
    // reason should be a human-readable explanation suitable for display in the passenger app.
    public void notifyPassengerRideCancelled(String passengerId, String reason) {
        messagingTemplate.convertAndSend(
                "/topic/passenger/" + passengerId,
                Map.of("status", "CANCELLED", "reason", reason)
        );
    }

    public void notifyPassengerRideCancelledAfterRideSuccess(String passengerId, String reason) {
       messagingTemplate.convertAndSend(
                "/topic/passenger/" + passengerId,
                Map.of("status", "DRIVER_CANCELLED", "reason", reason)
       );
    }

    // Notifies a driver that the offer they tried to accept is no longer available.
    // Called by WebSocketController when it catches an OptimisticLockException — this means
    // another thread (expiry listener or another accept message) already advanced the state
    // before this driver's accept message was processed.
    // The driver's app should use this to dismiss the offer UI and show an appropriate message.
    public void notifyDriverOfferExpired(String driverId) {
        messagingTemplate.convertAndSend(
                "/topic/driver/" + driverId,
                Map.of("status", "OFFER_EXPIRED", "message", "Offer is no longer available")
        );
    }
}

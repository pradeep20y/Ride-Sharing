package com.ridesharing.project.service;

import com.ridesharing.project.dto.request.CreateRideRequestRequest;
import com.ridesharing.project.entity.Passenger;
import com.ridesharing.project.entity.RideRequest;
import com.ridesharing.project.entity.RideRequestStatus;
import com.ridesharing.project.exception.BusinessException;
import com.ridesharing.project.exception.ResourceNotFoundException;
import com.ridesharing.project.repository.PassengerRepository;
import com.ridesharing.project.repository.RideRequestRepository;
import com.ridesharing.project.util.FareCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Handles the creation and retrieval of ride requests.
// On creation, computes fare/distance/duration automatically and delegates to
// RideMatchingService to begin the sequential driver matching process.
// Does not handle the matching algorithm itself — that responsibility belongs entirely
// to RideMatchingService to keep each service focused on a single concern.
// Interacts with: PassengerRepository (passenger validation), RideRequestRepository
// (persistence), FareCalculator (fare estimation), RideMatchingService (offer dispatch).
@Service
@Transactional(readOnly = true)
public class RideRequestService {

    private final RideRequestRepository rideRequestRepository;
    private final PassengerRepository passengerRepository;

    // RideMatchingService is injected here so that after saving the request to MySQL,
    // the matching algorithm is triggered in the same request-response cycle.
    // The passenger receives a 201 Created response immediately; the driver offer
    // is dispatched asynchronously within the same thread via WebSocket.
    private final RideMatchingService rideMatchingService;

    public RideRequestService(RideRequestRepository rideRequestRepository,
                               PassengerRepository passengerRepository,
                               RideMatchingService rideMatchingService) {
        this.rideRequestRepository = rideRequestRepository;
        this.passengerRepository = passengerRepository;
        this.rideMatchingService = rideMatchingService;
    }

    // Creates a new ride request, auto-calculates fare and duration from coordinates,
    // saves to MySQL, then triggers the matching algorithm to find and offer the ride
    // to the best nearby driver. Returns immediately after the initial offer is sent —
    // subsequent offer rounds (timeouts, rejections) are handled by background events.
    @Transactional
    public RideRequest createRideRequest(CreateRideRequestRequest request) {
        Passenger passenger = passengerRepository.findById(request.getPassengerId())
                .orElseThrow(() -> new ResourceNotFoundException("Passenger", "id", request.getPassengerId()));

        if (!"Active".equals(passenger.getStatus())) {
            throw new BusinessException(
                "Passenger account is not active and cannot submit ride requests. Current status: "
                + passenger.getStatus());
        }

        // Calculate the great-circle distance between pickup and dropoff using Haversine formula
        double distanceKm = FareCalculator.calculateDistance(
                request.getPickupLatitude(), request.getPickupLongitude(),
                request.getDropoffLatitude(), request.getDropoffLongitude());

        // Compute the total estimated fare: base charge + per-km rate + per-minute rate
        double estimatedFare = FareCalculator.calculateFare(distanceKm);

        // Estimate travel time in minutes at average city speed — shown to the passenger
        int estimatedDuration = FareCalculator.calculateDuration(distanceKm);

        RideRequest rideRequest = new RideRequest();
        rideRequest.setPassenger(passenger);
        rideRequest.setPickupLatitude(request.getPickupLatitude());
        rideRequest.setPickupLongitude(request.getPickupLongitude());
        rideRequest.setPickupAddress(request.getPickupAddress());
        rideRequest.setDropoffLatitude(request.getDropoffLatitude());
        rideRequest.setDropoffLongitude(request.getDropoffLongitude());
        rideRequest.setDropoffAddress(request.getDropoffAddress());
        rideRequest.setRideType(request.getRideType());
        rideRequest.setStatus(RideRequestStatus.OPEN);
        rideRequest.setEstimatedFare(estimatedFare);
        // Round to 2 decimal places so the distance field is clean for display
        rideRequest.setEstimatedDistance(Math.round(distanceKm * 100.0) / 100.0);
        rideRequest.setEstimatedDuration(estimatedDuration);

        RideRequest saved = rideRequestRepository.save(rideRequest);

        // Trigger the sequential matching algorithm — scores nearby drivers and sends
        // the first offer to the highest-scored driver via WebSocket.
        // If no nearby drivers are found, RideMatchingService cancels the request
        // and notifies the passenger via WebSocket immediately.
        rideMatchingService.sendInitialOffer(saved);

        return saved;
    }

    // Retrieves a ride request by its unique ID, throwing a not-found error if no match exists.
    public RideRequest getRideRequestById(String id) {
        return rideRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RideRequest", "id", id));
    }

    // Returns all ride requests currently in OPEN status — available for manual admin assignment.
    // Under normal operation most requests will be in OFFER_PENDING (already being matched)
    // rather than OPEN; OPEN requests are those where matching has not yet started.
    public List<RideRequest> getAllOpenRequests() {
        return rideRequestRepository.findByStatus(RideRequestStatus.OPEN);
    }

    // Returns all ride requests submitted by a specific passenger across all statuses.
    public List<RideRequest> getRequestsByPassenger(String passengerId) {
        Passenger passenger = passengerRepository.findById(passengerId)
                .orElseThrow(() -> new ResourceNotFoundException("Passenger", "id", passengerId));

        return rideRequestRepository.findByPassenger(passenger);
    }

    // Cancels a ride request. Only requests in OPEN or OFFER_PENDING state may be cancelled —
    // a request that has already been MATCHED has a driver committed, so cancellation at that
    // point must go through the Ride cancel endpoint instead.
    @Transactional
    public RideRequest cancelRideRequest(String id) {
        RideRequest rideRequest = rideRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RideRequest", "id", id));

        // Cancellation is only valid before a driver is committed to the ride
        if (rideRequest.getStatus() == RideRequestStatus.MATCHED
                || rideRequest.getStatus() == RideRequestStatus.COMPLETED
                || rideRequest.getStatus() == RideRequestStatus.CANCELLED) {
            throw new BusinessException(
                "Only OPEN or OFFER_PENDING requests can be cancelled. Current status: "
                + rideRequest.getStatus());
        }

        rideRequest.setStatus(RideRequestStatus.CANCELLED);
        rideRequest.setOfferedToDriverId(null);
        rideRequest.setOfferExpiresAt(null);
        return rideRequestRepository.save(rideRequest);
    }
}

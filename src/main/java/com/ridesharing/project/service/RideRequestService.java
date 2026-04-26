package com.ridesharing.project.service;

import com.ridesharing.project.dto.request.CreateRideRequestRequest;
import com.ridesharing.project.entity.Passenger;
import com.ridesharing.project.entity.RideRequest;
import com.ridesharing.project.exception.BusinessException;
import com.ridesharing.project.exception.ResourceNotFoundException;
import com.ridesharing.project.repository.PassengerRepository;
import com.ridesharing.project.repository.RideRequestRepository;
import com.ridesharing.project.util.FareCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// This service handles all business logic for ride requests including fare estimation, retrieval, and status management.
@Service
@Transactional(readOnly = true)
public class RideRequestService {

    private final RideRequestRepository rideRequestRepository;
    private final PassengerRepository passengerRepository;

    // Constructor injection ensures both repositories are always available and promotes testability.
    public RideRequestService(RideRequestRepository rideRequestRepository, PassengerRepository passengerRepository) {
        this.rideRequestRepository = rideRequestRepository;
        this.passengerRepository = passengerRepository;
    }

    // Creates a new ride request and automatically calculates the estimated fare, distance, and duration using pickup and dropoff coordinates.
    @Transactional
    public RideRequest createRideRequest(CreateRideRequestRequest request) {
        Passenger passenger = passengerRepository.findById(request.getPassengerId())
                .orElseThrow(() -> new ResourceNotFoundException("Passenger", "id", request.getPassengerId()));

        if (!"Active".equals(passenger.getStatus())) {
            throw new BusinessException("Passenger account is not active and cannot submit ride requests. Current status: " + passenger.getStatus());
        }

        // Calculate the great-circle distance between pickup and dropoff coordinates using the Haversine formula
        double distanceKm = FareCalculator.calculateDistance(
                request.getPickupLatitude(), request.getPickupLongitude(),
                request.getDropoffLatitude(), request.getDropoffLongitude());

        // Compute the total estimated fare incorporating base charge, distance rate, and time rate
        double estimatedFare = FareCalculator.calculateFare(distanceKm);

        // Estimate travel time in minutes at average city speed for display to the passenger
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
        rideRequest.setStatus("Open");
        rideRequest.setEstimatedFare(estimatedFare);
        // Round to 2 decimal places so the distance field is clean for display
        rideRequest.setEstimatedDistance(Math.round(distanceKm * 100.0) / 100.0);
        rideRequest.setEstimatedDuration(estimatedDuration);

        return rideRequestRepository.save(rideRequest);
    }

    // Retrieves a ride request by its unique ID, throwing a not-found error if no match exists.
    public RideRequest getRideRequestById(String id) {
        return rideRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RideRequest", "id", id));
    }

    // Returns all ride requests currently in the Open status, which are available for driver assignment.
    public List<RideRequest> getAllOpenRequests() {
        return rideRequestRepository.findByStatus("Open");
    }

    // Returns all ride requests submitted by a specific passenger across all statuses.
    public List<RideRequest> getRequestsByPassenger(String passengerId) {
        Passenger passenger = passengerRepository.findById(passengerId)
                .orElseThrow(() -> new ResourceNotFoundException("Passenger", "id", passengerId));

        return rideRequestRepository.findByPassenger(passenger);
    }

    // Cancels a ride request; only requests in the Open status may be cancelled to prevent inconsistent state.
    @Transactional
    public RideRequest cancelRideRequest(String id) {
        RideRequest rideRequest = rideRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RideRequest", "id", id));

        if (!"Open".equals(rideRequest.getStatus())) {
            throw new BusinessException("Only ride requests with 'Open' status can be cancelled. Current status: " + rideRequest.getStatus());
        }

        rideRequest.setStatus("Cancelled");
        return rideRequestRepository.save(rideRequest);
    }
}

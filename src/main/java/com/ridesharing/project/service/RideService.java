package com.ridesharing.project.service;

import com.ridesharing.project.entity.Driver;
import com.ridesharing.project.entity.Passenger;
import com.ridesharing.project.entity.Ride;
import com.ridesharing.project.entity.RideRequest;
import com.ridesharing.project.entity.RideRequestStatus;
import com.ridesharing.project.exception.BusinessException;
import com.ridesharing.project.exception.ResourceNotFoundException;
import com.ridesharing.project.repository.DriverRepository;
import com.ridesharing.project.repository.PassengerRepository;
import com.ridesharing.project.repository.RideRepository;
import com.ridesharing.project.repository.RideRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

// This service manages the full lifecycle of a ride from driver assignment through completion or cancellation.
@Service
@Transactional(readOnly = true)
public class RideService {

    private final RideRepository rideRepository;
    private final RideRequestRepository rideRequestRepository;
    private final DriverRepository driverRepository;
    private final PassengerRepository passengerRepository;

    // Constructor injection for all ride-related dependencies to ensure availability and testability.
    public RideService(RideRepository rideRepository, RideRequestRepository rideRequestRepository,
                       DriverRepository driverRepository, PassengerRepository passengerRepository) {
        this.rideRepository = rideRepository;
        this.rideRequestRepository = rideRequestRepository;
        this.driverRepository = driverRepository;
        this.passengerRepository = passengerRepository;
    }

    // Assigns an available driver to an open ride request, creating a new Ride record and updating both request and driver statuses.
    @Transactional
    public Ride createRide(String requestId, String driverId) {
        RideRequest rideRequest = rideRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("RideRequest", "id", requestId));

        if (rideRequest.getStatus() != RideRequestStatus.OPEN) {
            throw new BusinessException("Ride request is not available for assignment. Current status: " + rideRequest.getStatus());
        }

        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver", "id", driverId));

        if (!"ONLINE".equals(driver.getStatus())) {
            throw new BusinessException("Driver is not available for assignment. Current driver status: " + driver.getStatus());
        }

        Passenger passenger = rideRequest.getPassenger();

        Ride ride = new Ride();
        ride.setRideRequest(rideRequest);
        ride.setPassenger(passenger);
        ride.setDriver(driver);
        ride.setStatus("Assigned");
        // Copy the estimated fare from the request so driver and passenger agreed on the same figure
        ride.setFare(rideRequest.getEstimatedFare());

        // Mark the request as MATCHED to prevent it from being assigned to a second driver
        rideRequest.setStatus(RideRequestStatus.MATCHED);
        rideRequestRepository.save(rideRequest);

        // Put the driver on-trip so they no longer appear in the available pool
        driver.setStatus("ON_TRIP");
        driverRepository.save(driver);

        return rideRepository.save(ride);
    }

    // Transitions a ride from Assigned to InProgress when the driver begins the trip.
    @Transactional
    public Ride startRide(String id) {
        Ride ride = rideRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ride", "id", id));

        if (!"Assigned".equals(ride.getStatus())) {
            throw new BusinessException("A ride can only be started when in 'Assigned' status. Current status: " + ride.getStatus());
        }

        ride.setStatus("InProgress");
        return rideRepository.save(ride);
    }

    // Completes an in-progress ride, records the completion timestamp, and credits the driver's earnings.
    @Transactional
    public Ride completeRide(String id) {
        Ride ride = rideRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ride", "id", id));

        if (!"InProgress".equals(ride.getStatus())) {
            throw new BusinessException("A ride can only be completed when 'InProgress'. Current status: " + ride.getStatus());
        }

        // Record when the trip ended for reporting and receipts
        ride.setStatus("Completed");
        ride.setCompletedDate(LocalDateTime.now());

        Driver driver = ride.getDriver();
        // Accumulate the fare into the driver's lifetime earnings total
        driver.setTotalEarnings(driver.getTotalEarnings() + ride.getFare());
        // Increment ride count used in driver statistics
        driver.setTotalRides(driver.getTotalRides() + 1);
        // Return driver to ONLINE so they can receive new ride requests immediately
        driver.setStatus("ONLINE");
        driverRepository.save(driver);

        // Sync the originating ride request to COMPLETED for history consistency
        RideRequest rideRequest = ride.getRideRequest();
        rideRequest.setStatus(RideRequestStatus.COMPLETED);
        rideRequestRepository.save(rideRequest);

        return rideRepository.save(ride);
    }

    // Cancels an active ride, resets the driver to available, and marks the originating request as Cancelled.
    @Transactional
    public Ride cancelRide(String id) {
        Ride ride = rideRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ride", "id", id));

        if ("Completed".equals(ride.getStatus()) || "Cancelled".equals(ride.getStatus())) {
            throw new BusinessException("Ride cannot be cancelled because it is already " + ride.getStatus() + ".");
        }

        ride.setStatus("Cancelled");

        // Return the driver to ONLINE so they can receive new ride requests after cancellation
        Driver driver = ride.getDriver();
        driver.setStatus("ONLINE");
        driverRepository.save(driver);

        // Revert the ride request to CANCELLED so it cannot be reassigned or completed
        RideRequest rideRequest = ride.getRideRequest();
        rideRequest.setStatus(RideRequestStatus.CANCELLED);
        rideRequestRepository.save(rideRequest);

        return rideRepository.save(ride);
    }

    // Retrieves a ride by its unique ID, throwing a not-found error if no match exists.
    public Ride getRideById(String id) {
        return rideRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ride", "id", id));
    }

    // Returns all rides assigned to or completed by a specific driver.
    public List<Ride> getRidesByDriver(String driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver", "id", driverId));

        return rideRepository.findByDriver(driver);
    }

    // Returns all rides taken by a specific passenger across all statuses.
    public List<Ride> getRidesByPassenger(String passengerId) {
        Passenger passenger = passengerRepository.findById(passengerId)
                .orElseThrow(() -> new ResourceNotFoundException("Passenger", "id", passengerId));

        return rideRepository.findByPassenger(passenger);
    }

    @Transactional
    public Ride acceptRide(String requestId, String driverId) {

        RideRequest rideRequest = rideRequestRepository
                .findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("RideRequest", "id", requestId));

        Driver driver = driverRepository
                .findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("RideRequest", "id", requestId));

        // Guard against race condition where two drivers accept simultaneously
        if (rideRequest.getStatus() != RideRequestStatus.OPEN) {
            throw new ResourceNotFoundException("RideRequest", "id", requestId);
        }

        rideRequest.setStatus(RideRequestStatus.MATCHED);
        rideRequestRepository.save(rideRequest);

        driver.setStatus("ON_TRIP");
        driverRepository.save(driver);

        Ride ride = new Ride();
        ride.setRideRequest(rideRequest);
        ride.setPassenger(rideRequest.getPassenger());
        ride.setDriver(driver);
        ride.setStatus("Assigned");
        ride.setFare(rideRequest.getEstimatedFare());

        return rideRepository.save(ride);
    }

    // Handles driver location update during an active ride
// Updates Redis and returns the ride for the controller to use
    public Ride getRideIfActive(String rideId) {

        Ride ride = rideRepository
                .findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));

        if (!ride.getStatus().equals("InProgress") &&
                !ride.getStatus().equals("Assigned")) {
            throw new RuntimeException("Ride is not active");
        }

        return ride;
    }
}

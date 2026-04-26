package com.ridesharing.project.repository;

import com.ridesharing.project.entity.Passenger;
import com.ridesharing.project.entity.RideRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

// This repository provides data access methods for RideRequest entities, extending JPA's standard CRUD operations.
@Repository
public interface RideRequestRepository extends JpaRepository<RideRequest, String> {

    // Returns all ride requests with the given status (e.g. Open, Matched, Completed, Cancelled).
    List<RideRequest> findByStatus(String status);

    // Returns all ride requests submitted by a specific passenger.
    List<RideRequest> findByPassenger(Passenger passenger);

    // Returns ride requests for a passenger filtered by status.
    List<RideRequest> findByPassengerAndStatus(Passenger passenger, String status);
}

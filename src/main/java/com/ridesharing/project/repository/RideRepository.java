package com.ridesharing.project.repository;

import com.ridesharing.project.entity.Driver;
import com.ridesharing.project.entity.Passenger;
import com.ridesharing.project.entity.Ride;
import com.ridesharing.project.entity.RideRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// This repository provides data access methods for Ride entities, extending JPA's standard CRUD operations.
@Repository
public interface RideRepository extends JpaRepository<Ride, String> {

    // Returns all rides assigned to or completed by the specified driver.
    List<Ride> findByDriver(Driver driver);

    // Returns all rides taken by the specified passenger.
    List<Ride> findByPassenger(Passenger passenger);

    // Finds the ride associated with a particular ride request.
    Optional<Ride> findByRideRequest(RideRequest rideRequest);
}

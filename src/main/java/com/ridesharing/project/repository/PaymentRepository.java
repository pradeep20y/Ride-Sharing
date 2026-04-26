package com.ridesharing.project.repository;

import com.ridesharing.project.entity.Payment;
import com.ridesharing.project.entity.Ride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// This repository provides data access methods for Payment entities, extending JPA's standard CRUD operations.
@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {

    // Finds the payment record associated with the given ride.
    Optional<Payment> findByRide(Ride ride);

    // Checks whether a payment already exists for the given ride, used to prevent duplicate payments.
    boolean existsByRide(Ride ride);
}

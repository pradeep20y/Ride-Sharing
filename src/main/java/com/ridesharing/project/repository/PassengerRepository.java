package com.ridesharing.project.repository;

import com.ridesharing.project.entity.Passenger;
import com.ridesharing.project.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// This repository provides data access methods for Passenger entities, extending JPA's standard CRUD operations.
@Repository
public interface PassengerRepository extends JpaRepository<Passenger, String> {

    // Finds a passenger profile associated with the given user account.
    Optional<Passenger> findByUser(User user);

    // Returns all passengers with the specified status value.
    List<Passenger> findByStatus(String status);

    // Checks whether a passenger profile already exists for the given user.
    boolean existsByUser(User user);

    Optional<Passenger> findByUser_Id(String userId);
}

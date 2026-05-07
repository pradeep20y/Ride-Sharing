package com.ridesharing.project.repository;

import com.ridesharing.project.entity.Passenger;
import com.ridesharing.project.entity.RideRequest;
import com.ridesharing.project.entity.RideRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

// Data access layer for RideRequest entities.
// All status-based queries use RideRequestStatus enum for type safety — plain String
// queries were replaced when the status field was migrated from String to enum.
// Used by RideRequestService, RideMatchingService, OfferExpiryListener, and OfferExpirationJob.
@Repository
public interface RideRequestRepository extends JpaRepository<RideRequest, String> {

    // Returns all ride requests with the given status.
    // Accepts RideRequestStatus enum so callers cannot pass arbitrary strings.
    List<RideRequest> findByStatus(RideRequestStatus status);

    // Returns all ride requests submitted by a specific passenger across all statuses.
    List<RideRequest> findByPassenger(Passenger passenger);

    // Returns ride requests for a specific passenger filtered by status.
    // Used by RideRequestService to show a passenger their requests in a particular state.
    List<RideRequest> findByPassengerAndStatus(Passenger passenger, RideRequestStatus status);

    // Finds OFFER_PENDING requests whose offer timer has already expired in MySQL.
    // This is the query used by OfferExpirationJob as a fallback safety net in case
    // Redis missed the key expiry event — any request stuck in OFFER_PENDING with
    // offerExpiresAt in the past will be found and advanced by the scheduled job.
    List<RideRequest> findByStatusAndOfferExpiresAtBefore(RideRequestStatus status, LocalDateTime time);
}

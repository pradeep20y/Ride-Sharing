package com.ridesharing.project.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

// Captures a passenger's request for a ride, storing pickup/dropoff details,
// automatically computed fare estimates, and all state needed by the matching algorithm.
// Central entity touched by RideRequestService (creation), RideMatchingService (offer lifecycle),
// OfferExpiryListener (expiry events), OfferExpirationJob (fallback), and RideService (completion).
@Entity
@Table(name = "ride_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RideRequest {

    @Id
    private String id = UUID.randomUUID().toString();

    // The passenger who submitted this request — used to notify them when a driver is assigned
    @ManyToOne
    @JoinColumn(name = "passenger_id", nullable = false)
    private Passenger passenger;

    @Column(nullable = false)
    private Double pickupLatitude;

    @Column(nullable = false)
    private Double pickupLongitude;

    @Column(nullable = false)
    private String pickupAddress;

    @Column(nullable = false)
    private Double dropoffLatitude;

    @Column(nullable = false)
    private Double dropoffLongitude;

    @Column(nullable = false)
    private String dropoffAddress;

    @Column(nullable = false)
    private String rideType; // ECONOMY, COMFORT, PREMIUM

    // Stored as the enum name (e.g. "OPEN") so MySQL values remain human-readable.
    // Replaces the old plain String status field to enable type-safe comparisons
    // and Spring Data JPA enum-based query methods.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RideRequestStatus status = RideRequestStatus.OPEN;

    @Column(nullable = false)
    private Double estimatedFare;

    @Column(nullable = false)
    private Double estimatedDistance;

    @Column(nullable = false)
    private Integer estimatedDuration;

    // ID of the driver who currently holds the active offer.
    // Null when no offer is in flight (status is OPEN, MATCHED, COMPLETED, or CANCELLED).
    // Set by RideMatchingService.offerToNextDriver() and cleared on accept or cancel.
    @Column(name = "offered_to_driver_id")
    private String offeredToDriverId;

    // Wall-clock timestamp when the current offer expires.
    // Set to now() + OFFER_EXPIRY_SECONDS when an offer is dispatched.
    // Used by OfferExpirationJob as a MySQL-side fallback to find stale OFFER_PENDING
    // requests in case Redis missed the key expiry event (e.g. after a Redis restart).
    @Column(name = "offer_expires_at")
    private LocalDateTime offerExpiresAt;

    // Running count of how many drivers have been offered this ride request so far.
    // Incremented by RideMatchingService each time a new offer is sent.
    // When this reaches MAX_OFFER_ATTEMPTS the request transitions to CANCELLED.
    @Column(name = "offer_attempts", nullable = false)
    private Integer offerAttempts = 0;

    // JPA optimistic locking version counter managed automatically by Hibernate.
    // Prevents a race condition where both the Redis expiry listener (timeout path)
    // and a WebSocket accept message (accept path) attempt to update the same
    // RideRequest row at the same moment. Whichever transaction commits second
    // will receive an OptimisticLockException, which the WebSocketController catches
    // and converts into an OFFER_EXPIRED notification to the late-arriving driver.
    @Version
    @Column(nullable = false)
    private Integer version;

    // Set once at creation time; never modified — used for history and reporting
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate = LocalDateTime.now();
}

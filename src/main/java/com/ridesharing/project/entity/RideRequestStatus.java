package com.ridesharing.project.entity;

// Represents every state a RideRequest can move through during its lifecycle.
// Using an enum instead of a plain String prevents typos, enables IDE auto-completion,
// and lets Spring Data JPA generate type-safe repository query methods.
// Stored as the enum name (e.g. "OPEN") in MySQL via @Enumerated(EnumType.STRING)
// so the database values remain human-readable.
// Interacts with: RideRequest entity, RideMatchingService, RideRequestRepository,
// OfferExpiryListener, and OfferExpirationJob.
public enum RideRequestStatus {

    // Passenger has submitted the request and it is waiting for the matching algorithm
    // to find and offer it to the first eligible driver. No driver has been contacted yet.
    OPEN,

    // The matching algorithm has selected the best-scored driver and sent them an offer.
    // The system is waiting for that specific driver to accept within the 10-second window.
    // A Redis key with TTL is active during this state to trigger expiry-based advancement.
    OFFER_PENDING,

    // A driver accepted the offer. A Ride record has been created linking the driver
    // and passenger. This is the terminal success state for the matching phase.
    MATCHED,

    // The Ride linked to this request was completed successfully by the driver.
    // Set by RideService when the driver marks the trip as finished.
    COMPLETED,

    // Either all MAX_OFFER_ATTEMPTS drivers rejected or timed out, no drivers were
    // found nearby, or the passenger manually cancelled the request.
    // No further matching will be attempted once in this state.
    CANCELLED
}

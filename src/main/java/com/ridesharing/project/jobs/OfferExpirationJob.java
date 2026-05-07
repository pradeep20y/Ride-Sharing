package com.ridesharing.project.jobs;

import com.ridesharing.project.entity.RideRequest;
import com.ridesharing.project.entity.RideRequestStatus;
import com.ridesharing.project.repository.RideRequestRepository;
import com.ridesharing.project.service.RideMatchingService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

// Fallback safety net for the offer expiry mechanism.
// The PRIMARY path for advancing expired offers is OfferExpiryListener reacting to
// Redis TTL events in real time. This job exists because Redis can lose keyspace
// notification events after a restart or network partition — without this fallback,
// a ride request could get permanently stuck in OFFER_PENDING state with no driver
// ever assigned and no passenger notification sent.
// Runs every 30 seconds and queries MySQL directly for any OFFER_PENDING requests
// whose offerExpiresAt timestamp has already passed. For each one found, it delegates
// to RideMatchingService.moveToNextDriver() exactly as the Redis listener would.
// Interacts with: RideRequestRepository (MySQL query), RideMatchingService (state advancement).
//@Component
public class OfferExpirationJob {

    private final RideRequestRepository rideRequestRepository;
    private final RideMatchingService rideMatchingService;

    public OfferExpirationJob(RideRequestRepository rideRequestRepository,
                               RideMatchingService rideMatchingService) {
        this.rideRequestRepository = rideRequestRepository;
        this.rideMatchingService = rideMatchingService;
    }

    // Scans MySQL every 30 seconds for OFFER_PENDING requests whose offer timer has expired.
    // Uses fixedDelay (not fixedRate) so the next scan only starts 30 seconds after the
    // previous scan fully completes — prevents overlapping executions if the DB query is slow.
    // Under normal operation (Redis healthy) this method will find zero records because
    // OfferExpiryListener already advanced them within milliseconds of their TTL expiring.
    // Under failure conditions (Redis down, missed events) this job catches any requests
    // that slipped through and advances them up to 30 seconds late — an acceptable delay
    // for a safety net mechanism.
//    @Scheduled(fixedDelay = 30000)
    public void processExpiredOffers() {
        // Query MySQL for all requests still stuck in OFFER_PENDING with an expired timer.
        // offerExpiresAt < now means the 10-second window has passed in wall-clock time.
        List<RideRequest> expiredRequests = rideRequestRepository
                .findByStatusAndOfferExpiresAtBefore(
                        RideRequestStatus.OFFER_PENDING,
                        LocalDateTime.now());

        // Process each expired request independently.
        // moveToNextDriver contains its own @Transactional boundary so each request
        // is handled in its own transaction — a failure on one does not roll back the others.
        for (RideRequest request : expiredRequests) {
            rideMatchingService.moveToNextDriver(request);
        }
    }
}

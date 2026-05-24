package com.ridesharing.project.service;

import com.ridesharing.project.dto.response.NearbyDriverResponse;
import com.ridesharing.project.entity.Driver;
import com.ridesharing.project.entity.Ride;
import com.ridesharing.project.entity.RideRequest;
import com.ridesharing.project.entity.RideRequestStatus;
import com.ridesharing.project.exception.ResourceNotFoundException;
import com.ridesharing.project.repository.DriverRepository;
import com.ridesharing.project.repository.RideRepository;
import com.ridesharing.project.repository.RideRequestRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

// Core service that implements the intelligent sequential driver matching algorithm.
// Replaces the old "broadcast to all nearby drivers at once" approach with a scored,
// one-at-a-time offer flow: best driver gets the offer, waits 10 seconds, then moves
// to the next best if no response, up to MAX_OFFER_ATTEMPTS total tries.
// Interacts with: RideRequestRepository (state persistence), DriverRepository (rating lookup
// and driver status updates), RideRepository (ride creation on accept), LocationService
// (Redis GEO radius queries), RideNotificationService (WebSocket pushes), RedisTemplate
// (offer key TTL management).
// Called by: RideRequestService (initial offer), WebSocketController (accept/reject),
// OfferExpiryListener (timeout), OfferExpirationJob (fallback sweep).
@Service
@Transactional(readOnly = true)
public class RideMatchingService {

    // How many seconds a driver has to accept an offer before it automatically moves on
    private static final int OFFER_EXPIRY_SECONDS = 10;

    // Maximum number of drivers to try before cancelling the request entirely
    private static final int MAX_OFFER_ATTEMPTS = 5;

    // Redis key prefix for offer timer keys — format: "offer:{requestId}"
    // All other Redis keys in the system use different prefixes so this namespace is safe
    private static final String OFFER_KEY_PREFIX = "offer:";

    private final RideRequestRepository rideRequestRepository;
    private final DriverRepository driverRepository;
    private final RideRepository rideRepository;
    private final LocationService locationService;
    private final RideNotificationService rideNotificationService;
    private final RedisTemplate<String, String> redisTemplate;

    public RideMatchingService(RideRequestRepository rideRequestRepository,
                                DriverRepository driverRepository,
                                RideRepository rideRepository,
                                LocationService locationService,
                                RideNotificationService rideNotificationService,
                                RedisTemplate<String, String> redisTemplate) {
        this.rideRequestRepository = rideRequestRepository;
        this.driverRepository = driverRepository;
        this.rideRepository = rideRepository;
        this.locationService = locationService;
        this.rideNotificationService = rideNotificationService;
        this.redisTemplate = redisTemplate;
    }

    // Entry point called by RideRequestService immediately after a new ride request is saved.
    // Validates that the request is in OPEN state (it was just created so this is a safety guard),
    // then delegates to offerToNextDriver to score nearby drivers and send the first offer.
    // Guard exists to prevent accidental re-triggering if called on an already-active request.
    @Transactional
    public void sendInitialOffer(RideRequest request) {
        // Guard: matching can only start from the initial OPEN state
        if (request.getStatus() != RideRequestStatus.OPEN) {
            throw new IllegalStateException(
                "Cannot start matching for a request that is not OPEN. Current status: " + request.getStatus());
        }

        offerToNextDriver(request);
    }

    // Called by WebSocketController when a driver sends an accept message.
    // Validates that the offer is still active and belongs to this driver, then
    // atomically marks the request as MATCHED, deletes the Redis timer key (to prevent
    // the expiry listener from firing after acceptance), creates the Ride record, and
    // updates the driver's status to ON_TRIP.
    // Returns the created Ride so the controller can notify the passenger.
    // @Transactional ensures that the request update, driver update, and ride creation
    // are all committed together — if any step fails, the entire transaction rolls back.
    // The @Version field on RideRequest handles the race condition where both an expiry
    // event and an accept message arrive simultaneously — the loser gets OptimisticLockException.
    @Transactional
    public Ride driverAccepted(String requestId, String driverId) {
        RideRequest request = rideRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("RideRequest", "id", requestId));

        // Guard: only process acceptance if the offer is still actively waiting
        // Status could be MATCHED (another driver accepted first — race condition handled by @Version)
        // or CANCELLED (all attempts exhausted) or OPEN (offer was never sent — data integrity error)
        if (request.getStatus() != RideRequestStatus.OFFER_PENDING) {
            throw new IllegalStateException(
                "Ride request is no longer in OFFER_PENDING state. Current status: " + request.getStatus());
        }

        // Guard: only the specific driver who received the offer may accept it
        // Prevents a different driver from accepting an offer that was never sent to them
        if (!driverId.equals(request.getOfferedToDriverId())) {
            throw new IllegalStateException(
                "Driver " + driverId + " does not hold the active offer for request " + requestId
                + ". Current offer holder: " + request.getOfferedToDriverId());
        }

        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver", "id", driverId));

        // Transition the request to MATCHED and clear all offer tracking fields
        request.setStatus(RideRequestStatus.MATCHED);
        request.setOfferedToDriverId(null);
        request.setOfferExpiresAt(null);

        // Save the request state to MySQL BEFORE deleting the Redis key.
        // If the Redis delete fails after the MySQL save, the expiry listener may still fire
        // but the guard in moveToNextDriver (status != OFFER_PENDING) will stop it immediately.
        rideRequestRepository.save(request);

        // Delete the Redis offer key to cancel the countdown timer.
        // Without this deletion, the key would expire after 10 seconds and OfferExpiryListener
        // would try to advance to the next driver even though this driver already accepted.
        redisTemplate.delete(OFFER_KEY_PREFIX + requestId);

        // Create the Ride record that links the driver, passenger, and request together
        Ride ride = new Ride();
        ride.setRideRequest(request);
        ride.setPassenger(request.getPassenger());
        ride.setDriver(driver);
        ride.setStatus("Assigned");
        ride.setFare(request.getEstimatedFare());

        // Mark the driver as ON_TRIP so they no longer appear in nearby-driver searches.done
        driver.setStatus("ON_TRIP");
        driverRepository.save(driver);

        return rideRepository.save(ride);
    }

    // Called by WebSocketController when a driver explicitly sends a reject message.
    // Differs from a timeout: the driver actively said no, so we delete the Redis key
    // immediately rather than waiting for the TTL to expire. Then advances to the next driver.
    // Guards against stale or mismatched reject messages — only the driver currently
    // holding the offer can reject it.
    @Transactional
    public void driverRejected(String requestId, String driverId) {
        RideRequest request = rideRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("RideRequest", "id", requestId));

        // Guard: silently ignore rejection if offer is no longer pending
        // (could be MATCHED because another transaction accepted, or CANCELLED)
        if (request.getStatus() != RideRequestStatus.OFFER_PENDING) {
            return;
        }

        // Guard: silently ignore rejection from a driver who does not hold the active offer
        // Prevents race conditions where a stale reject arrives after the offer moved on
        if (!driverId.equals(request.getOfferedToDriverId())) {
            return;
        }

        // Delete the Redis key immediately — explicit rejection means no need to wait for expiry.
        // This prevents OfferExpiryListener from firing and duplicating the advancement logic.
        redisTemplate.delete(OFFER_KEY_PREFIX + requestId);

        // Advance to the next eligible driver (or cancel if all attempts exhausted)
        moveToNextDriver(request);
    }

    // Advances the matching state machine to the next eligible driver after a timeout or rejection.
    // Called by OfferExpiryListener (Redis TTL expired), driverRejected (explicit reject),
    // and OfferExpirationJob (MySQL fallback sweep).
    // Checks the attempt counter first: if MAX_OFFER_ATTEMPTS is reached, the request is
    // cancelled and the passenger is notified. Otherwise, sends offer to the next best driver.
    // @Transactional ensures the request state is atomically updated — combined with the
    // @Version field, concurrent calls from both expiry listener and fallback job will have
    // one succeed and the other throw OptimisticLockException (which callers handle or ignore).
    @Transactional
    public void moveToNextDriver(RideRequest request) {
        // Guard: only advance if the request is still actively waiting for a driver.
        // Silently return if the status changed between the caller's load and this invocation.
        // This handles the case where a driver accepted between when the Redis key expired
        // and when this method was called.
        if (request.getStatus() != RideRequestStatus.OFFER_PENDING) {
            return;
        }

        // Notify the driver whose offer just timed out so their UI can react immediately.
        // This covers the normal expiry path (Redis TTL fired → OfferExpiryListener).
        // The race-condition path (driver sent ACCEPT just as the key expired) is handled
        // separately in WebSocketController via the OptimisticLockException catch block.
        if (request.getOfferedToDriverId() != null) {
            rideNotificationService.notifyDriverOfferExpired(request.getOfferedToDriverId());
        }

        // If all allowed attempts are exhausted, cancel the request and notify the passenger
        if (request.getOfferAttempts() >= MAX_OFFER_ATTEMPTS) {
            request.setStatus(RideRequestStatus.CANCELLED);
            request.setOfferedToDriverId(null);
            request.setOfferExpiresAt(null);
            rideRequestRepository.save(request);

            rideNotificationService.notifyPassengerRideCancelled(
                    request.getPassenger().getId(),
                    "No drivers were available to accept your ride after "
                    + MAX_OFFER_ATTEMPTS + " attempts. Please try again.");
            return;
        }

        // Attempts remaining — find and offer the next best driver
        offerToNextDriver(request);
    }

    // Scores all eligible nearby drivers and sends the offer to the single best candidate.
    // "Eligible" means: present in the Redis GEO index AND not the driver who last held
    // the offer (to avoid immediately re-offering to someone who just timed out or rejected).
    // Scoring formula: 60% weight on proximity (closer = higher score) and 40% on rating
    // (higher rating = higher score). Distance is weighted more because passenger wait time
    // is the primary concern — a nearby lower-rated driver is usually better than a highly-rated
    // driver who is far away.
    // Database is saved BEFORE the Redis key is set and BEFORE the WebSocket push.
    // This ordering ensures that if Redis or the WebSocket broker is temporarily unavailable,
    // the MySQL state is still consistent and OfferExpirationJob can recover the offer.
    @Transactional
    private void offerToNextDriver(RideRequest request) {
        // Query Redis GEO for all drivers within the default 5 km radius of the pickup point
        List<NearbyDriverResponse> nearbyDrivers = locationService.findNearbyDrivers(
                request.getPickupLatitude(), request.getPickupLongitude());

        // Filter out the driver who currently holds or last held the offer.
        // This prevents immediately re-offering to a driver who just timed out.
        // On the very first call offeredToDriverId is null so no driver is excluded.
        List<NearbyDriverResponse> eligibleDrivers = nearbyDrivers.stream()
                .filter(d -> !d.getDriverId().equals(request.getOfferedToDriverId()))
                .collect(Collectors.toList());

        // If no drivers are in the Redis GEO index near the pickup, cancel immediately
        if (eligibleDrivers.isEmpty()) {
            request.setStatus(RideRequestStatus.CANCELLED);
            request.setOfferedToDriverId(null);
            request.setOfferExpiresAt(null);
            rideRequestRepository.save(request);
            rideNotificationService.notifyPassengerRideCancelled(
                    request.getPassenger().getId(),
                    "No drivers are available in your area right now. Please try again later.");
            return;
        }

        // Score each eligible driver using distance (60%) and rating (40%).
        // Driver rating is loaded from MySQL because NearbyDriverResponse only carries
        // the driver ID, distance, and coordinates from the Redis GEO query.
        NearbyDriverResponse bestDriverResponse = null;
        Driver bestDriver = null;
        double bestScore = -1.0;

        for (NearbyDriverResponse nearby : eligibleDrivers) {
            // Skip drivers that exist in Redis but have been deleted from MySQL
            Driver driver = driverRepository.findById(nearby.getDriverId()).orElse(null);
            if (driver == null) {
                continue;
            }

            double score = calculateScore(nearby.getDistanceInKm(), driver.getRating());
            if (score > bestScore) {
                bestScore = score;
                bestDriverResponse = nearby;
                bestDriver = driver;
            }
        }

        // All Redis-tracked drivers have been deleted from MySQL — cancel the request
        if (bestDriver == null) {
            request.setStatus(RideRequestStatus.CANCELLED);
            request.setOfferedToDriverId(null);
            request.setOfferExpiresAt(null);
            rideRequestRepository.save(request);
            rideNotificationService.notifyPassengerRideCancelled(
                    request.getPassenger().getId(),
                    "No drivers are available in your area right now. Please try again later.");
            return;
        }

        // Transition to OFFER_PENDING and record which driver holds the offer
        request.setStatus(RideRequestStatus.OFFER_PENDING);
        request.setOfferedToDriverId(bestDriver.getId());
        // Record the wall-clock expiry time so OfferExpirationJob can find this record
        // in MySQL if the Redis expiry event is missed (e.g. after a Redis restart)
        request.setOfferExpiresAt(LocalDateTime.now().plusSeconds(OFFER_EXPIRY_SECONDS));
        // Increment the attempt counter — when this reaches MAX_OFFER_ATTEMPTS, moveToNextDriver cancels
        request.setOfferAttempts(request.getOfferAttempts() + 1);

        // Save to MySQL FIRST — state must be committed before any external side effects.
        // If the Redis set or WebSocket push fails after this save, OfferExpirationJob
        // will find this OFFER_PENDING record with a past offerExpiresAt and advance it.
        RideRequest saved = rideRequestRepository.save(request);

        // Set the offer key in Redis with a TTL equal to the offer window.
        // When this key expires, OfferExpiryListener fires and calls moveToNextDriver.
        // Value is the driverId — stored for debugging but not read by the listener.
        redisTemplate.opsForValue().set(
                OFFER_KEY_PREFIX + saved.getId(),
                bestDriver.getId(),
                OFFER_EXPIRY_SECONDS,
                TimeUnit.SECONDS
        );

        // Push the offer to the selected driver via WebSocket — this is the last step
        // so a WebSocket failure does not leave the database or Redis in an inconsistent state
        rideNotificationService.notifyDriver(
                bestDriver.getId(),
                saved,
                bestDriverResponse.getDistanceInKm()
        );
    }

    // Calculates a composite score for a driver candidate given their distance from the
    // pickup point and their rating. Used to rank all eligible nearby drivers and select
    // the single best one to receive the next offer.
    private double calculateScore(double distanceKm, double rating) {
        // Inverse of distance — closer drivers score higher on this component.
        // Adding 1.0 prevents division by zero when the driver is exactly at the pickup location
        // (distanceKm = 0.0 → distanceScore = 1.0, the maximum possible value).
        double distanceScore = 1.0 / (1.0 + distanceKm);

        // Normalise rating to the 0.0–1.0 range assuming the maximum possible rating is 5.0.
        // A 5.0-rated driver scores 1.0; a 1.0-rated driver scores 0.2.
        double ratingScore = rating / 5.0;

        // Weighted combination: proximity matters more than rating because passenger wait
        // time is the highest-priority metric in a ride-sharing context. A driver 0.5 km
        // away with a 3.5 rating will still score higher than a 5.0-rated driver 4 km away.
        return (0.6 * distanceScore) + (0.4 * ratingScore);
    }
}

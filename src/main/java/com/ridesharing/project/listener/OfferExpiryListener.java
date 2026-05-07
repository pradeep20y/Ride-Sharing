package com.ridesharing.project.listener;

import com.ridesharing.project.entity.RideRequest;
import com.ridesharing.project.entity.RideRequestStatus;
import com.ridesharing.project.repository.RideRequestRepository;
import com.ridesharing.project.service.RideMatchingService;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

// Listens for Redis keyspace expiry events and triggers the next step of the
// matching algorithm when a driver's offer timer key expires.
// This is the primary mechanism (with OfferExpirationJob as fallback) that advances
// the matching state machine when a driver does not respond within 10 seconds.
// Registered in RedisListenerConfig on the "__keyevent@0__:expired" channel.
// Interacts with: RideMatchingService (to advance matching), RideRequestRepository
// (to load the current request state before delegating).
@Component
public class OfferExpiryListener implements MessageListener {

    // Prefix used by RideMatchingService when writing offer timer keys.
    // Only keys matching this prefix represent ride offer timers — all other
    // Redis key expiry events (e.g. from location tracking or session caching) are ignored.
    private static final String OFFER_KEY_PREFIX = "offer:";

    private final RideMatchingService rideMatchingService;
    private final RideRequestRepository rideRequestRepository;

    public OfferExpiryListener(RideMatchingService rideMatchingService,
                                RideRequestRepository rideRequestRepository) {
        this.rideMatchingService = rideMatchingService;
        this.rideRequestRepository = rideRequestRepository;
    }

    // Called by the Redis listener container whenever any key expires in database 0.
    // Filters to only process "offer:{requestId}" keys, then loads the corresponding
    // RideRequest from MySQL and delegates advancement to RideMatchingService.
    // Guards against processing events for requests that are already MATCHED or CANCELLED —
    // the driver may have accepted or another thread may have cancelled just before
    // or at the exact moment the Redis key expired.
    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = new String(message.getBody());

        // Ignore all Redis expiry events that are not ride offer timer keys
        if (!expiredKey.startsWith(OFFER_KEY_PREFIX)) {
            return;
        }

        // Extract the requestId from the key format: "offer:{requestId}" → requestId
        String requestId = expiredKey.substring(OFFER_KEY_PREFIX.length());

        // Load the current state of the ride request from MySQL.
        // We must re-read from the database here because the in-memory state from
        // before the TTL was set may be stale — the driver could have accepted
        // between when the offer was sent and when this event fired.
        RideRequest rideRequest = rideRequestRepository.findById(requestId).orElse(null);

        // Request was deleted from the database — nothing to advance
        if (rideRequest == null) {
            return;
        }

        // Only advance if the request is still actively waiting for a driver response.
        // If status is MATCHED the driver accepted just before expiry — do not advance.
        // If status is CANCELLED the request was already terminated — do not advance.
        // moveToNextDriver also has this guard, but checking here avoids an unnecessary
        // service call and transaction overhead for already-resolved requests.
        if (rideRequest.getStatus() != RideRequestStatus.OFFER_PENDING) {
            return;
        }

        // Delegate to RideMatchingService — it will score remaining drivers and
        // either send the next offer or cancel the request if all attempts are exhausted.
        // OptimisticLockException from @Version conflict (race with accept) propagates
        // up and is swallowed by the listener container; OfferExpirationJob will catch
        // this case on its next 30-second sweep via the MySQL offerExpiresAt column.
        rideMatchingService.moveToNextDriver(rideRequest);
    }
}

package com.ridesharing.project.service;

import com.ridesharing.project.dto.request.RegisterRequest;
import com.ridesharing.project.dto.request.SetDriverStatusRequest;
import com.ridesharing.project.dto.request.UpdateDriverRatingRequest;
import com.ridesharing.project.dto.request.UpdateLocationRequest;
import com.ridesharing.project.dto.request.UpdateVehicleRequest;
import com.ridesharing.project.dto.response.AuthResponse;
import com.ridesharing.project.dto.response.DriverResponse;
import com.ridesharing.project.entity.Driver;
import com.ridesharing.project.entity.User;
import com.ridesharing.project.exception.BusinessException;
import com.ridesharing.project.exception.ResourceNotFoundException;
import com.ridesharing.project.repository.DriverRepository;
import com.ridesharing.project.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Business logic layer for all Driver operations.
 *
 * <p>This service is the single authority on driver state — controllers and future
 * internal callers (e.g., matching, payment) should always go through here rather
 * than touching the repository directly.  All mutating methods are annotated with
 * {@code @Transactional} so that partial updates are automatically rolled back on
 * failure.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Validate business rules (unique license plate, user existence, etc.)</li>
 *   <li>Map between domain entities and response DTOs</li>
 *   <li>Coordinate repository calls and keep entity state consistent</li>
 * </ul>
 *
 * TODO: Phase 2 — Inject a WebSocket broadcast service and push location updates
 *       to subscribed passengers in real time.
 * TODO: Phase 3 — Expose driver availability events to the matching algorithm via
 *       an in-process event or a Kafka topic.
 * TODO: Phase 6 — Delegate earnings accumulation to the Payment service so that
 *       totalEarnings is sourced from settled payment records.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)  // default to read-only; mutating methods override this
public class DriverService {

    private static final Logger log = LoggerFactory.getLogger(DriverService.class);

    // Constructor injection is preferred over @Autowired field injection:
    //  - makes dependencies explicit and visible
    //  - simplifies unit testing (no reflection required)
    private final DriverRepository driverRepository;
    private final UserRepository   userRepository;
    private final LocationService locationService;



    // ── Method ───────────────────────────────────────────────────────────────────

    /**
     * Registers a new driver account in a single atomic transaction.
     *
     * <p>Both the User record (userType=DRIVER) and the Driver profile are
     * persisted together. If either insert fails — duplicate email, duplicate
     * license plate, or any DB error — the entire transaction is rolled back
     * and no partial data is written.
     *
     * @param request validated registration payload
     * @return AuthResponse containing the new user's id and basic profile
     * @throws BusinessException if the email or license plate is already taken
     */
    

    // ── Create ────────────────────────────────────────────────────────────────

    /**
     * Registers a new driver profile linked to an existing user account.
     *
     * <p>Business rules enforced:
     * <ul>
     *   <li>The user must already exist (validated by userId lookup).</li>
     *   <li>A user can only have one driver profile (enforced by DB unique constraint
     *       on user_id and caught here as a {@link BusinessException}).</li>
     *   <li>The license plate must be unique across all drivers.</li>
     * </ul>
     *
     * @param request validated registration payload
     * @return DriverResponse representing the newly created driver
     * @throws ResourceNotFoundException if the referenced user does not exist
     * @throws BusinessException         if the license plate is already in use, or
     *                                   if the user already has a driver profile
     */
    /* @Transactional
    public DriverResponse registerDriver(RegisterDriverRequest request) {
        log.info("Registering driver for userId={}, licensePlate={}", request.getUserId(), request.getLicensePlate());

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getUserId()));

        // Prevent a user from owning multiple driver profiles
        if (driverRepository.findByUser(user).isPresent()) {
            throw new BusinessException(
                "User '" + user.getName() + "' already has a registered driver profile. "
                + "Each user account may only have one driver profile.");
        }

        // License plate must be globally unique
        if (driverRepository.existsByLicensePlate(request.getLicensePlate())) {
            throw new BusinessException(
                "License plate '" + request.getLicensePlate() + "' is already registered to another driver.");
        }

        Driver driver = new Driver();
        driver.setUser(user);
        driver.setLicensePlate(request.getLicensePlate());
        driver.setVehicleType(request.getVehicleType());
        driver.setStatus("OFFLINE");      // new drivers start offline until they go on duty
        driver.setRating(5.0);            // default starting rating
        driver.setTotalEarnings(0.0);
        driver.setTotalRides(0);
        driver.setAcceptanceRate(100.0);  // perfect acceptance rate at start

        Driver saved = driverRepository.save(driver);
        log.info("Driver registered successfully: driverId={}", saved.getId());
        return toResponse(saved);
    } */

    // ── Read ──────────────────────────────────────────────────────────────────

    /**
     * Returns all driver profiles in the system (unfiltered, unsorted).
     *
     * <p>For large datasets consider adding pagination (Phase 4 — scalability).
     *
     * @return list of all drivers; empty list when none exist
     */
    public List<DriverResponse> getAllDrivers() {
        return driverRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Looks up a single driver profile by its unique driver ID.
     *
     * @param driverId UUID of the driver profile
     * @return DriverResponse for the found driver
     * @throws ResourceNotFoundException if no driver exists with the given ID
     */
    public DriverResponse getDriverById(String driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver", "id", driverId));
        return toResponse(driver);
    }

    /**
     * Looks up the driver profile associated with a given user account.
     * Useful when a client knows the user's ID (e.g., from a JWT) but needs
     * the driver-specific data.
     *
     * @param userId UUID of the user account
     * @return DriverResponse for the driver linked to that user
     * @throws ResourceNotFoundException if the user does not exist, or the user
     *                                   has not registered as a driver
     */
    public DriverResponse getDriverByUserId(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Driver driver = driverRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Driver", "userId", userId));

        return toResponse(driver);
    }

    /**
     * Returns all ONLINE drivers sorted by rating (highest first).
     * This is the primary feed used by the matching algorithm to find
     * candidates for a new ride request.
     *
     * TODO: Phase 3 — Replace this DB query with a geospatial query so only
     *       drivers within a configurable radius are returned.
     * TODO: Phase 4 — Move the result set to a Redis sorted set for O(log N)
     *       lookups instead of a full table scan.
     *
     * @return list of ONLINE drivers ordered by rating descending; empty if none
     */
    public List<DriverResponse> getOnlineDrivers() {
        return driverRepository.findByStatusOrderByRatingDesc("ONLINE")
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Returns all drivers that currently have the given status.
     *
     * @param status one of OFFLINE, ONLINE, ON_TRIP
     * @return filtered list of drivers; empty if none match
     */
    public List<DriverResponse> getDriversByStatus(String status) {
        return driverRepository.findByStatus(status)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Returns all drivers operating a vehicle of the given type.
     *
     * @param vehicleType one of ECONOMY, COMFORT, PREMIUM
     * @return filtered list of drivers; empty if none match
     */
    public List<DriverResponse> getDriversByVehicleType(String vehicleType) {
        return driverRepository.findByVehicleType(vehicleType)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── Update ────────────────────────────────────────────────────────────────

    /**
     * Changes the driver's availability status (OFFLINE / ONLINE / ON_TRIP).
     *
     * <p>Drivers set themselves ONLINE when they start a shift and OFFLINE
     * when they finish.  ON_TRIP is typically set by the system when a ride
     * begins (Phase 3), but this endpoint allows manual override for MVP.
     *
     * TODO: Phase 3 — Enforce valid state-transition graph and reject illegal
     *       transitions (e.g., OFFLINE → ON_TRIP without going through ONLINE).
     *
     * @param driverId UUID of the driver
     * @param request  contains the target status
     * @return updated DriverResponse
     * @throws ResourceNotFoundException if the driver does not exist
     */
    @Transactional
    public DriverResponse setDriverStatus(String driverId, SetDriverStatusRequest request) {
        log.info("Updating status for driverId={} to {}", driverId, request.getStatus());

        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver", "id", driverId));
        if ("OFFLINE".equals(request.getStatus())) {
            // Stop this driver from appearing in nearby-driver searches the instant
            // they go offline, rather than waiting for a stale GEO entry to confuse matching.
            locationService.removeDriverLocation(driverId);
        }
        driver.setStatus(request.getStatus());
        System.out.println("DRIVER COUNTTTTTTTTTTTTTTT "+
        locationService.getOnlineDriverCount());
        return toResponse(driverRepository.save(driver));
    }

    /**
     * Records the driver's latest GPS coordinates and timestamps the update.
     *
     * <p>This is called periodically by the driver's mobile app while ONLINE
     * or ON_TRIP.  The {@code lastLocationUpdate} timestamp lets the system
     * detect stale location data (e.g., driver whose app crashed).
     *
     * TODO: Phase 2 — After persisting, broadcast the new coordinates via
     *       WebSocket to any passengers currently tracking this driver.
     * TODO: Phase 4 — Publish to a Redis GEO set for fast radius queries.
     *
     * @param driverId UUID of the driver
     * @param request  contains latitude and longitude
     * @return updated DriverResponse
     * @throws ResourceNotFoundException if the driver does not exist
     */
    @Transactional
    public DriverResponse updateDriverLocation(String driverId, UpdateLocationRequest request) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver", "id", driverId));

        driver.setCurrentLatitude(request.getLatitude());
        driver.setCurrentLongitude(request.getLongitude());
        driver.setLastLocationUpdate(LocalDateTime.now());

        return toResponse(driverRepository.save(driver));
    }

    /**
     * Updates the driver's vehicle details (license plate and/or vehicle type).
     *
     * <p>If the new license plate differs from the current one, uniqueness is
     * re-validated.  If only the vehicle type changes the license plate check
     * is skipped to avoid a spurious conflict error.
     *
     * @param driverId UUID of the driver
     * @param request  contains new licensePlate and vehicleType
     * @return updated DriverResponse
     * @throws ResourceNotFoundException if the driver does not exist
     * @throws BusinessException         if the new license plate belongs to another driver
     */
    @Transactional
    public DriverResponse updateVehicle(String driverId, UpdateVehicleRequest request) {
        log.info("Updating vehicle for driverId={}", driverId);

        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver", "id", driverId));

        // Only validate uniqueness when the plate is actually changing
        boolean plateChanged = !request.getLicensePlate().equals(driver.getLicensePlate());
        if (plateChanged && driverRepository.existsByLicensePlate(request.getLicensePlate())) {
            throw new BusinessException(
                "License plate '" + request.getLicensePlate() + "' is already registered to another driver.");
        }

        driver.setLicensePlate(request.getLicensePlate());
        driver.setVehicleType(request.getVehicleType());

        return toResponse(driverRepository.save(driver));
    }

    /**
     * Sets the driver's average rating to the supplied value.
     *
     * <p>In Phase 1 this is a direct override.  Phase 7 will replace this with
     * a weighted rolling average computed from individual trip ratings.
     *
     * TODO: Phase 7 — Replace with weighted average calculation from the Ratings service.
     *
     * @param driverId UUID of the driver
     * @param request  contains the new rating value (0.0–5.0)
     * @return updated DriverResponse
     * @throws ResourceNotFoundException if the driver does not exist
     */
    @Transactional
    public DriverResponse updateDriverRating(String driverId, UpdateDriverRatingRequest request) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver", "id", driverId));

        driver.setRating(request.getRating());
        return toResponse(driverRepository.save(driver));
    }

    /**
     * Adds the fare amount for a completed trip to the driver's cumulative earnings
     * and increments the total ride count.
     *
     * <p>This method is intended to be called internally by the Trip/Payment service
     * when a ride is settled, not directly by an HTTP client.
     *
     * TODO: Phase 6 — Replace direct DB update with an event consumer that processes
     *       PaymentSettled events from Kafka to decouple the payment and driver services.
     *
     * @param driverId   UUID of the driver
     * @param fareAmount amount to add to totalEarnings (must be positive)
     * @return updated DriverResponse
     * @throws ResourceNotFoundException if the driver does not exist
     * @throws BusinessException         if fareAmount is not positive
     */
    @Transactional
    public DriverResponse addEarnings(String driverId, Double fareAmount) {
        if (fareAmount == null || fareAmount <= 0) {
            throw new BusinessException("Fare amount must be a positive value, got: " + fareAmount);
        }

        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver", "id", driverId));

        driver.setTotalEarnings(driver.getTotalEarnings() + fareAmount);
        driver.setTotalRides(driver.getTotalRides() + 1);

        log.info("Added earnings {} to driverId={}, new total={}", fareAmount, driverId, driver.getTotalEarnings());
        return toResponse(driverRepository.save(driver));
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    /**
     * Permanently deletes a driver profile.
     *
     * <p>If no driver with the given ID exists the method completes silently
     * (idempotent DELETE behaviour — deleting a non-existent resource is not
     * an error from the client's perspective).
     *
     * TODO: Phase 9 — Replace hard delete with a soft delete (isDeleted flag) to
     *       preserve historical trip/earnings records for auditing.
     *
     * @param driverId UUID of the driver to delete
     */
    @Transactional
    public void deleteDriver(String driverId) {
        // Verify existence first so we can return a meaningful 404 instead of
        // silently doing nothing when the caller expects the resource to be there.
        if (!driverRepository.existsById(driverId)) {
            throw new ResourceNotFoundException("Driver", "id", driverId);
        }
        log.info("Deleting driverId={}", driverId);
        driverRepository.deleteById(driverId);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Maps a {@link Driver} entity (and its nested {@link User}) to the API
     * response DTO.  Keeping this mapping in one place ensures all endpoints
     * return a consistent shape and makes future field additions easy to maintain.
     *
     * @param driver the entity retrieved from the database
     * @return fully populated DriverResponse
     */
    private DriverResponse toResponse(Driver driver) {
        return DriverResponse.builder()
                .id(driver.getId())
                .userId(driver.getUser().getId())
                .userName(driver.getUser().getName())
                .userEmail(driver.getUser().getEmail())
                .userPhone(driver.getUser().getPhone())
                .licensePlate(driver.getLicensePlate())
                .vehicleType(driver.getVehicleType())
                .status(driver.getStatus())
                .currentLatitude(driver.getCurrentLatitude())
                .currentLongitude(driver.getCurrentLongitude())
                .lastLocationUpdate(driver.getLastLocationUpdate())
                .rating(driver.getRating())
                .totalEarnings(driver.getTotalEarnings())
                .totalRides(driver.getTotalRides())
                .acceptanceRate(driver.getAcceptanceRate())
                .createdAt(driver.getCreatedAt())
                .updatedAt(driver.getUpdatedAt())
                .build();
    }
}

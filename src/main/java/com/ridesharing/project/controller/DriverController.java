package com.ridesharing.project.controller;

import com.ridesharing.project.dto.request.RegisterDriverRequest;
import com.ridesharing.project.dto.request.SetDriverStatusRequest;
import com.ridesharing.project.dto.request.UpdateDriverRatingRequest;
import com.ridesharing.project.dto.request.UpdateLocationRequest;
import com.ridesharing.project.dto.request.UpdateVehicleRequest;
import com.ridesharing.project.dto.response.ApiResponse;
import com.ridesharing.project.dto.response.DriverResponse;
import com.ridesharing.project.service.DriverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller exposing all Driver endpoints under {@code /api/drivers}.
 *
 * <p>This controller is intentionally thin — it owns only HTTP concerns:
 * request parsing, input validation (via {@code @Valid}), response wrapping,
 * and HTTP status codes.  All business logic is delegated to {@link DriverService}.
 *
 * <p>Error handling is centralised in
 * {@link com.ridesharing.project.exception.GlobalExceptionHandler} so no try/catch
 * blocks are needed here.
 *
 * <p>Swagger UI is available at {@code /swagger-ui.html} after the application starts.
 *
 * TODO: Phase 2 — Add a WebSocket endpoint alongside the REST location update endpoint
 *       so that drivers can stream location updates without repeated HTTP round-trips.
 * TODO: Phase 9 — Add JWT-based security so that only authenticated drivers can update
 *       their own profile, and only admins can delete or manually set ratings.
 */
@RestController
@RequestMapping("/drivers")
@CrossOrigin(origins = "*")
@Tag(name = "Drivers", description = "Operations for managing driver profiles — registration, status, location, vehicle, and ratings")
public class DriverController {

    private final DriverService driverService;

    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    // ── Create ────────────────────────────────────────────────────────────────

    /**
     * Registers a new driver profile linked to an existing user account.
     * The driver starts in OFFLINE status and will appear in ride queries
     * only after they set their status to ONLINE.
     *
     * @param request validated registration payload (userId, licensePlate, vehicleType)
     * @return 201 Created with the new driver's full profile
     */
 /*    @PostMapping
    @Operation(
        summary = "Register a new driver",
        description = "Creates a driver profile for an existing user account. "
                    + "The user must not already have a driver profile. "
                    + "The license plate must be globally unique."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Driver registered successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed — check fieldErrors in the response",
            content = @Content(schema = @Schema(implementation = com.ridesharing.project.dto.response.ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Referenced user not found",
            content = @Content(schema = @Schema(implementation = com.ridesharing.project.dto.response.ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "License plate already in use, or user already has a driver profile",
            content = @Content(schema = @Schema(implementation = com.ridesharing.project.dto.response.ErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<DriverResponse>> registerDriver(
            @Valid @RequestBody RegisterDriverRequest request) {

        DriverResponse driver = driverService.registerDriver(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(driver, "Driver registered successfully"));
    }
 */
    // ── Read ──────────────────────────────────────────────────────────────────

    /**
     * Returns every driver profile in the system, unfiltered.
     * Use the status or vehicle-type endpoints for filtered results.
     *
     * @return 200 OK with a (possibly empty) list of all drivers
     */
    @GetMapping
    @Operation(
        summary = "Get all drivers",
        description = "Returns all registered driver profiles. Use /status/{status} or /vehicle/{type} for filtered results."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "List of drivers returned (may be empty)")
    public ResponseEntity<ApiResponse<List<DriverResponse>>> getAllDrivers() {
        List<DriverResponse> drivers = driverService.getAllDrivers();
        return ResponseEntity.ok(ApiResponse.success(drivers,
                drivers.size() + " driver(s) found"));
    }

    /**
     * Retrieves a single driver by their unique driver profile ID.
     *
     * @param id UUID of the driver profile
     * @return 200 OK with the driver's profile, or 404 if not found
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get driver by ID", description = "Retrieves a driver profile using the driver's unique UUID.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Driver found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Driver not found",
            content = @Content(schema = @Schema(implementation = com.ridesharing.project.dto.response.ErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<DriverResponse>> getDriverById(
            @Parameter(description = "Driver profile UUID", example = "d1e2f3a4-b5c6-7890-abcd-ef1234567890")
            @PathVariable String id) {

        DriverResponse driver = driverService.getDriverById(id);
        return ResponseEntity.ok(ApiResponse.success(driver, "Driver found"));
    }

    /**
     * Retrieves the driver profile associated with a given user account.
     * Useful when the caller has a user ID (e.g., from an auth token) and
     * needs the driver-specific data.
     *
     * @param userId UUID of the user account
     * @return 200 OK with the driver's profile, or 404 if user/driver not found
     */
    @GetMapping("/user/{userId}")
    @Operation(
        summary = "Get driver by user ID",
        description = "Looks up the driver profile linked to the specified user account."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Driver found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User or driver profile not found",
            content = @Content(schema = @Schema(implementation = com.ridesharing.project.dto.response.ErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<DriverResponse>> getDriverByUserId(
            @Parameter(description = "User account UUID", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String userId) {

        DriverResponse driver = driverService.getDriverByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(driver, "Driver found"));
    }

    /**
     * Returns all ONLINE drivers sorted by rating (highest first).
     * This is the primary feed for the matching algorithm.
     *
     * @return 200 OK with the list of online drivers ordered by rating desc
     */
    @GetMapping("/status/online")
    @Operation(
        summary = "Get online drivers (sorted by rating)",
        description = "Returns all drivers with status ONLINE, sorted by rating descending. "
                    + "Used by the ride-matching algorithm to find available candidates."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Online drivers returned (may be empty)")
    public ResponseEntity<ApiResponse<List<DriverResponse>>> getOnlineDrivers() {
        List<DriverResponse> drivers = driverService.getOnlineDrivers();
        return ResponseEntity.ok(ApiResponse.success(drivers,
                drivers.size() + " online driver(s) found"));
    }

    /**
     * Returns all drivers that currently have the specified status.
     *
     * @param status one of OFFLINE, ONLINE, ON_TRIP
     * @return 200 OK with the filtered driver list
     */
    @GetMapping("/status/{status}")
    @Operation(
        summary = "Get drivers by status",
        description = "Filters drivers by availability status. Valid values: OFFLINE, ONLINE, ON_TRIP."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Filtered driver list returned (may be empty)")
    public ResponseEntity<ApiResponse<List<DriverResponse>>> getDriversByStatus(
            @Parameter(description = "Driver status filter", example = "ONLINE",
                       schema = @Schema(allowableValues = {"OFFLINE", "ONLINE", "ON_TRIP"}))
            @PathVariable String status) {

        List<DriverResponse> drivers = driverService.getDriversByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(drivers,
                drivers.size() + " driver(s) found with status " + status));
    }

    /**
     * Returns all drivers operating a vehicle of the specified type.
     *
     * @param type one of ECONOMY, COMFORT, PREMIUM
     * @return 200 OK with the filtered driver list
     */
    @GetMapping("/vehicle/{type}")
    @Operation(
        summary = "Get drivers by vehicle type",
        description = "Filters drivers by their vehicle category. Valid values: ECONOMY, COMFORT, PREMIUM."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Filtered driver list returned (may be empty)")
    public ResponseEntity<ApiResponse<List<DriverResponse>>> getDriversByVehicleType(
            @Parameter(description = "Vehicle type filter", example = "ECONOMY",
                       schema = @Schema(allowableValues = {"ECONOMY", "COMFORT", "PREMIUM"}))
            @PathVariable String type) {

        List<DriverResponse> drivers = driverService.getDriversByVehicleType(type);
        return ResponseEntity.ok(ApiResponse.success(drivers,
                drivers.size() + " driver(s) found with vehicle type " + type));
    }

    // ── Update ────────────────────────────────────────────────────────────────

    /**
     * Changes the driver's availability status.
     * Drivers set themselves ONLINE at the start of a shift and OFFLINE at the end.
     * ON_TRIP is typically managed by the system in Phase 3.
     *
     * @param id      UUID of the driver
     * @param request contains the target status
     * @return 200 OK with the updated driver profile
     */
    @PutMapping("/{id}/status")
    @Operation(
        summary = "Update driver status",
        description = "Changes the driver's availability status. "
                    + "Drivers set themselves ONLINE/OFFLINE; ON_TRIP is usually managed by the system."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Status updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid status value",
            content = @Content(schema = @Schema(implementation = com.ridesharing.project.dto.response.ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Driver not found",
            content = @Content(schema = @Schema(implementation = com.ridesharing.project.dto.response.ErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<DriverResponse>> setDriverStatus(
            @Parameter(description = "Driver profile UUID") @PathVariable String id,
            @Valid @RequestBody SetDriverStatusRequest request) {

        DriverResponse driver = driverService.setDriverStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success(driver,
                "Driver status updated to " + request.getStatus()));
    }

    /**
     * Records the driver's latest GPS coordinates.
     * Called periodically by the driver's mobile app while on duty.
     *
     * TODO: Phase 2 — Add a complementary WebSocket endpoint for streaming location
     *       updates to reduce the overhead of repeated HTTP calls.
     *
     * @param id      UUID of the driver
     * @param request contains latitude and longitude
     * @return 200 OK with the updated driver profile (including new coordinates)
     */
    @PutMapping("/{id}/location")
    @Operation(
        summary = "Update driver location",
        description = "Records the driver's current GPS coordinates and timestamps the update. "
                    + "Call this periodically from the driver app while the driver is on duty."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Location updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Coordinates out of valid range",
            content = @Content(schema = @Schema(implementation = com.ridesharing.project.dto.response.ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Driver not found",
            content = @Content(schema = @Schema(implementation = com.ridesharing.project.dto.response.ErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<DriverResponse>> updateLocation(
            @Parameter(description = "Driver profile UUID") @PathVariable String id,
            @Valid @RequestBody UpdateLocationRequest request) {

        DriverResponse driver = driverService.updateDriverLocation(id, request);
        return ResponseEntity.ok(ApiResponse.success(driver, "Driver location updated successfully"));
    }

    /**
     * Updates the driver's vehicle details (license plate and vehicle type).
     * If only the type changes, no additional uniqueness check is performed
     * for the plate.
     *
     * @param id      UUID of the driver
     * @param request contains new licensePlate and vehicleType
     * @return 200 OK with the updated driver profile
     */
    @PutMapping("/{id}/vehicle")
    @Operation(
        summary = "Update driver vehicle",
        description = "Updates the license plate and/or vehicle category for a driver. "
                    + "The new license plate must be globally unique if it differs from the current one."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Vehicle updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed",
            content = @Content(schema = @Schema(implementation = com.ridesharing.project.dto.response.ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Driver not found",
            content = @Content(schema = @Schema(implementation = com.ridesharing.project.dto.response.ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "License plate already in use",
            content = @Content(schema = @Schema(implementation = com.ridesharing.project.dto.response.ErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<DriverResponse>> updateVehicle(
            @Parameter(description = "Driver profile UUID") @PathVariable String id,
            @Valid @RequestBody UpdateVehicleRequest request) {

        DriverResponse driver = driverService.updateVehicle(id, request);
        return ResponseEntity.ok(ApiResponse.success(driver, "Driver vehicle updated successfully"));
    }

    /**
     * Sets the driver's average rating.
     * Phase 1: direct override.
     * Phase 7: will become a weighted average from the Ratings service.
     *
     * TODO: Phase 7 — Replace with a weighted rolling average endpoint that
     *       accepts a single trip rating and recalculates the driver average.
     *
     * @param id      UUID of the driver
     * @param request contains the new rating value (0.0–5.0)
     * @return 200 OK with the updated driver profile
     */
    @PutMapping("/{id}/rating")
    @Operation(
        summary = "Update driver rating",
        description = "Sets the driver's average rating. Currently a direct override. "
                    + "Will be replaced with a weighted average in Phase 7."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Rating updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Rating out of range (must be 0.0–5.0)",
            content = @Content(schema = @Schema(implementation = com.ridesharing.project.dto.response.ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Driver not found",
            content = @Content(schema = @Schema(implementation = com.ridesharing.project.dto.response.ErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<DriverResponse>> updateRating(
            @Parameter(description = "Driver profile UUID") @PathVariable String id,
            @Valid @RequestBody UpdateDriverRatingRequest request) {

        DriverResponse driver = driverService.updateDriverRating(id, request);
        return ResponseEntity.ok(ApiResponse.success(driver,
                "Driver rating updated to " + request.getRating()));
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    /**
     * Permanently deletes a driver profile.
     * The linked user account is NOT deleted — only the driver profile is removed.
     *
     * TODO: Phase 9 — Switch to a soft delete (isDeleted flag) to retain historical
     *       trip and earnings data for compliance and auditing purposes.
     *
     * @param id UUID of the driver to delete
     * @return 200 OK with a success message, or 404 if not found
     */
    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete driver",
        description = "Permanently deletes a driver profile. The linked user account is not affected. "
                    + "Returns 404 if the driver does not exist."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Driver deleted successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Driver not found",
            content = @Content(schema = @Schema(implementation = com.ridesharing.project.dto.response.ErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<Void>> deleteDriver(
            @Parameter(description = "Driver profile UUID") @PathVariable String id) {

        driverService.deleteDriver(id);
        return ResponseEntity.ok(ApiResponse.success("Driver deleted successfully"));
    }
}

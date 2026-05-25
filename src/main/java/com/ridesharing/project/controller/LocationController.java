package com.ridesharing.project.controller;

import com.ridesharing.project.dto.request.LocationUpdateRequest;
import com.ridesharing.project.dto.response.ApiResponse;
import com.ridesharing.project.dto.response.ErrorResponse;
import com.ridesharing.project.dto.response.NearbyDriverResponse;
import com.ridesharing.project.service.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.geo.Point;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "Location", description = "Driver location tracking — update, remove, and query GPS positions stored in Redis")
@RestController
@RequestMapping("/location")
@CrossOrigin(origins = "*")
@Validated
public class    LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    // Stores the driver's current GPS coordinates in Redis.
    // Called periodically by the driver app while the driver is on duty so that
    // nearby-driver queries always reflect up-to-date positions.
    @Operation(
        summary = "Update driver location",
        description = "Stores the driver's current GPS coordinates in Redis. Called periodically by the driver app while the driver is on duty."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Location updated successfully",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid coordinates — latitude or longitude out of range or missing",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping("/driver/{driverId}")
    public ResponseEntity<Map<String, String>> updateDriverLocation(
            @Parameter(description = "Unique identifier of the driver", required = true)
            @PathVariable String driverId,
            @Valid @RequestBody LocationUpdateRequest request) {

        locationService.updateDriverLocation(driverId, request.getLatitude(), request.getLongitude());
        return ResponseEntity.ok(Map.of(
                "message", "Location updated successfully for driver " + driverId,
                "driverId", driverId
        ));
    }

    // Removes the driver from the Redis GEO index.
    // Called when the driver goes offline so they no longer appear in nearby-driver searches.
    @Operation(
        summary = "Remove driver from location tracking",
        description = "Removes the driver from the Redis GEO index. Called when the driver goes offline so they no longer appear in nearby-driver searches."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Driver successfully removed from active location tracking",
            content = @Content(schema = @Schema(implementation = Map.class))
        )
    })
    @DeleteMapping("/driver/{driverId}")
    public ResponseEntity<Map<String, String>> removeDriverLocation(
            @Parameter(description = "Unique identifier of the driver", required = true)
            @PathVariable String driverId) {
        locationService.removeDriverLocation(driverId);
        return ResponseEntity.ok(Map.of(
                "message", "Driver " + driverId + " removed from active location tracking"
        ));
    }

    // Primary endpoint for ride-matching — returns all drivers within the default 5 km radius.
    // Called by the passenger app immediately after a ride request is created to show nearby options.
    @Operation(
        summary = "Find nearby drivers (default radius)",
        description = "Returns all active drivers within the default 5 km radius of the given coordinates. Primary endpoint for ride-matching."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "List of nearby drivers returned successfully",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Missing or invalid latitude/longitude parameters",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse<List<NearbyDriverResponse>>> findNearbyDrivers(
            @Parameter(description = "Latitude of the search origin (decimal degrees, -90 to 90)", required = true)
            @NotNull(message = "Latitude is required")  @RequestParam Double latitude,
            @Parameter(description = "Longitude of the search origin (decimal degrees, -180 to 180)", required = true)
            @NotNull(message = "Longitude is required") @RequestParam Double longitude) {

        List<NearbyDriverResponse> drivers = locationService.findNearbyDrivers(latitude, longitude);
        return ResponseEntity.ok(ApiResponse.success(drivers,
                drivers.size() + " driver(s) found nearby"));
    }

    // Same as /nearby but allows the caller to specify a custom search radius.
    // Use this when the default 5 km radius yields too few results (sparse area)
    // or when the client wants to show a broader map view of available drivers.
    @Operation(
        summary = "Find nearby drivers with custom radius",
        description = "Returns active drivers within a caller-specified radius (0.1–50.0 km). Use when the default 5 km radius yields too few results or a broader map view is needed."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "List of nearby drivers within the specified radius returned successfully",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid radius — must be between 0.1 and 50.0 km, or missing required parameters",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/nearby/radius")
    public ResponseEntity<ApiResponse<List<NearbyDriverResponse>>> findNearbyDriversWithRadius(
            @Parameter(description = "Latitude of the search origin (decimal degrees, -90 to 90)", required = true)
            @NotNull(message = "Latitude is required")  @RequestParam Double latitude,
            @Parameter(description = "Longitude of the search origin (decimal degrees, -180 to 180)", required = true)
            @NotNull(message = "Longitude is required") @RequestParam Double longitude,
            @Parameter(description = "Search radius in kilometres (0.1 – 50.0)", required = true)
            @NotNull(message = "Radius is required")
            @DecimalMin(value = "0.1",  inclusive = true, message = "Radius must be at least 0.1 km")
            @DecimalMax(value = "50.0", inclusive = true, message = "Radius must be at most 50.0 km")
            @RequestParam Double radius) {

        List<NearbyDriverResponse> drivers = locationService.findNearbyDrivers(latitude, longitude, radius);
        return ResponseEntity.ok(ApiResponse.success(drivers,
                drivers.size() + " driver(s) found within " + radius + " km"));
    }

    // Returns the last known GPS position for a single driver.
    // A null position means the driver is offline — the caller should treat them as unavailable.
    @Operation(
        summary = "Get driver location",
        description = "Returns the last known GPS position for a single driver. A null/404 response means the driver is offline or has no recorded position."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Driver location returned successfully",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Driver not found in Redis — driver is offline or has never reported a position",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/driver/{driverId}")
    public ResponseEntity<Map<String, Object>> getDriverLocation(
            @Parameter(description = "Unique identifier of the driver", required = true)
            @PathVariable String driverId) {
        Point location = locationService.getDriverLocation(driverId);
        if (location == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Driver " + driverId + " is not currently online or has no recorded position"));
        }
        return ResponseEntity.ok(Map.of(
                "driverId",  driverId,
                "latitude",  location.getY(), // Point.getY() is latitude in Spring Data Geo
                "longitude", location.getX()  // Point.getX() is longitude in Spring Data Geo
        ));
    }

    // Returns the total number of driver entries in the Redis GEO index.
    // Used by operations and monitoring dashboards to observe real-time fleet availability.
    @Operation(
        summary = "Get online driver count",
        description = "Returns the total number of driver entries currently in the Redis GEO index. Used by operations and monitoring dashboards to observe real-time fleet availability."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Online driver count returned successfully",
            content = @Content(schema = @Schema(implementation = Map.class))
        )
    })
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getOnlineDriverCount() {
        Long count = locationService.getOnlineDriverCount();
        return ResponseEntity.ok(Map.of("onlineDriverCount", count != null ? count : 0L));
    }
}

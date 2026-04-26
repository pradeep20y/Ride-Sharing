package com.ridesharing.project.controller;

import com.ridesharing.project.dto.request.LocationUpdateRequest;
import com.ridesharing.project.dto.response.ApiResponse;
import com.ridesharing.project.dto.response.NearbyDriverResponse;
import com.ridesharing.project.service.LocationService;
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

@RestController
@RequestMapping("/api/location")
@CrossOrigin(origins = "*")
@Validated
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    // Stores the driver's current GPS coordinates in Redis.
    // Called periodically by the driver app while the driver is on duty so that
    // nearby-driver queries always reflect up-to-date positions.
    @PostMapping("/driver/{driverId}")
    public ResponseEntity<Map<String, String>> updateDriverLocation(
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
    @DeleteMapping("/driver/{driverId}")
    public ResponseEntity<Map<String, String>> removeDriverLocation(@PathVariable String driverId) {
        locationService.removeDriverLocation(driverId);
        return ResponseEntity.ok(Map.of(
                "message", "Driver " + driverId + " removed from active location tracking"
        ));
    }

    // Primary endpoint for ride-matching — returns all drivers within the default 5 km radius.
    // Called by the passenger app immediately after a ride request is created to show nearby options.
    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse<List<NearbyDriverResponse>>> findNearbyDrivers(
            @NotNull(message = "Latitude is required")  @RequestParam Double latitude,
            @NotNull(message = "Longitude is required") @RequestParam Double longitude) {

        List<NearbyDriverResponse> drivers = locationService.findNearbyDrivers(latitude, longitude);
        return ResponseEntity.ok(ApiResponse.success(drivers,
                drivers.size() + " driver(s) found nearby"));
    }

    // Same as /nearby but allows the caller to specify a custom search radius.
    // Use this when the default 5 km radius yields too few results (sparse area)
    // or when the client wants to show a broader map view of available drivers.
    @GetMapping("/nearby/radius")
    public ResponseEntity<ApiResponse<List<NearbyDriverResponse>>> findNearbyDriversWithRadius(
            @NotNull(message = "Latitude is required")  @RequestParam Double latitude,
            @NotNull(message = "Longitude is required") @RequestParam Double longitude,
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
    @GetMapping("/driver/{driverId}")
    public ResponseEntity<Map<String, Object>> getDriverLocation(@PathVariable String driverId) {
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
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getOnlineDriverCount() {
        Long count = locationService.getOnlineDriverCount();
        return ResponseEntity.ok(Map.of("onlineDriverCount", count != null ? count : 0L));
    }
}

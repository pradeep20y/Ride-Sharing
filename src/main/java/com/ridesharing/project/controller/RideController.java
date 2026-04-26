package com.ridesharing.project.controller;

import com.ridesharing.project.dto.response.ApiResponse;
import com.ridesharing.project.dto.response.RideResponse;
import com.ridesharing.project.entity.Ride;
import com.ridesharing.project.service.RideService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

// This controller exposes REST endpoints for managing the full ride lifecycle from driver assignment through completion or cancellation.
@RestController
@RequestMapping("/rides")
@CrossOrigin(origins = "*")
public class RideController {

    @Autowired
    private RideService rideService;

    // Assigns the specified driver to the specified open ride request and creates a new ride record.
    @PostMapping("/{requestId}/assign/{driverId}")
    public ResponseEntity<ApiResponse<RideResponse>> createRide(
            @PathVariable String requestId,
            @PathVariable String driverId) {
        Ride ride = rideService.createRide(requestId, driverId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(convertToResponse(ride), "Driver assigned and ride created successfully"));
    }

    // Transitions the ride from Assigned to InProgress status when the driver begins the trip.
    @PutMapping("/{id}/start")
    public ResponseEntity<ApiResponse<RideResponse>> startRide(@PathVariable String id) {
        Ride ride = rideService.startRide(id);
        return ResponseEntity.ok(ApiResponse.success(convertToResponse(ride), "Ride started successfully"));
    }

    // Completes the in-progress ride and updates the driver's earnings and ride count.
    @PutMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<RideResponse>> completeRide(@PathVariable String id) {
        Ride ride = rideService.completeRide(id);
        return ResponseEntity.ok(ApiResponse.success(convertToResponse(ride), "Ride completed successfully"));
    }

    // Cancels the active ride and returns the driver to available status.
    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<RideResponse>> cancelRide(@PathVariable String id) {
        Ride ride = rideService.cancelRide(id);
        return ResponseEntity.ok(ApiResponse.success(convertToResponse(ride), "Ride cancelled successfully"));
    }

    // Returns the ride matching the given ride ID.
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RideResponse>> getRideById(@PathVariable String id) {
        Ride ride = rideService.getRideById(id);
        return ResponseEntity.ok(ApiResponse.success(convertToResponse(ride), "Ride retrieved successfully"));
    }

    // Returns all rides assigned to or completed by the driver with the given ID.
    @GetMapping("/driver/{driverId}")
    public ResponseEntity<ApiResponse<List<RideResponse>>> getRidesByDriver(@PathVariable String driverId) {
        List<RideResponse> responses = rideService.getRidesByDriver(driverId)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(responses, "Driver rides retrieved successfully"));
    }

    // Returns all rides taken by the passenger with the given ID.
    @GetMapping("/passenger/{passengerId}")
    public ResponseEntity<ApiResponse<List<RideResponse>>> getRidesByPassenger(@PathVariable String passengerId) {
        List<RideResponse> responses = rideService.getRidesByPassenger(passengerId)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(responses, "Passenger rides retrieved successfully"));
    }

    // Converts a Ride entity to a RideResponse DTO for inclusion in the API response.
    private RideResponse convertToResponse(Ride ride) {
        return RideResponse.builder()
                .id(ride.getId())
                .rideRequestId(ride.getRideRequest().getId())
                .passengerId(ride.getPassenger().getId())
                .passengerName(ride.getPassenger().getUser().getName())
                .driverId(ride.getDriver().getId())
                .driverName(ride.getDriver().getUser().getName())
                .driverLicensePlate(ride.getDriver().getLicensePlate())
                .status(ride.getStatus())
                .fare(ride.getFare())
                .assignedDate(ride.getAssignedDate())
                .completedDate(ride.getCompletedDate())
                .build();
    }
}

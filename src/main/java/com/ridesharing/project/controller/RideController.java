package com.ridesharing.project.controller;

import com.ridesharing.project.dto.response.ApiResponse;
import com.ridesharing.project.dto.response.ErrorResponse;
import com.ridesharing.project.dto.response.RideResponse;
import com.ridesharing.project.entity.Ride;
import com.ridesharing.project.service.RideService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

// This controller exposes REST endpoints for managing the full ride lifecycle from driver assignment through completion or cancellation.
@Tag(name = "Ride Management", description = "APIs for managing the full ride lifecycle: assignment, start, completion, cancellation, and retrieval")
@RestController
@RequestMapping("/rides")
@CrossOrigin(origins = "*")
public class RideController {

    @Autowired
    private RideService rideService;

    // Assigns the specified driver to the specified open ride request and creates a new ride record.
    @Operation(summary = "Assign driver and create ride", description = "Assigns the specified driver to the specified open ride request and creates a new ride record")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Ride created and driver assigned successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RideResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Ride request is not OPEN or driver is not ONLINE",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Ride request or driver not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{requestId}/assign/{driverId}")
    public ResponseEntity<ApiResponse<RideResponse>> createRide(
            @Parameter(description = "ID of the ride request to assign", required = true) @PathVariable String requestId,
            @Parameter(description = "ID of the driver to assign to the ride", required = true) @PathVariable String driverId) {
        Ride ride = rideService.createRide(requestId, driverId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(convertToResponse(ride), "Driver assigned and ride created successfully"));
    }

    // Transitions the ride from Assigned to InProgress status when the driver begins the trip.
    @Operation(summary = "Start a ride", description = "Transitions the ride from Assigned to InProgress status when the driver begins the trip")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Ride started successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RideResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Ride is not in Assigned status",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Ride not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}/start")
    public ResponseEntity<ApiResponse<RideResponse>> startRide(
            @Parameter(description = "ID of the ride to start", required = true) @PathVariable String id) {
        Ride ride = rideService.startRide(id);
        return ResponseEntity.ok(ApiResponse.success(convertToResponse(ride), "Ride started successfully"));
    }

    // Completes the in-progress ride and updates the driver's earnings and ride count.
    @Operation(summary = "Complete a ride", description = "Completes the in-progress ride and updates the driver's earnings and ride count")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Ride completed successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RideResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Ride is not in InProgress status",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Ride not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<RideResponse>> completeRide(
            @Parameter(description = "ID of the ride to complete", required = true) @PathVariable String id) {
        Ride ride = rideService.completeRide(id);
        return ResponseEntity.ok(ApiResponse.success(convertToResponse(ride), "Ride completed successfully"));
    }

    // Cancels the active ride and returns the driver to available status.
    @Operation(summary = "Cancel a ride", description = "Cancels the active ride and returns the driver to available status")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Ride cancelled successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RideResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Ride is already completed or cancelled",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Ride not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<RideResponse>> cancelRide(
            @Parameter(description = "ID of the ride to cancel", required = true) @PathVariable String id) {
        Ride ride = rideService.cancelRide(id);
        return ResponseEntity.ok(ApiResponse.success(convertToResponse(ride), "Ride cancelled successfully"));
    }

    // Returns the ride matching the given ride ID.
    @Operation(summary = "Get ride by ID", description = "Returns the ride matching the given ride ID")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Ride retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RideResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Ride not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RideResponse>> getRideById(
            @Parameter(description = "ID of the ride to retrieve", required = true) @PathVariable String id) {
        Ride ride = rideService.getRideById(id);
        return ResponseEntity.ok(ApiResponse.success(convertToResponse(ride), "Ride retrieved successfully"));
    }

    // Returns all rides assigned to or completed by the driver with the given ID.
    @Operation(summary = "Get rides by driver", description = "Returns all rides assigned to or completed by the driver with the given ID")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Driver rides retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RideResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Driver not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/driver/{driverId}")
    public ResponseEntity<ApiResponse<List<RideResponse>>> getRidesByDriver(
            @Parameter(description = "ID of the driver whose rides to retrieve", required = true) @PathVariable String driverId) {
        List<RideResponse> responses = rideService.getRidesByDriver(driverId)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(responses, "Driver rides retrieved successfully"));
    }

    // Returns all rides taken by the passenger with the given ID.
    @Operation(summary = "Get rides by passenger", description = "Returns all rides taken by the passenger with the given ID")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Passenger rides retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RideResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Passenger not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/passenger/{passengerId}")
    public ResponseEntity<ApiResponse<List<RideResponse>>> getRidesByPassenger(
            @Parameter(description = "ID of the passenger whose rides to retrieve", required = true) @PathVariable String passengerId) {
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

package com.ridesharing.project.controller;

import com.ridesharing.project.dto.request.CreateRideRequestRequest;
import com.ridesharing.project.dto.response.ApiResponse;
import com.ridesharing.project.dto.response.ErrorResponse;
import com.ridesharing.project.dto.response.RideRequestResponse;
import com.ridesharing.project.entity.RideRequest;
import com.ridesharing.project.service.RideMatchingService;
import com.ridesharing.project.service.RideRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

// This controller exposes REST endpoints for creating and managing ride requests, including fare estimation and cancellation.
@Tag(name = "Ride Requests", description = "Endpoints for creating, retrieving, and managing passenger ride requests")
@RestController
@RequestMapping("/rides/request")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class RideRequestController {

    
        private final RideRequestService rideRequestService;

        // Creates a new ride request with automatic fare, distance, and duration estimation from coordinates.
        @Operation(
                summary = "Create a ride request",
                description = "Creates a new ride request with automatic fare, distance, and duration estimation derived from the provided pickup and dropoff coordinates."
        )
        @ApiResponses({
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "201",
                        description = "Ride request created successfully",
                        content = @Content(mediaType = "application/json", schema = @Schema(implementation = RideRequestResponse.class))
                ),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "Validation failed — one or more request fields are invalid",
                        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
                ),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "Passenger not found",
                        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
                ),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "422",
                        description = "Passenger account is not active",
                        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
                )
        })
        @PostMapping
        public ResponseEntity<ApiResponse<RideRequestResponse>> createRideRequest(
                @Valid @RequestBody CreateRideRequestRequest request) {
                RideRequest rideRequest = rideRequestService.createRideRequest(request);
                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success(convertToResponse(rideRequest), "Ride request created successfully"));
        }

        // Returns the ride request matching the given request ID.
        @Operation(
                summary = "Get ride request by ID",
                description = "Returns the ride request matching the given request ID."
        )
        @ApiResponses({
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Ride request retrieved successfully",
                        content = @Content(mediaType = "application/json", schema = @Schema(implementation = RideRequestResponse.class))
                ),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "Ride request not found",
                        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
                )
        })
        @GetMapping("/{id}")
        public ResponseEntity<ApiResponse<RideRequestResponse>> getRideRequestById(
                @Parameter(description = "Unique identifier of the ride request", required = true)
                @PathVariable String id) {
                RideRequest rideRequest = rideRequestService.getRideRequestById(id);
                return ResponseEntity.ok(ApiResponse.success(convertToResponse(rideRequest), "Ride request retrieved successfully"));
        }

        // Returns all ride requests currently in Open status and available for driver assignment.
        @Operation(
                summary = "Get all open ride requests",
                description = "Returns all ride requests currently in Open status that are available for driver assignment."
        )
        @ApiResponses({
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Open ride requests retrieved successfully",
                        content = @Content(mediaType = "application/json", schema = @Schema(implementation = RideRequestResponse.class))
                )
        })
        @GetMapping("/open")
        public ResponseEntity<ApiResponse<List<RideRequestResponse>>> getAllOpenRequests() {
                List<RideRequestResponse> responses = rideRequestService.getAllOpenRequests()
                        .stream()
                        .map(this::convertToResponse)
                        .collect(Collectors.toList());
                return ResponseEntity.ok(ApiResponse.success(responses, "Open ride requests retrieved successfully"));
        }

        // Returns all ride requests submitted by the passenger with the given ID.
        @Operation(
                summary = "Get ride requests by passenger",
                description = "Returns all ride requests submitted by the passenger identified by the given passenger ID."
        )
        @ApiResponses({
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Passenger ride requests retrieved successfully",
                        content = @Content(mediaType = "application/json", schema = @Schema(implementation = RideRequestResponse.class))
                ),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "Passenger not found",
                        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
                )
        })
        @GetMapping("/passenger/{passengerId}")
        public ResponseEntity<ApiResponse<List<RideRequestResponse>>> getRequestsByPassenger(
                @Parameter(description = "Unique identifier of the passenger", required = true)
                @PathVariable String passengerId) {
                List<RideRequestResponse> responses = rideRequestService.getRequestsByPassenger(passengerId)
                        .stream()
                        .map(this::convertToResponse)
                        .collect(Collectors.toList());
                return ResponseEntity.ok(ApiResponse.success(responses, "Passenger ride requests retrieved successfully"));
        }

        // Cancels the open ride request with the given ID.
        @Operation(
                summary = "Cancel a ride request",
                description = "Cancels the ride request with the given ID. Only requests in Open status can be cancelled."
        )
        @ApiResponses({
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Ride request cancelled successfully",
                        content = @Content(mediaType = "application/json", schema = @Schema(implementation = RideRequestResponse.class))
                ),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "Ride request cannot be cancelled because it is already matched, completed, or cancelled",
                        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
                ),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "Ride request not found",
                        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
                )
        })
        @PutMapping("/{id}/cancel")
        public ResponseEntity<ApiResponse<RideRequestResponse>> cancelRideRequest(
                @Parameter(description = "Unique identifier of the ride request to cancel", required = true)
                @PathVariable String id) {
                RideRequest rideRequest = rideRequestService.cancelRideRequest(id);
                return ResponseEntity.ok(ApiResponse.success(convertToResponse(rideRequest), "Ride request cancelled successfully"));
        }

        // Converts a RideRequest entity to a RideRequestResponse DTO for inclusion in the API response.
        private RideRequestResponse convertToResponse(RideRequest rideRequest) {
                return RideRequestResponse.builder()
                        .id(rideRequest.getId())
                        .passengerId(rideRequest.getPassenger().getId())
                        .passengerName(rideRequest.getPassenger().getUser().getName())
                        .pickupLatitude(rideRequest.getPickupLatitude())
                        .pickupLongitude(rideRequest.getPickupLongitude())
                        .pickupAddress(rideRequest.getPickupAddress())
                        .dropoffLatitude(rideRequest.getDropoffLatitude())
                        .dropoffLongitude(rideRequest.getDropoffLongitude())
                        .dropoffAddress(rideRequest.getDropoffAddress())
                        .rideType(rideRequest.getRideType())
                        .status(rideRequest.getStatus())
                        .estimatedFare(rideRequest.getEstimatedFare())
                        .estimatedDistance(rideRequest.getEstimatedDistance())
                        .estimatedDuration(rideRequest.getEstimatedDuration())
                        .createdDate(rideRequest.getCreatedDate())
                        .build();
        }
}

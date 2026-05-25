package com.ridesharing.project.controller;

import com.ridesharing.project.dto.request.CreatePassengerRequest;
import com.ridesharing.project.dto.request.UpdatePassengerRequest;
import com.ridesharing.project.dto.response.ApiResponse;
import com.ridesharing.project.dto.response.ErrorResponse;
import com.ridesharing.project.dto.response.PassengerResponse;
import com.ridesharing.project.entity.Passenger;
import com.ridesharing.project.service.PassengerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// This controller exposes REST endpoints for creating, retrieving, and updating passenger profiles and wallet information.
@RestController
@RequestMapping("/passengers")
@CrossOrigin(origins = "*")
@Tag(name = "Passenger", description = "APIs for managing passenger profiles and wallet information")
public class PassengerController {

    @Autowired
    private PassengerService passengerService;

    // Creates a new passenger profile for the user specified in the request body.
    @Operation(summary = "Create a passenger profile", description = "Creates a new passenger profile for the user specified in the request body.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Passenger profile created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PassengerResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error in request body",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "User already has a passenger profile",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<ApiResponse<PassengerResponse>> createPassenger(
            @Valid @RequestBody CreatePassengerRequest request) {
        Passenger passenger = passengerService.createPassenger(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(convertToResponse(passenger), "Passenger profile created successfully"));
    }

    // Returns the passenger profile matching the given passenger ID.
    @Operation(summary = "Get passenger by ID", description = "Returns the passenger profile matching the given passenger ID.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Passenger retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PassengerResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Passenger not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PassengerResponse>> getPassengerById(
            @Parameter(description = "Unique identifier of the passenger", required = true) @PathVariable String id) {
        Passenger passenger = passengerService.getPassengerById(id);
        return ResponseEntity.ok(ApiResponse.success(convertToResponse(passenger), "Passenger retrieved successfully"));
    }

    // Returns the passenger profile associated with the given user account ID.
    @Operation(summary = "Get passenger by user ID", description = "Returns the passenger profile associated with the given user account ID.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Passenger retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PassengerResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Passenger not found for the given user ID",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<PassengerResponse>> getPassengerByUserId(
            @Parameter(description = "Unique identifier of the user account", required = true) @PathVariable String userId) {
        Passenger passenger = passengerService.getPassengerByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(convertToResponse(passenger), "Passenger retrieved successfully"));
    }

    // Updates the status and wallet balance of the passenger identified by the given ID.
    @Operation(summary = "Update passenger profile", description = "Updates the status and wallet balance of the passenger identified by the given ID.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Passenger profile updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PassengerResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error in request body",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Passenger not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PassengerResponse>> updatePassenger(
            @Parameter(description = "Unique identifier of the passenger", required = true) @PathVariable String id,
            @Valid @RequestBody UpdatePassengerRequest request) {
        Passenger passenger = passengerService.updatePassenger(id, request);
        return ResponseEntity.ok(ApiResponse.success(convertToResponse(passenger), "Passenger profile updated successfully"));
    }

    // Returns the current wallet balance of the passenger identified by the given ID.
    @Operation(summary = "Get wallet balance", description = "Returns the current wallet balance of the passenger identified by the given ID.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Wallet balance retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Double.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Passenger not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}/wallet")
    public ResponseEntity<ApiResponse<Double>> getWalletBalance(
            @Parameter(description = "Unique identifier of the passenger", required = true) @PathVariable String id) {
        Double balance = passengerService.getWalletBalance(id);
        return ResponseEntity.ok(ApiResponse.success(balance, "Wallet balance retrieved successfully"));
    }

    // Converts a Passenger entity to a PassengerResponse DTO for inclusion in the API response.
    private PassengerResponse convertToResponse(Passenger passenger) {
        return PassengerResponse.builder()
                .id(passenger.getId())
                .userId(passenger.getUser().getId())
                .userName(passenger.getUser().getName())
                .userEmail(passenger.getUser().getEmail())
                .userPhone(passenger.getUser().getPhone())
                .walletBalance(passenger.getWalletBalance())
                .status(passenger.getStatus())
                .joinDate(passenger.getJoinDate())
                .build();
    }
}

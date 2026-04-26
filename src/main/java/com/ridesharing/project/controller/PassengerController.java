package com.ridesharing.project.controller;

import com.ridesharing.project.dto.request.CreatePassengerRequest;
import com.ridesharing.project.dto.request.UpdatePassengerRequest;
import com.ridesharing.project.dto.response.ApiResponse;
import com.ridesharing.project.dto.response.PassengerResponse;
import com.ridesharing.project.entity.Passenger;
import com.ridesharing.project.service.PassengerService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// This controller exposes REST endpoints for creating, retrieving, and updating passenger profiles and wallet information.
@RestController
@RequestMapping("/passengers")
@CrossOrigin(origins = "*")
public class PassengerController {

    @Autowired
    private PassengerService passengerService;

    // Creates a new passenger profile for the user specified in the request body.
    @PostMapping
    public ResponseEntity<ApiResponse<PassengerResponse>> createPassenger(
            @Valid @RequestBody CreatePassengerRequest request) {
        Passenger passenger = passengerService.createPassenger(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(convertToResponse(passenger), "Passenger profile created successfully"));
    }

    // Returns the passenger profile matching the given passenger ID.
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PassengerResponse>> getPassengerById(@PathVariable String id) {
        Passenger passenger = passengerService.getPassengerById(id);
        return ResponseEntity.ok(ApiResponse.success(convertToResponse(passenger), "Passenger retrieved successfully"));
    }

    // Returns the passenger profile associated with the given user account ID.
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<PassengerResponse>> getPassengerByUserId(@PathVariable String userId) {
        Passenger passenger = passengerService.getPassengerByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(convertToResponse(passenger), "Passenger retrieved successfully"));
    }

    // Updates the status and wallet balance of the passenger identified by the given ID.
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PassengerResponse>> updatePassenger(
            @PathVariable String id,
            @Valid @RequestBody UpdatePassengerRequest request) {
        Passenger passenger = passengerService.updatePassenger(id, request);
        return ResponseEntity.ok(ApiResponse.success(convertToResponse(passenger), "Passenger profile updated successfully"));
    }

    // Returns the current wallet balance of the passenger identified by the given ID.
    @GetMapping("/{id}/wallet")
    public ResponseEntity<ApiResponse<Double>> getWalletBalance(@PathVariable String id) {
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

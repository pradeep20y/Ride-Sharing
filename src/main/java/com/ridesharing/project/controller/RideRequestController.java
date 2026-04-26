package com.ridesharing.project.controller;

import com.ridesharing.project.dto.request.CreateRideRequestRequest;
import com.ridesharing.project.dto.response.ApiResponse;
import com.ridesharing.project.dto.response.RideRequestResponse;
import com.ridesharing.project.entity.RideRequest;
import com.ridesharing.project.service.RideRequestService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

// This controller exposes REST endpoints for creating and managing ride requests, including fare estimation and cancellation.
@RestController
@RequestMapping("/rides/request")
@CrossOrigin(origins = "*")
public class RideRequestController {

    @Autowired
    private RideRequestService rideRequestService;

    // Creates a new ride request with automatic fare, distance, and duration estimation from coordinates.
    @PostMapping
    public ResponseEntity<ApiResponse<RideRequestResponse>> createRideRequest(
            @Valid @RequestBody CreateRideRequestRequest request) {
        RideRequest rideRequest = rideRequestService.createRideRequest(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(convertToResponse(rideRequest), "Ride request created successfully"));
    }

    // Returns the ride request matching the given request ID.
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RideRequestResponse>> getRideRequestById(@PathVariable String id) {
        RideRequest rideRequest = rideRequestService.getRideRequestById(id);
        return ResponseEntity.ok(ApiResponse.success(convertToResponse(rideRequest), "Ride request retrieved successfully"));
    }

    // Returns all ride requests currently in Open status and available for driver assignment.
    @GetMapping("/open")
    public ResponseEntity<ApiResponse<List<RideRequestResponse>>> getAllOpenRequests() {
        List<RideRequestResponse> responses = rideRequestService.getAllOpenRequests()
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(responses, "Open ride requests retrieved successfully"));
    }

    // Returns all ride requests submitted by the passenger with the given ID.
    @GetMapping("/passenger/{passengerId}")
    public ResponseEntity<ApiResponse<List<RideRequestResponse>>> getRequestsByPassenger(
            @PathVariable String passengerId) {
        List<RideRequestResponse> responses = rideRequestService.getRequestsByPassenger(passengerId)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(responses, "Passenger ride requests retrieved successfully"));
    }

    // Cancels the open ride request with the given ID.
    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<RideRequestResponse>> cancelRideRequest(@PathVariable String id) {
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

package com.ridesharing.project.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Sent by a driver via WebSocket to explicitly reject a ride offer they received.
// Both fields are required — the server needs the requestId to locate the ride request
// and the driverId to verify the rejecting driver actually holds the active offer
// (preventing a different driver from rejecting someone else's offer).
// Processed by WebSocketController.rejectRide() which delegates to RideMatchingService.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RideRejectRequest {

    // Identifies which ride request is being rejected — used to load the RideRequest from MySQL
    @NotBlank(message = "Request ID is required")
    private String requestId;

    // Identifies which driver is rejecting — must match the offeredToDriverId on the RideRequest
    @NotBlank(message = "Driver ID is required")
    private String driverId;
}

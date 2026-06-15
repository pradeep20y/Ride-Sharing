package com.ridesharing.project.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Sent by the driver via WebSocket to accept a ride offer
// Server uses requestId to find the ride and driverId to assign the driver
@Data
@NoArgsConstructor
@AllArgsConstructor
public class 
RideAcceptRequest {

    @NotBlank(message = "Request ID is required")
    private String requestId;

    @NotBlank(message = "Driver ID is required")
    private String driverId;
}

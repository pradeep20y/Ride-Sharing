package com.ridesharing.project.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// This DTO represents an active or completed ride with passenger, driver, and fare details returned in API responses.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RideResponse {

    private String id;
    private String rideRequestId;
    private String passengerId;
    private String passengerName;
    private String driverId;
    private String driverName;
    private String driverLicensePlate;
    private String status;
    private Double fare;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime assignedDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completedDate;
}

package com.ridesharing.project.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// This DTO represents a ride request with all location details, estimated fare, and current status returned in API responses.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RideRequestResponse {

    private String id;
    private String passengerId;
    private String passengerName;
    private Double pickupLatitude;
    private Double pickupLongitude;
    private String pickupAddress;
    private Double dropoffLatitude;
    private Double dropoffLongitude;
    private String dropoffAddress;
    private String rideType;
    private String status;
    private Double estimatedFare;
    private Double estimatedDistance;
    private Integer estimatedDuration;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdDate;
}

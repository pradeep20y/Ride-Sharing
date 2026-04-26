package com.ridesharing.project.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverDTO {

    private String id;
    private String userId;
    private String userName;
    private String userEmail;
    private String licensePlate;
    private String vehicleType;
    private String status;
    private Double currentLatitude;
    private Double currentLongitude;
    private LocalDateTime lastLocationUpdate;
    private Double rating;
    private Double totalEarnings;
    private Integer totalRides;
    private Double acceptanceRate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

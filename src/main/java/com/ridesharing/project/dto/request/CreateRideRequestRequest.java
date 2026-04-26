package com.ridesharing.project.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// This DTO carries all location and preference details needed to create a new ride request with automatic fare estimation.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRideRequestRequest {

    @NotBlank(message = "Passenger ID is required")
    private String passengerId;

    @NotNull(message = "Pickup latitude is required")
    @DecimalMin(value = "-90.0", inclusive = true, message = "Pickup latitude must be between -90.0 and 90.0")
    @DecimalMax(value = "90.0", inclusive = true, message = "Pickup latitude must be between -90.0 and 90.0")
    private Double pickupLatitude;

    @NotNull(message = "Pickup longitude is required")
    @DecimalMin(value = "-180.0", inclusive = true, message = "Pickup longitude must be between -180.0 and 180.0")
    @DecimalMax(value = "180.0", inclusive = true, message = "Pickup longitude must be between -180.0 and 180.0")
    private Double pickupLongitude;

    @NotBlank(message = "Pickup address is required")
    private String pickupAddress;

    @NotNull(message = "Dropoff latitude is required")
    @DecimalMin(value = "-90.0", inclusive = true, message = "Dropoff latitude must be between -90.0 and 90.0")
    @DecimalMax(value = "90.0", inclusive = true, message = "Dropoff latitude must be between -90.0 and 90.0")
    private Double dropoffLatitude;

    @NotNull(message = "Dropoff longitude is required")
    @DecimalMin(value = "-180.0", inclusive = true, message = "Dropoff longitude must be between -180.0 and 180.0")
    @DecimalMax(value = "180.0", inclusive = true, message = "Dropoff longitude must be between -180.0 and 180.0")
    private Double dropoffLongitude;

    @NotBlank(message = "Dropoff address is required")
    private String dropoffAddress;

    @NotBlank(message = "Ride type is required")
    @Pattern(regexp = "^(ECONOMY|COMFORT|PREMIUM)$", message = "Ride type must be one of: ECONOMY, COMFORT, PREMIUM")
    private String rideType;
}

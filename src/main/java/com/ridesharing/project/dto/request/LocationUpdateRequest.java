package com.ridesharing.project.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationUpdateRequest {

    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0",  inclusive = true, message = "Latitude must be between -90.0 and 90.0")
    @DecimalMax(value = "90.0",   inclusive = true, message = "Latitude must be between -90.0 and 90.0")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", inclusive = true, message = "Longitude must be between -180.0 and 180.0")
    @DecimalMax(value = "180.0",  inclusive = true, message = "Longitude must be between -180.0 and 180.0")
    private Double longitude;
}

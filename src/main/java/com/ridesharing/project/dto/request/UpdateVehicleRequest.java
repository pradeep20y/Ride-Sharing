package com.ridesharing.project.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating a driver's vehicle information.
 *
 * <p>Drivers may legally switch vehicles (e.g., renting a premium car for a shift).
 * When the license plate changes, uniqueness is re-validated at the service layer.
 * The driver's status does not change as a result of a vehicle update.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for updating driver vehicle details")
public class UpdateVehicleRequest {

    /**
     * New license plate for the vehicle.
     * If unchanged, the service skips the uniqueness check to avoid a false conflict.
     */
    @NotBlank(message = "License plate is required")
    @Size(min = 3, max = 20, message = "License plate must be between 3 and 20 characters")
    @Pattern(
        regexp = "^[A-Z0-9]+$",
        message = "License plate must contain only uppercase letters and numbers (e.g., XYZ9876)"
    )
    @Schema(
        description = "New vehicle license plate — uppercase letters and numbers only",
        example = "XYZ9876",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String licensePlate;

    /**
     * New vehicle category.
     * Changing this affects which ride requests the driver appears in.
     */
    @NotBlank(message = "Vehicle type is required")
    @Pattern(
        regexp = "^(ECONOMY|COMFORT|PREMIUM)$",
        message = "Vehicle type must be one of: ECONOMY, COMFORT, PREMIUM"
    )
    @Schema(
        description = "New vehicle category",
        example = "COMFORT",
        allowableValues = {"ECONOMY", "COMFORT", "PREMIUM"},
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String vehicleType;
}

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
 * Request DTO for registering a new driver profile.
 *
 * <p>A driver profile is always linked to an existing User account.
 * The user must already exist before they can register as a driver.
 * License plate uniqueness is enforced at the service layer in addition
 * to the database unique constraint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for registering a new driver")
public class RegisterDriverRequest {

    /**
     * ID of the existing User account that will become a driver.
     * The user must already be registered in the system.
     */
    @NotBlank(message = "User ID is required")
    @Schema(
        description = "ID of the existing user account to associate with this driver profile",
        example = "550e8400-e29b-41d4-a716-446655440000",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String userId;

    /**
     * Vehicle license plate — must be unique across all drivers.
     * Only uppercase letters and digits are accepted (e.g., "ABC1234").
     */
    @NotBlank(message = "License plate is required")
    @Size(min = 3, max = 20, message = "License plate must be between 3 and 20 characters")
    @Pattern(
        regexp = "^[A-Z0-9]+$",
        message = "License plate must contain only uppercase letters and numbers (e.g., ABC1234)"
    )
    @Schema(
        description = "Unique vehicle license plate — uppercase letters and numbers only",
        example = "ABC1234",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String licensePlate;

    /**
     * Category of vehicle the driver operates.
     * Determines which ride requests this driver is eligible for.
     */
    @NotBlank(message = "Vehicle type is required")
    @Pattern(
        regexp = "^(ECONOMY|COMFORT|PREMIUM)$",
        message = "Vehicle type must be one of: ECONOMY, COMFORT, PREMIUM"
    )
    @Schema(
        description = "Category of vehicle",
        example = "ECONOMY",
        allowableValues = {"ECONOMY", "COMFORT", "PREMIUM"},
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String vehicleType;
}

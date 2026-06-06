package com.ridesharing.project.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for one-shot driver registration.
 *
 * <p>Creates both a User account (userType=DRIVER) and a linked Driver
 * profile in a single atomic transaction. The frontend sends this after
 * the user fills in personal details + vehicle details and clicks Register.
 *
 * <p>Business rules enforced downstream:
 * <ul>
 *   <li>Email must be globally unique across all users.</li>
 *   <li>License plate must be globally unique across all drivers.</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for one-shot driver registration")
public class RegisterRequest {

    // ── User fields ───────────────────────────────────────────────────────────

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    @Schema(description = "Full name of the driver", example = "Prad Kumar",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    @Schema(description = "Email address — used as login credential", example = "prad@example.com",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be exactly 10 digits")
    @Schema(description = "10-digit mobile number", example = "9876543210",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String phone;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Schema(description = "Plain-text password — hashed before storage", example = "Secret@123",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    // ── Driver / Vehicle fields ───────────────────────────────────────────────

    @Size(min = 3, max = 20, message = "License plate must be between 3 and 20 characters")
    @Pattern(regexp = "^[A-Z0-9]+$",
             message = "License plate must contain only uppercase letters and numbers (e.g., TN01AB1234)")
    @Schema(description = "Unique vehicle license plate — uppercase letters and numbers only",
            example = "TN01AB1234", requiredMode = Schema.RequiredMode.REQUIRED)
    private String licensePlate;

        @Pattern(regexp = "^(ECONOMY|COMFORT|PREMIUM)$",
             message = "Vehicle type must be one of: ECONOMY, COMFORT, PREMIUM")
    @Schema(description = "Category of vehicle",
            example = "ECONOMY",
            allowableValues = {"ECONOMY", "COMFORT", "PREMIUM"},
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String vehicleType;
}

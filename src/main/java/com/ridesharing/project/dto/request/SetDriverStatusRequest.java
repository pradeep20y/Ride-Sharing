package com.ridesharing.project.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for changing a driver's availability status.
 *
 * <p>Valid transitions:
 * <ul>
 *   <li>OFFLINE → ONLINE  (driver comes on duty)</li>
 *   <li>ONLINE  → OFFLINE (driver goes off duty)</li>
 *   <li>ONLINE  → ON_TRIP (driver accepts a ride — typically set by the system)</li>
 *   <li>ON_TRIP → ONLINE  (driver completes a ride)</li>
 * </ul>
 *
 * TODO: Phase 3 — Enforce valid state transitions and reject invalid ones
 *       (e.g., OFFLINE → ON_TRIP should not be allowed).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for updating driver availability status")
public class SetDriverStatusRequest {

    /**
     * Target status the driver should move to.
     * Must be one of the three recognised values.
     */
    @NotBlank(message = "Status is required")
    @Pattern(
        regexp = "^(OFFLINE|ONLINE|ON_TRIP)$",
        message = "Status must be one of: OFFLINE, ONLINE, ON_TRIP"
    )
    @Schema(
        description = "Target availability status for the driver",
        example = "ONLINE",
        allowableValues = {"OFFLINE", "ONLINE", "ON_TRIP"},
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String status;
}

package com.ridesharing.project.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating a driver's average rating.
 *
 * <p>In Phase 1 this endpoint accepts an absolute rating value directly.
 * In later phases this will be replaced by a calculated weighted average
 * derived from individual trip ratings stored in the Ratings service.
 *
 * TODO: Phase 7 — Replace direct rating override with weighted rolling average
 *       computed from the Ratings & Reputation service.  The caller should
 *       submit a single trip rating; the service recalculates the driver average.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for updating a driver's average rating")
public class UpdateDriverRatingRequest {

    /**
     * New rating value on a 0.0–5.0 scale.
     * Decimal precision up to one place is typical (e.g., 4.7).
     */
    @NotNull(message = "Rating is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Rating must be between 0.0 and 5.0")
    @DecimalMax(value = "5.0", inclusive = true, message = "Rating must be between 0.0 and 5.0")
    @Schema(
        description = "Driver rating on a 0.0 to 5.0 scale",
        example = "4.7",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Double rating;
}

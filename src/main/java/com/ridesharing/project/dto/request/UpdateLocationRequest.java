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
 * Request DTO for updating a driver's current GPS location.
 *
 * <p>Location updates are expected to be called frequently from the driver's
 * mobile device while the driver is ONLINE or ON_TRIP.  The service layer
 * records the timestamp of each update automatically.
 *
 * TODO: Phase 2 — Replace REST polling with WebSocket push for real-time
 *       location streaming to reduce HTTP overhead and latency.
 * TODO: Phase 4 — Feed location updates into a geospatial index (e.g., Redis
 *       GEO commands) to enable efficient nearby-driver queries.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for updating the driver's current GPS coordinates")
public class UpdateLocationRequest {

    /**
     * WGS-84 latitude in decimal degrees.
     * Valid range: -90.0 (South Pole) to +90.0 (North Pole).
     */
    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0", inclusive = true, message = "Latitude must be between -90.0 and 90.0")
    @DecimalMax(value = "90.0",  inclusive = true, message = "Latitude must be between -90.0 and 90.0")
    @Schema(
        description = "WGS-84 latitude in decimal degrees (-90 to 90)",
        example = "37.7749",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Double latitude;

    /**
     * WGS-84 longitude in decimal degrees.
     * Valid range: -180.0 (International Date Line West) to +180.0 (East).
     */
    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", inclusive = true, message = "Longitude must be between -180.0 and 180.0")
    @DecimalMax(value = "180.0",  inclusive = true, message = "Longitude must be between -180.0 and 180.0")
    @Schema(
        description = "WGS-84 longitude in decimal degrees (-180 to 180)",
        example = "-122.4194",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Double longitude;
}

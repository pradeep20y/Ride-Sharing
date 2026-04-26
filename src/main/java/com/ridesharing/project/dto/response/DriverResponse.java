package com.ridesharing.project.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO representing a driver profile returned by the API.
 *
 * <p>This is the single canonical view of a driver for all read operations.
 * It flattens the Driver–User relationship so clients receive all relevant
 * user fields (name, email, phone) without needing a separate User lookup.
 *
 * <p>Sensitive or internal fields (e.g., raw DB ids from related tables,
 * internal flags) are intentionally excluded from this view.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Complete driver profile returned in API responses")
public class DriverResponse {

    // ── Driver identity ──────────────────────────────────────────────────────

    @Schema(description = "Unique driver profile identifier (UUID)", example = "d1e2f3a4-b5c6-7890-abcd-ef1234567890")
    private String id;

    // ── Associated user fields (flattened for convenience) ───────────────────

    @Schema(description = "ID of the linked user account", example = "550e8400-e29b-41d4-a716-446655440000")
    private String userId;

    @Schema(description = "Driver's full name", example = "John Doe")
    private String userName;

    @Schema(description = "Driver's email address", example = "john.doe@example.com")
    private String userEmail;

    @Schema(description = "Driver's contact phone number", example = "+14155552671")
    private String userPhone;

    // ── Vehicle information ───────────────────────────────────────────────────

    @Schema(description = "Vehicle license plate (uppercase letters and numbers)", example = "ABC1234")
    private String licensePlate;

    @Schema(
        description = "Vehicle category determining eligible ride types",
        example = "ECONOMY",
        allowableValues = {"ECONOMY", "COMFORT", "PREMIUM"}
    )
    private String vehicleType;

    // ── Availability ──────────────────────────────────────────────────────────

    @Schema(
        description = "Current driver availability status",
        example = "ONLINE",
        allowableValues = {"OFFLINE", "ONLINE", "ON_TRIP"}
    )
    private String status;

    // ── Location ──────────────────────────────────────────────────────────────

    @Schema(description = "Last known GPS latitude (-90 to 90)", example = "37.7749")
    private Double currentLatitude;

    @Schema(description = "Last known GPS longitude (-180 to 180)", example = "-122.4194")
    private Double currentLongitude;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "Timestamp of the most recent location update", example = "2024-01-15 10:30:00")
    private LocalDateTime lastLocationUpdate;

    // ── Performance metrics ───────────────────────────────────────────────────

    @Schema(description = "Average passenger rating (0.0–5.0)", example = "4.8")
    private Double rating;

    @Schema(description = "Total lifetime earnings in the platform currency", example = "1250.75")
    private Double totalEarnings;

    @Schema(description = "Total number of completed rides", example = "152")
    private Integer totalRides;

    @Schema(description = "Percentage of ride requests accepted (0–100)", example = "94.5")
    private Double acceptanceRate;

    // ── Audit timestamps ─────────────────────────────────────────────────────

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "Timestamp when the driver profile was created", example = "2024-01-01 09:00:00")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "Timestamp of the most recent profile update", example = "2024-01-15 10:30:00")
    private LocalDateTime updatedAt;
}

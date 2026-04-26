package com.ridesharing.project.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Standardised error response returned for all failed API requests.
 *
 * <p>Having a single error shape across the whole API means clients only need
 * one error-parsing path regardless of the failure type (404, 409, 422, 500…).
 *
 * <p>{@code fieldErrors} is only included in the JSON when it is non-null, so
 * simple 404/500 responses stay compact.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standardised error envelope returned for all failed API requests")
public class ErrorResponse {

    @Schema(description = "HTTP status code", example = "404")
    private int status;

    /**
     * Short machine-readable error category (matches the exception class name
     * or a well-known constant such as "VALIDATION_FAILED").
     */
    @Schema(description = "Short machine-readable error category", example = "DRIVER_NOT_FOUND")
    private String error;

    @Schema(description = "Human-readable description of what went wrong", example = "Driver with ID xyz was not found")
    private String message;

    @Schema(description = "Request path that triggered the error", example = "/api/drivers/xyz")
    private String path;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "Server timestamp when the error occurred", example = "2024-01-15 10:30:00")
    private LocalDateTime timestamp;

    /**
     * Present only when the error is a validation failure (HTTP 400).
     * Each entry identifies a single field that failed validation.
     */
    @Schema(description = "Per-field validation errors — present only for HTTP 400 validation failures")
    private List<FieldError> fieldErrors;

    // ── Nested types ─────────────────────────────────────────────────────────

    /**
     * Details of a single field-level validation failure.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Details of a single field validation failure")
    public static class FieldError {

        @Schema(description = "Name of the field that failed validation", example = "licensePlate")
        private String field;

        @Schema(description = "Value that was rejected (serialised as a string)", example = "abc 123")
        private String rejectedValue;

        @Schema(description = "Reason the value was rejected", example = "License plate must contain only uppercase letters and numbers")
        private String message;
    }
}

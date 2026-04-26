package com.ridesharing.project.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Generic success response wrapper used across all API endpoints.
 *
 * <p>Wrapping every successful response in this envelope gives clients a
 * consistent shape to parse: check {@code success}, read {@code message}
 * for a human summary, and extract strongly-typed {@code data}.
 *
 * <p>Usage:
 * <pre>{@code
 *   return ResponseEntity.ok(ApiResponse.success(driverResponse, "Driver registered successfully"));
 *   return ResponseEntity.ok(ApiResponse.success("Driver deleted successfully"));
 * }</pre>
 *
 * @param <T> Type of the payload carried in {@code data}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Generic success response wrapper used across all API endpoints")
public class ApiResponse<T> {

    @Schema(description = "true when the operation succeeded", example = "true")
    private boolean success;

    @Schema(description = "Human-readable summary of the operation result", example = "Driver registered successfully")
    private String message;

    /**
     * The operation's return value.  Null (and omitted from JSON) for operations
     * that return no meaningful payload (e.g., DELETE).
     */
    @Schema(description = "Response payload — omitted when the operation produces no data")
    private T data;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "Server timestamp when the response was generated", example = "2024-01-15 10:30:00")
    private LocalDateTime timestamp;

    // ── Factory methods ───────────────────────────────────────────────────────

    /**
     * Creates a successful response with a data payload.
     *
     * @param data    the response payload
     * @param message human-readable success message
     * @param <T>     payload type
     * @return wrapped success response
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Creates a successful response with no data payload (e.g., DELETE operations).
     *
     * @param message human-readable success message
     * @param <T>     phantom payload type
     * @return wrapped success response
     */
    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
}

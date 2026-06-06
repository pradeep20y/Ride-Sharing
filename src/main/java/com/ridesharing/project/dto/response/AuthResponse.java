package com.ridesharing.project.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Response payload returned after successful registration")
public class AuthResponse {

    @Schema(description = "Generated UUID of the created user account",
            example = "550e8400-e29b-41d4-a716-446655440000")
    private String userId;

    @Schema(description = "Full name", example = "Prad Kumar")
    private String name;

    @Schema(description = "Registered email address", example = "prad@example.com")
    private String email;

    @Schema(description = "Registered phone number", example = "9191929291")
    private String phone;   
    
    @Schema(description = "PASSENGER or DRIVER", example = "DRIVER")
    private String userType;

    @Schema(description = "Generated UUID of the driver profile — present only for driver registrations",
            example = "660e8400-e29b-41d4-a716-446655440001")
    private String profileId;  // null for passengers, omitted from JSON via @JsonInclude

    @Schema(description = "Human-readable result message", example = "Driver registered successfully")
    private String message;
}

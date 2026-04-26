package com.ridesharing.project.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// This DTO carries the data required to create a new passenger profile linked to an existing user account.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePassengerRequest {

    @NotBlank(message = "User ID is required to create a passenger profile")
    private String userId;
}

package com.ridesharing.project.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// This DTO carries the fields that may be updated on an existing passenger profile.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePassengerRequest {

    @NotBlank(message = "Status is required and must not be blank")
    private String status;

    @NotNull(message = "Wallet balance is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Wallet balance cannot be a negative value")
    private Double walletBalance;
}

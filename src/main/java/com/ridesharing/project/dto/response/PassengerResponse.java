package com.ridesharing.project.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// This DTO represents the complete passenger profile data returned in API responses.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PassengerResponse {

    private String id;
    private String userId;
    private String userName;
    private String userEmail;
    private String userPhone;
    private Double walletBalance;
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime joinDate;
}

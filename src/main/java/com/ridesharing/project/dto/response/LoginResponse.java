package com.ridesharing.project.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {
    private String token;
    private String userId;
    private String name;
    private String email;
    private String phone;
    private String userType;
    private String profileId;   // null if DRIVER
}

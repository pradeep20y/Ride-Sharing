package com.ridesharing.project.dto.request;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
        private String identifier; // email or phone number
        private String password;
}

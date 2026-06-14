package com.ridesharing.project.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

import com.ridesharing.project.dto.request.LoginRequest;
import com.ridesharing.project.dto.request.RegisterRequest;
import com.ridesharing.project.dto.response.ApiResponse;
import com.ridesharing.project.dto.response.AuthResponse;
import com.ridesharing.project.dto.response.LoginResponse;
import com.ridesharing.project.service.AuthService;

 
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
 
    private final AuthService authService;


    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {

        return authService.authenticate(request);
    }

    /**
     * Registers a new driver — creates a User account (userType=DRIVER)
     * and a Driver profile atomically in one request.
     *
     * @param request validated registration payload
     * @return 201 Created with userId, driverId, and basic profile
     */
    @PostMapping("/register/driver")
    public ResponseEntity<ApiResponse<AuthResponse>> registerDriver(
            @Valid @RequestBody RegisterRequest request) {

        AuthResponse response = authService.registerDriver(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Driver registered successfully"));
    }

    @PostMapping("register/passenger")
    public ResponseEntity<ApiResponse<AuthResponse>> registerPassenger(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.registerPassenger(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "passenger registered successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<UserMeResponse> getCurrentUser(Authentication authentication) {
        String phone = authentication.getName(); // Spring Security sets this from JWT subject
        UserMeResponse response = authService.getCurrentUserDetails(phone);
        return ResponseEntity.ok(response);
    }


}

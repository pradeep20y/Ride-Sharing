package com.ridesharing.project.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

import com.prad.starter.jwt.JwtUtil;
import com.ridesharing.project.dto.UserDTO;
import com.ridesharing.project.dto.request.LoginRequest;
import com.ridesharing.project.dto.request.RegisterDriverRequest;
import com.ridesharing.project.dto.response.ApiResponse;
import com.ridesharing.project.dto.response.AuthResponse;
import com.ridesharing.project.dto.response.LoginResponse;
import com.ridesharing.project.entity.User;
import com.ridesharing.project.service.DriverService;
import com.ridesharing.project.service.UserService;

 
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {
 
    private final UserService userService;
    private final DriverService driverService;


    public AuthController(  UserService userService, DriverService driverService) {
       
      
        this.userService = userService;
        this.driverService = driverService;
    }

    @PostMapping("/register")
    public User register(@RequestBody UserDTO request) {
        return userService.createUser(request);
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {

        return userService.authenticate(request);
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
            @Valid @RequestBody RegisterDriverRequest request) {

        AuthResponse response = driverService.registerDriver(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Driver registered successfully"));
    }


}

package com.ridesharing.project.controller;

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
import com.ridesharing.project.dto.response.LoginResponse;
import com.ridesharing.project.entity.User;
import com.ridesharing.project.service.UserService;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserService userService;


    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil, UserService userService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }

    @PostMapping("/register")
    public User register(@RequestBody UserDTO request) {
        return userService.createUser(request);
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {

        return userService.authenticate(request);
    }
}

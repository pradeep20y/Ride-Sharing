package com.ridesharing.project.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.prad.starter.jwt.JwtUtil;
import com.ridesharing.project.dto.request.LoginRequest;
import com.ridesharing.project.dto.request.RegisterRequest;
import com.ridesharing.project.dto.response.AuthResponse;
import com.ridesharing.project.dto.response.LoginResponse;
import com.ridesharing.project.entity.Passenger;
import com.ridesharing.project.entity.User;
import com.ridesharing.project.exception.BusinessException;
import com.ridesharing.project.entity.Driver;
import com.ridesharing.project.repository.DriverRepository;
import com.ridesharing.project.repository.PassengerRepository;
import com.ridesharing.project.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(DriverService.class);


// ✅ Fix — add final to every field
private final AuthenticationManager authenticationManager;
private final UserRepository userRepository;
private final PasswordEncoder passwordEncoder;
private final JwtUtil jwtUtil;
private final PassengerRepository passengerRepository;
private final DriverRepository driverRepository;

    public LoginResponse authenticate(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(
                        loginRequest.getIdentifier(),
                        loginRequest.getPassword()));

        User user = userRepository.findByPhone(loginRequest.getIdentifier())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String jwtToken = jwtUtil.generateTokenFromUsername(userDetails);

        // ── Resolve role-specific profile ID ─────────────────────────────────
        String profileId = null;
    
        if ("PASSENGER".equals(user.getUserType())) {
            profileId = passengerRepository.findByUser(user)
                    .map(Passenger::getId)
                    .orElse(null);
        } else if ("DRIVER".equals(user.getUserType())) {
            profileId = driverRepository.findByUser(user)
                    .map(Driver::getId)
                    .orElse(null);
        }

        return LoginResponse.builder()
                .token(jwtToken)
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .userType(user.getUserType())
                .profileId(profileId)   // null if DRIVER
                .build();
    }   

    @Transactional
    public AuthResponse registerDriver(RegisterRequest request) {
        log.info("Driver registration attempt: email={}, licensePlate={}",
                request.getEmail(), request.getLicensePlate());

        // ── Duplicate checks (service layer, before hitting DB constraints) ───
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(
                "Email '" + request.getEmail() + "' is already registered. "
            + "Please use a different email or log in.");
        }

        if (driverRepository.existsByLicensePlate(request.getLicensePlate())) {
            throw new BusinessException(
                "License plate '" + request.getLicensePlate() + "' is already "
            + "registered to another driver.");
        }

        // ── Step 1: Create the User account ──────────────────────────────────
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword())); // never store plain-text
        user.setUserType("DRIVER");
        user.setRating(5.0);

        User savedUser = userRepository.save(user);
        log.info("User account created: userId={}", savedUser.getId());

        // ── Step 2: Create the Driver profile ────────────────────────────────
        Driver driver = new Driver();
        driver.setUser(savedUser);
        driver.setLicensePlate(request.getLicensePlate());
        driver.setVehicleType(request.getVehicleType());
        driver.setStatus("OFFLINE");      // all new drivers start offline
        driver.setRating(5.0);
        driver.setTotalEarnings(0.0);
        driver.setTotalRides(0);
        driver.setAcceptanceRate(100.0);

        Driver savedDriver = driverRepository.save(driver);
        log.info("Driver profile created: driverId={}", savedDriver.getId());

        // ── Step 3: Build and return response ────────────────────────────────
        return AuthResponse.builder()
                .userId(savedUser.getId())
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .phone(savedUser.getPhone())
                .userType(savedUser.getUserType())
                .profileId(savedDriver.getId())
                .message("Driver registered successfully")
                .build();
    }

    @Transactional
    public AuthResponse registerPassenger(RegisterRequest request) {
        log.info("Passenger registration attempt: email={}", request.getEmail());

        // ── Duplicate checks ──────────────────────────────────────────────────
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(
                "Email '" + request.getEmail() + "' is already registered. "
            + "Please use a different email or log in.");
        }

        // ── Step 1: Create the User account ──────────────────────────────────
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setUserType("PASSENGER");
        user.setRating(5.0);

        User savedUser = userRepository.save(user);
        log.info("User account created: userId={}", savedUser.getId());

        // ── Step 2: Create the Passenger profile ─────────────────────────────
        Passenger passenger = new Passenger();
        passenger.setUser(savedUser);
        passenger.setWalletBalance(0.0);
        passenger.setStatus("Active");

        Passenger savedPassenger = passengerRepository.save(passenger);
        log.info("Passenger profile created: passengerId={}", savedPassenger.getId());

        // ── Step 3: Build and return response ────────────────────────────────
        return AuthResponse.builder()
                .userId(savedUser.getId())
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .phone(savedUser.getPhone())
                .userType(savedUser.getUserType())
                .profileId(savedPassenger.getId())
                .message("Passenger registered successfully")
                .build();
    }

}

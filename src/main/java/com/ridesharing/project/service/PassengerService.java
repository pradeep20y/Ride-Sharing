package com.ridesharing.project.service;

import com.ridesharing.project.dto.request.CreatePassengerRequest;
import com.ridesharing.project.dto.request.RegisterRequest;
import com.ridesharing.project.dto.request.UpdatePassengerRequest;
import com.ridesharing.project.dto.response.AuthResponse;
import com.ridesharing.project.entity.Passenger;
import com.ridesharing.project.entity.User;
import com.ridesharing.project.exception.BusinessException;
import com.ridesharing.project.exception.ResourceNotFoundException;
import com.ridesharing.project.repository.PassengerRepository;
import com.ridesharing.project.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
 import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// This service handles all business logic for passenger profiles including creation, retrieval, and profile updates.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PassengerService {

    private static final Logger log = LoggerFactory.getLogger(DriverService.class);


    private final PassengerRepository passengerRepository;
    private final UserRepository userRepository;
 

    // Constructor injection ensures both repositories are always available and promotes testability.
  

  
    // Retrieves a passenger profile by its unique ID, throwing a not-found error if no match exists.
    public Passenger getPassengerById(String id) {
        return passengerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Passenger", "id", id));
    }

    // Retrieves a passenger profile by the ID of the linked user account.
    public Passenger getPassengerByUserId(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        return passengerRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Passenger", "userId", userId));
    }

    // Updates the status and wallet balance of an existing passenger profile with the provided values.
    @Transactional
    public Passenger updatePassenger(String id, UpdatePassengerRequest request) {
        Passenger passenger = passengerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Passenger", "id", id));

        passenger.setStatus(request.getStatus());
        passenger.setWalletBalance(request.getWalletBalance());

        return passengerRepository.save(passenger);
    }

    // Returns the current wallet balance for a passenger without loading the full profile context.
    public Double getWalletBalance(String id) {
        Passenger passenger = passengerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Passenger", "id", id));

        return passenger.getWalletBalance();
    }
}

package com.ridesharing.project.service;

import com.ridesharing.project.dto.UserDTO;
import com.ridesharing.project.entity.User;
import com.ridesharing.project.repository.UserRepository;

import lombok.RequiredArgsConstructor;


import org.springframework.security.crypto.password.PasswordEncoder;



import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

 

    // Create new user
    public User createUser(UserDTO request) {
        if (userRepository.existsByEmail(request.getEmail()) && userRepository.existsByPhone(request.getPhone())) {
            throw new RuntimeException("User already exists");
        }
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setName(request.getName());
        user.setUserType(request.getUserType());
        user.setPassword(
        passwordEncoder.encode(request.getPassword())
        );
        return userRepository.save(user);
    }



    // Get user by ID
    public Optional<User> getUserById(String id) {
        return userRepository.findById(id);
    }

    // Get user by email
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> getUserByPhone(String phoneNumber) {
        return userRepository.findByPhone(phoneNumber);
    }
    
    // Get all users
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Get all passengers
    public List<User> getAllPassengers() {
        return userRepository.findByUserType("PASSENGER");
    }

    // Get all drivers
    public List<User> getAllDrivers() {
        return userRepository.findByUserType("DRIVER");
    }

    // Update user
    public User updateUser(String id, User userDetails) {
        Optional<User> user = userRepository.findById(id);

        if (user.isPresent()) {
            User existingUser = user.get();
            existingUser.setName(userDetails.getName());
            existingUser.setPhone(userDetails.getPhone());
            existingUser.setRating(userDetails.getRating());
            return userRepository.save(existingUser);
        }

        throw new RuntimeException("User not found");
    }

    // Delete user
    public void deleteUser(String id) {
        userRepository.deleteById(id);
    }
}
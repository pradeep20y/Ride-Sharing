package com.ridesharing.project.service;

import com.ridesharing.project.entity.User;
import com.ridesharing.project.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    // Create new user
    public User createUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
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
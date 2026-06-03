package com.ridesharing.project.controller;

import com.ridesharing.project.dto.UserDTO;
import com.ridesharing.project.dto.response.ErrorResponse;
import com.ridesharing.project.entity.User;
import com.ridesharing.project.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
@CrossOrigin(origins = "*")
@Tag(name = "User Management", description = "APIs for managing users — create, retrieve, update, and delete passenger and driver accounts")
public class UserController {

    @Autowired
    private UserService userService;

    // Create new user
    @Operation(
        summary = "Create a new user",
        description = "Registers a new user (passenger or driver) in the system. Returns 400 if the email address is already in use."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "User created successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDTO.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request — email already exists or request body is invalid",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping
    public ResponseEntity<UserDTO> createUser(@RequestBody UserDTO user) {
        try {
            User createdUser = userService.createUser(user);
            return new ResponseEntity<>(convertToDTO(createdUser), HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
    }

    // Get all users
    @Operation(
        summary = "Get all users",
        description = "Retrieves a list of every user registered in the system, regardless of type."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "List of users returned successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDTO.class))
        )
    })
    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        List<UserDTO> dtos = users.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return new ResponseEntity<>(dtos, HttpStatus.OK);
    }

    // Get user by ID
    @Operation(
        summary = "Get user by ID",
        description = "Retrieves a single user by their unique identifier."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User found and returned successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDTO.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "User not found for the given ID",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(
            @Parameter(description = "Unique identifier of the user", required = true, example = "abc123")
            @PathVariable String id) {
        Optional<User> user = userService.getUserById(id);
        if (user.isPresent()) {
            return new ResponseEntity<>(convertToDTO(user.get()), HttpStatus.OK);
        }
        return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
    }

    // Get user by email
    @Operation(
        summary = "Get user by email",
        description = "Retrieves a single user whose email address matches the provided value."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User found and returned successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDTO.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "No user found with the given email address",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/email/{email}")
    public ResponseEntity<UserDTO> getUserByEmail(
            @Parameter(description = "Email address of the user to look up", required = true, example = "john.doe@example.com")
            @PathVariable String email) {
        Optional<User> user = userService.getUserByEmail(email);
        if (user.isPresent()) {
            return new ResponseEntity<>(convertToDTO(user.get()), HttpStatus.OK);
        }
        return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
    }

    // Get all passengers
    @Operation(
        summary = "Get all passengers",
        description = "Retrieves a list of all users whose type is PASSENGER."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "List of passengers returned successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDTO.class))
        )
    })
    @GetMapping("/type/passenger")
    public ResponseEntity<List<UserDTO>> getAllPassengers() {
        List<User> passengers = userService.getAllPassengers();
        List<UserDTO> dtos = passengers.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return new ResponseEntity<>(dtos, HttpStatus.OK);
    }

    // Get all drivers
    @Operation(
        summary = "Get all drivers",
        description = "Retrieves a list of all users whose type is DRIVER."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "List of drivers returned successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDTO.class))
        )
    })
    @GetMapping("/type/driver")
    public ResponseEntity<List<UserDTO>> getAllDrivers() {
        List<User> drivers = userService.getAllDrivers();
        List<UserDTO> dtos = drivers.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return new ResponseEntity<>(dtos, HttpStatus.OK);
    }

    // Update user
    @Operation(
        summary = "Update an existing user",
        description = "Updates the details of the user identified by the given ID. Returns 404 if the user does not exist."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User updated successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDTO.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "User not found for the given ID",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateUser(
            @Parameter(description = "Unique identifier of the user to update", required = true, example = "abc123")
            @PathVariable String id,
            @RequestBody User userDetails
    ) {
        try {
            User updatedUser = userService.updateUser(id, userDetails);
            return new ResponseEntity<>(convertToDTO(updatedUser), HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
    }

    // Delete user
    @Operation(
        summary = "Delete a user",
        description = "Permanently removes the user identified by the given ID from the system."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User deleted successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "User not found for the given ID",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(
            @Parameter(description = "Unique identifier of the user to delete", required = true, example = "abc123")
            @PathVariable String id) {
        try {
            userService.deleteUser(id);
            return new ResponseEntity<>("User deleted successfully", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("User not found", HttpStatus.NOT_FOUND);
        }
    }

    // Helper method to convert User to UserDTO
    private UserDTO convertToDTO(User user) {
        return new UserDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getPassword(),
                user.getUserType(),
                user.getRating(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}

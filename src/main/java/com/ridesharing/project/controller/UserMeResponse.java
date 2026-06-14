package com.ridesharing.project.controller;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserMeResponse {
    private String userId;      // User.id
    private String profileId;   // Driver.id or Passenger.id
    private String name;
    private String email;
    private String phone;
    private String userType;    // PASSENGER or DRIVER
    private Double rating;
}

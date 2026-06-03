package com.ridesharing.project.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

    private String id;
    private String name;
    private String email;
    private String phone;
    private String password;
    private String userType;
    private Double rating;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

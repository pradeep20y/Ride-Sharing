package com.ridesharing.project.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

// This entity represents a passenger profile linked to a user account, tracking wallet balance and membership status.
@Entity
@Table(name = "passengers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Passenger {

    @Id
    private String id = UUID.randomUUID().toString();

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private Double walletBalance = 0.0;

    @Column(nullable = false)
    private String status = "Active";

    @Column(name = "join_date", nullable = false, updatable = false)
    private LocalDateTime joinDate = LocalDateTime.now();
}

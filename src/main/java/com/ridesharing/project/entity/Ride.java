package com.ridesharing.project.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

// This entity represents an active or historical ride linking a ride request, a passenger, and a driver together.
@Entity
@Table(name = "rides")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Ride {

    @Id
    private String id = UUID.randomUUID().toString();

    @OneToOne
    @JoinColumn(name = "ride_request_id", nullable = false, unique = true)
    private RideRequest rideRequest;

    @ManyToOne
    @JoinColumn(name = "passenger_id", nullable = false)
    private Passenger passenger;

    @ManyToOne
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;

    @Column(nullable = false)
    private String status = "Assigned"; // Assigned, InProgress, Completed, Cancelled

    @Column(nullable = false)
    private Double fare;

    @Column(name = "assigned_date", nullable = false, updatable = false)
    private LocalDateTime assignedDate = LocalDateTime.now();

    @Column(name = "completed_date")
    private LocalDateTime completedDate;
}

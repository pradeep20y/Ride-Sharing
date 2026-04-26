package com.ridesharing.project.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

// This entity captures a passenger's request for a ride, storing pickup/dropoff details and automatically computed fare estimates.
@Entity
@Table(name = "ride_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RideRequest {

    @Id
    private String id = UUID.randomUUID().toString();

    @ManyToOne
    @JoinColumn(name = "passenger_id", nullable = false)
    private Passenger passenger;

    @Column(nullable = false)
    private Double pickupLatitude;

    @Column(nullable = false)
    private Double pickupLongitude;

    @Column(nullable = false)
    private String pickupAddress;

    @Column(nullable = false)
    private Double dropoffLatitude;

    @Column(nullable = false)
    private Double dropoffLongitude;

    @Column(nullable = false)
    private String dropoffAddress;

    @Column(nullable = false)
    private String rideType; // ECONOMY, COMFORT, PREMIUM

    @Column(nullable = false)
    private String status = "Open"; // Open, Matched, Completed, Cancelled

    @Column(nullable = false)
    private Double estimatedFare;

    @Column(nullable = false)
    private Double estimatedDistance;

    @Column(nullable = false)
    private Integer estimatedDuration;

    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate = LocalDateTime.now();
}

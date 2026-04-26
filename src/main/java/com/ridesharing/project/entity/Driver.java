package com.ridesharing.project.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "drivers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Driver {

    @Id
    private String id = UUID.randomUUID().toString();

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, unique = true)
    private String licensePlate;

    @Column(nullable = false)
    private String vehicleType; // ECONOMY, COMFORT, PREMIUM

    @Column(nullable = false)
    private String status; // OFFLINE, ONLINE, ON_TRIP (default: OFFLINE)

    @Column(name = "current_latitude")
    private Double currentLatitude;

    @Column(name = "current_longitude")
    private Double currentLongitude;

    @Column(name = "last_location_update")
    private LocalDateTime lastLocationUpdate;

    @Column(nullable = false)
    private Double rating = 5.0;

    @Column(name = "total_earnings")
    private Double totalEarnings = 0.0;

    @Column(name = "total_rides")
    private Integer totalRides = 0;

    @Column(name = "acceptance_rate")
    private Double acceptanceRate = 100.0; // 0-100

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
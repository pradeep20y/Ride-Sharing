package com.ridesharing.project.service;

import com.ridesharing.project.dto.response.DriverAssignedResponse;
import com.ridesharing.project.dto.response.LocationTrackingResponse;
import com.ridesharing.project.dto.response.NearbyDriverResponse;
import com.ridesharing.project.dto.response.RideOfferResponse;
import com.ridesharing.project.entity.Driver;
import com.ridesharing.project.entity.Ride;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;


import java.time.LocalDateTime;
import java.util.List;

@Service
public class RideNotificationService {
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void notifyNearbyDrivers(List<NearbyDriverResponse> nearbyDrivers,
                                    String requestId,
                                    String pickupAddress,
                                    String dropoffAddress,
                                    Double estimatedFare) {
        for (NearbyDriverResponse driver : nearbyDrivers) {
            RideOfferResponse offer = RideOfferResponse.builder()
                    .requestId(requestId)
                    .pickupAddress(pickupAddress)
                    .dropoffAddress(dropoffAddress)
                    .distanceFromDriverKm(driver.getDistanceInKm())
                    .estimatedFare(estimatedFare)
                    .offerExpiresInSeconds(10)
                    .build();

            messagingTemplate.convertAndSend(
                    "/topic/driver/"+driver.getDriverId(),
                    offer
            );
        }
    }

    public void notifyPassengerDriverAssigned(String passengerId, Ride ride, Driver driver) {

        DriverAssignedResponse response = DriverAssignedResponse.builder()
                .rideId(ride.getId())
                .driverName(driver.getUser().getName())
                .licensePlate(driver.getLicensePlate())
                .vehicleType(driver.getVehicleType())
                .driverRating(driver.getRating())
                .driverLatitude(driver.getCurrentLatitude())
                .driverLongitude(driver.getCurrentLongitude())
                .build();

        // Passenger's personal topic — only they receive this
        messagingTemplate.convertAndSend(
                "/topic/passenger/" + passengerId,
                response
        );
    }

    public void pushLocationToPassenger(String rideId, Double latitude, Double longitude) {

        LocationTrackingResponse tracking = LocationTrackingResponse.builder()
                .rideId(rideId)
                .latitude(latitude)
                .longitude(longitude)
                .timestamp(LocalDateTime.now())
                .build();

        // Ride-specific tracking topic — passenger subscribes to this after driver is assigned
        messagingTemplate.convertAndSend(
                "/topic/ride/" + rideId + "/tracking",
                tracking
        );
    }

    public void notifyPassengerRideCancelled(String passengerId, String reason) {

        messagingTemplate.convertAndSend(
                "/topic/passenger/" + passengerId,
                java.util.Map.of("status", "CANCELLED", "reason", reason)
        );
    }


}

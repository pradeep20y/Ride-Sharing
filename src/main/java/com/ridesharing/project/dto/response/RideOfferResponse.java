package com.ridesharing.project.dto.response;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RideOfferResponse {

    // The ride request ID — driver sends this back when accepting
    private String requestId;

    private String pickupAddress;
    private String dropoffAddress;

    // How far the driver currently is from the pickup point
    private Double distanceFromDriverKm;

    private Double estimatedFare;

    // How many seconds the driver has to accept before offer expires
    private Integer offerExpiresInSeconds;
}
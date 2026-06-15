package com.ridesharing.project.dto.response;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class RideConfirmedResponse {

    String status;
    String rideId;
    double pickupLatitude;
    double pickupLongitude;
    String pickupAddress;
    double dropoffLatitude;
    double dropoffLongitude;
    String dropoffAddress;
}
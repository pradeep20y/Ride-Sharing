package com.ridesharing.project.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NearbyDriverResponse {

    private String driverId;
    private Double distanceInKm;
    private Double latitude;
    private Double longitude;
}

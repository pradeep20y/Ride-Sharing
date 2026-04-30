package com.ridesharing.project.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Pushed to the passenger via WebSocket when a driver accepts their ride request
// Passenger uses this to display driver details and track their location
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverAssignedResponse {

    private String rideId;
    private String driverName;
    private String licensePlate;
    private String vehicleType;
    private Double driverRating;

    // Driver's current location so passenger can see them on the map
    private Double driverLatitude;
    private Double driverLongitude;
}

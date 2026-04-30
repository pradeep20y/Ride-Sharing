package com.ridesharing.project.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

// Pushed to the passenger during an active ride to show driver movement in real time
// Passenger's app uses latitude and longitude to update the driver pin on the map
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationTrackingResponse {

    private String rideId;
    private Double latitude;
    private Double longitude;
    private LocalDateTime timestamp;
}

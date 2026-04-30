package com.ridesharing.project.controller;

import com.ridesharing.project.dto.request.LocationUpdateRequest;
import com.ridesharing.project.dto.request.RideAcceptRequest;
import com.ridesharing.project.entity.Ride;
import com.ridesharing.project.service.LocationService;
import com.ridesharing.project.service.RideNotificationService;
import com.ridesharing.project.service.RideService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketController {

    @Autowired
    private RideService rideService;

    @Autowired
    private LocationService locationService;

    @Autowired
    private RideNotificationService rideNotificationService;

    // Receives ride acceptance from driver and delegates entirely to RideService
    @MessageMapping("/ride/accept")
    public void acceptRide(RideAcceptRequest request) {

        Ride ride = rideService.acceptRide(request.getRequestId(), request.getDriverId());

        String passengerId = ride.getPassenger().getId();
        rideNotificationService.notifyPassengerDriverAssigned(passengerId, ride, ride.getDriver());
    }

    // Receives location update from driver during active ride
    // Delegates location storage to LocationService and notification to RideNotificationService
    @MessageMapping("/ride/{rideId}/location")
    public void updateRideLocation(@DestinationVariable String rideId, LocationUpdateRequest location) {

        Ride ride = rideService.getRideIfActive(rideId);

        locationService.updateDriverLocation(ride.getDriver().getId(), location.getLatitude(), location.getLongitude());

        rideNotificationService.pushLocationToPassenger(rideId, location.getLatitude(), location.getLongitude());
    }
}

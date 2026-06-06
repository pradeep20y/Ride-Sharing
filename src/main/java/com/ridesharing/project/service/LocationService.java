package com.ridesharing.project.service;

import com.ridesharing.project.dto.response.NearbyDriverResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationService {

    // Redis sorted-set key under which all driver GPS positions are stored via GEO commands.
    private static final String DRIVER_GEO_KEY = "drivers:locations";

    // Default search radius used when a passenger requests nearby drivers without specifying one.
    private static final Double DEFAULT_RADIUS_KM = 5.0;

    private final RedisTemplate<String, String> redisTemplate;

 
    // Stores the driver's current GPS coordinates in the Redis GEO index.
    // Called every time a driver sends a location update so passengers always see accurate nearby drivers.
    // Note: Redis GEO expects longitude first, then latitude — opposite of the standard geographic convention.
    public void updateDriverLocation(String driverId, Double latitude, Double longitude) {
        redisTemplate.opsForGeo().add(
                DRIVER_GEO_KEY,
                new Point(longitude, latitude), // Redis takes longitude first, then latitude
                driverId
        );
    }

    // Removes a driver from the Redis GEO index when they go offline.
    // Called when a driver ends their shift so they no longer appear in nearby-driver searches.
    public void removeDriverLocation(String driverId) {
        redisTemplate.opsForZSet().remove(DRIVER_GEO_KEY, driverId); // GEO is built on a sorted set internally, so ZREM maps to opsForZSet
    }

    // Finds drivers within the default 5 km radius around the given coordinates.
    // This is the primary entry point used by the ride-matching flow when no custom radius is needed.
    public List<NearbyDriverResponse> findNearbyDrivers(Double latitude, Double longitude) {
        return findNearbyDrivers(latitude, longitude, DEFAULT_RADIUS_KM);
    }

    // Finds drivers within the given radius around the specified coordinates.
    // @param latitude   the passenger's current latitude
    // @param longitude  the passenger's current longitude
    // @param radiusKm   search radius in kilometres (caller-supplied, e.g. for surge or rural areas)
    @SuppressWarnings("deprecation")
    public List<NearbyDriverResponse> findNearbyDrivers(Double latitude, Double longitude, Double radiusKm) {
        Circle searchArea = new Circle(
                new Point(longitude, latitude), // Redis GEO uses longitude-first convention
                new Distance(radiusKm, Metrics.KILOMETERS)
        );

        GeoResults<RedisGeoCommands.GeoLocation<String>> results = redisTemplate.opsForGeo().radius(
                DRIVER_GEO_KEY,
                searchArea,
                RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                        .includeDistance()      // attach the km distance to each result so the client can show it
                        .includeCoordinates()   // attach lat/lng so the client can plot the driver on a map
                        .sortAscending()        // return nearest driver first for better UX
                        .limit(10)              // cap at 10 to prevent oversized response payloads
        );

        if (results == null) {
            return new ArrayList<>();
        }

        List<NearbyDriverResponse> nearby = new ArrayList<>();
        for (GeoResult<RedisGeoCommands.GeoLocation<String>> result : results.getContent()) {
            String driverId = result.getContent().getName();
            double distanceKm = Math.round(result.getDistance().getValue() * 100.0) / 100.0; // round to 2 decimal places
            Point coordinates = result.getContent().getPoint();

            nearby.add(NearbyDriverResponse.builder()
                    .driverId(driverId)
                    .distanceInKm(distanceKm)
                    .latitude(coordinates.getY())   // Point.getY() is latitude in Spring Data Geo
                    .longitude(coordinates.getX())  // Point.getX() is longitude in Spring Data Geo
                    .build());
        }

        return nearby;
    }

    // Returns the last known GPS position stored in Redis for the given driver.
    // Returns null when the driver is not present in the GEO index, meaning they are offline
    // or have never reported a location in the current Redis dataset.
    public Point getDriverLocation(String driverId) {
        List<Point> positions = redisTemplate.opsForGeo().position(DRIVER_GEO_KEY, driverId);
        if (positions == null || positions.isEmpty() || positions.get(0) == null) {
            return null; // driver not found in Redis — they are offline or have no recorded position
        }
        return positions.get(0);
    }

    // Calculates the straight-line distance in kilometres between two drivers currently tracked in Redis.
    // Useful for coordinating multi-driver pickups or verifying proximity before a handoff.
    // Returns null if either driver is not present in the GEO index.
    public Distance getDistanceBetweenDrivers(String driverId1, String driverId2) {
        return redisTemplate.opsForGeo().distance(
                DRIVER_GEO_KEY, driverId1, driverId2, Metrics.KILOMETERS); // null if either member is missing
    }

    // Returns the total number of driver entries currently tracked in the Redis GEO index.
    // Used by the monitoring endpoint to observe fleet availability without scanning MySQL.
    // Uses opsForZSet because Redis GEO is backed by a sorted set — size() reads the same ZCARD command.
    public Long getOnlineDriverCount() {
        return redisTemplate.opsForZSet().size(DRIVER_GEO_KEY); // ZCARD on the GEO sorted set
    }
}

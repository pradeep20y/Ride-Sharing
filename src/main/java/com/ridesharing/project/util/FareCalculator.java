package com.ridesharing.project.util;

// This utility class provides static methods for calculating ride distances and estimated fares using GPS coordinates.
public class FareCalculator {

    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double BASE_FARE = 5.00;
    private static final double RATE_PER_KM = 0.50;
    private static final double RATE_PER_MINUTE = 0.25;
    private static final double AVERAGE_SPEED_KMH = 20.0;

    // Calculates the straight-line distance between two GPS coordinates using the Haversine formula, returning kilometres.
    public static double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        // Convert the difference in latitude and longitude from degrees to radians
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        // Haversine formula: compute the square of half the chord length between the two points on a sphere
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);

        // Compute the central angle in radians using the inverse haversine
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        // Multiply by Earth's mean radius to get the arc distance in kilometres
        return EARTH_RADIUS_KM * c;
    }

    // Calculates the total ride fare from distance by summing a base charge, a per-kilometre charge, and a time-based charge.
    public static double calculateFare(double distanceKm) {
        // Fixed starting charge applied to every ride regardless of distance
        double baseFare = BASE_FARE;

        // Distance component: $0.50 per kilometre travelled
        double distanceFare = distanceKm * RATE_PER_KM;

        // Estimate travel time in minutes assuming an average city speed of 20 km/h
        double estimatedTimeMinutes = (distanceKm / AVERAGE_SPEED_KMH) * 60;

        // Time component: $0.25 per estimated minute of travel
        double timeFare = estimatedTimeMinutes * RATE_PER_MINUTE;

        // Sum all fare components and round to 2 decimal places for currency representation
        double totalFare = baseFare + distanceFare + timeFare;
        return Math.round(totalFare * 100.0) / 100.0;
    }

    // Estimates the ride duration in minutes based on distance and average driving speed.
    public static int calculateDuration(double distanceKm) {
        // Divide distance by average speed (km/h) then multiply by 60 to convert hours to minutes
        return (int) Math.round((distanceKm / AVERAGE_SPEED_KMH) * 60);
    }
}

package com.openclassrooms.tourguide.dto;

import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;

public record NearbyAttractionDTO(
        String attractionName,
        double attractionLatitude,
        double attractionLongitude,
        double userLatitude,
        double userLongitude,
        double distance,
        int rewardPoints
) {
    public NearbyAttractionDTO(
            Attraction attraction,
            VisitedLocation visitedLocation,
            double distance,
            int rewardPoints){
        this(
                attraction.attractionName,
                attraction.latitude,
                attraction.longitude,
                visitedLocation.location.latitude,
                visitedLocation.location.longitude,
                distance,
                rewardPoints
        );
    }
}

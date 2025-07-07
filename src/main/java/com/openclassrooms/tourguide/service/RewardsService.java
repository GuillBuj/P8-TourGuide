package com.openclassrooms.tourguide.service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;

/**
 * Service class for managing reward-related operations including reward calculation,
 * proximity checking, and distance calculations between locations and attractions.
 *
 * This service provides functionality to calculate rewards for users based on visited
 * locations and nearby attractions, with support for both synchronous and asynchronous
 * operations.
 */
@Service
public class RewardsService {
    private static final double STATUTE_MILES_PER_NAUTICAL_MILE = 1.15077945;

	// proximity in miles
    private int defaultProximityBuffer = 10;
	private int proximityBuffer = defaultProximityBuffer;
	private int attractionProximityRange = 200;
	private final GpsUtil gpsUtil;
	private final RewardCentral rewardsCentral;

	private final ExecutorService executorService = Executors.newFixedThreadPool(100);

	/**
	 * Constructs a RewardsService with dependencies.
	 *
	 * @param gpsUtil The GPS utility service for location and attraction data
	 * @param rewardCentral The reward central service for points calculation
	 */
	public RewardsService(GpsUtil gpsUtil, RewardCentral rewardCentral) {
		this.gpsUtil = gpsUtil;
		this.rewardsCentral = rewardCentral;
	}

	/**
	 * Sets a custom proximity buffer for reward eligibility.
	 *
	 * @param proximityBuffer The proximity distance in miles within which attractions
	 *                        are considered for rewards
	 */
	public void setProximityBuffer(int proximityBuffer) {
		this.proximityBuffer = proximityBuffer;
	}

	/**
	 * Resets the proximity buffer to the default value.
	 */
	public void setDefaultProximityBuffer() {
		proximityBuffer = defaultProximityBuffer;
	}

	/**
	 * Calculates rewards for a user based on their visited locations and nearby attractions.
	 *
	 * @param user The user for whom to calculate rewards
	 */
	public void calculateRewards(User user) {
		List<VisitedLocation> userLocations = user.getVisitedLocations();
		List<Attraction> attractions = gpsUtil.getAttractions();

		for (VisitedLocation visitedLocation : userLocations) {
			for (Attraction attraction : attractions) {
				if (nearAttraction(visitedLocation, attraction)) {
					int rewardPoints = getRewardPoints(attraction, user);
					UserReward reward = new UserReward(visitedLocation, attraction, rewardPoints);
					user.addUserReward(reward);
				}
			}
		}
	}

	/**
	 * Asynchronously calculates rewards for a user.
	 *
	 * @param user The user for whom to calculate rewards
	 * @return CompletableFuture that completes when reward calculation is done
	 */
	public CompletableFuture<Void> calculateRewardsAsync(User user) {
		return CompletableFuture.runAsync(() -> {
			calculateRewards(user);
		}, executorService);
	}

	/**
	 * Shuts down the executor service used for asynchronous operations.
	 */
	public void shutdown() {
		executorService.shutdown();
	}

	/**
	 * Checks if a location is within the attraction proximity range.
	 *
	 * @param attraction The attraction to check
	 * @param location The location to check
	 * @return true if the location is within the attraction proximity range, false otherwise
	 */
	public boolean isWithinAttractionProximity(Attraction attraction, Location location) {
		return getDistance(attraction, location) <= attractionProximityRange;
	}

	/**
	 * Checks if a visited location is near enough to an attraction to qualify for rewards.
	 *
	 * @param visitedLocation The visited location to check
	 * @param attraction The attraction to check
	 * @return true if within proximity buffer, false otherwise
	 */
	private boolean nearAttraction(VisitedLocation visitedLocation, Attraction attraction) {
		return getDistance(attraction, visitedLocation.location) <= proximityBuffer;
	}

	/**
	 * Gets the reward points for a user visiting a specific attraction.
	 *
	 * @param attraction The attraction visited
	 * @param user The user who visited the attraction
	 * @return The number of reward points earned
	 */
	public int getRewardPoints(Attraction attraction, User user) {
		return rewardsCentral.getAttractionRewardPoints(attraction.attractionId, user.getUserId());
	}

	/**
	 * Calculates the distance between two geographic locations.
	 *
	 * @param loc1 The first location
	 * @param loc2 The second location
	 * @return The distance between locations in statute miles
	 */
	public double getDistance(Location loc1, Location loc2) {
        double lat1 = Math.toRadians(loc1.latitude);
        double lon1 = Math.toRadians(loc1.longitude);
        double lat2 = Math.toRadians(loc2.latitude);
        double lon2 = Math.toRadians(loc2.longitude);

        double angle = Math.acos(Math.sin(lat1) * Math.sin(lat2)
                               + Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon1 - lon2));

        double nauticalMiles = 60 * Math.toDegrees(angle);
        return STATUTE_MILES_PER_NAUTICAL_MILE * nauticalMiles;
	}

}

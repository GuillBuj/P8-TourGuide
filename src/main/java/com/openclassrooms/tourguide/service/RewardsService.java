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
//	private final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 15);

	public RewardsService(GpsUtil gpsUtil, RewardCentral rewardCentral) {
		this.gpsUtil = gpsUtil;
		this.rewardsCentral = rewardCentral;
	}
	
	public void setProximityBuffer(int proximityBuffer) {
		this.proximityBuffer = proximityBuffer;
	}
	
	public void setDefaultProximityBuffer() {
		proximityBuffer = defaultProximityBuffer;
	}

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

//	public CompletableFuture<Void> calculateRewardsAsync(User user) {
//		List<VisitedLocation> visitedLocations = user.getVisitedLocations();
//		List<Attraction> allAttractions = gpsUtil.getAttractions();
//
//		Set<String> alreadyAwardedAttractions = user.getUserRewards().stream()
//				.map(r -> r.attraction.attractionName)
//				.collect(Collectors.toSet());
//
//		List<CompletableFuture<Void>> rewardTasks = visitedLocations.stream()
//				.flatMap(visited -> allAttractions.stream()
//						.filter(attraction -> !alreadyAwardedAttractions.contains(attraction.attractionName))
//						.filter(attraction -> nearAttraction(visited, attraction))
//						.map(attraction -> CompletableFuture.runAsync(() -> {
//							int rewardPoints = getRewardPoints(attraction, user);
//							synchronized (user) {
//								user.addUserReward(new UserReward(visited, attraction, rewardPoints));
//							}
//						}, executorService)))
//				.toList();
//
//		return CompletableFuture.allOf(rewardTasks.toArray(new CompletableFuture[0]));
//	}

	private static final int MAX_THREADS = Runtime.getRuntime().availableProcessors() * 4;
	private static final Semaphore semaphore = new Semaphore(MAX_THREADS);
	private static final ExecutorService executor = Executors.newCachedThreadPool();

	public CompletableFuture<Void> calculateRewardsAsync(User user) {
		return CompletableFuture.runAsync(() -> {
			calculateRewards(user);
		}, executorService);
	}

	public void shutdown() {
		executorService.shutdown();
	}
	
	public boolean isWithinAttractionProximity(Attraction attraction, Location location) {
		return getDistance(attraction, location) <= attractionProximityRange;
	}
	
	private boolean nearAttraction(VisitedLocation visitedLocation, Attraction attraction) {
		return getDistance(attraction, visitedLocation.location) <= proximityBuffer;
	}
	
	public int getRewardPoints(Attraction attraction, User user) {
		return rewardsCentral.getAttractionRewardPoints(attraction.attractionId, user.getUserId());
	}
	
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

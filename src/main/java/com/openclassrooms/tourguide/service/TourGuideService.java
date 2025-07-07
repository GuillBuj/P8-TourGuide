package com.openclassrooms.tourguide.service;

import com.openclassrooms.tourguide.dto.NearbyAttractionDTO;
import com.openclassrooms.tourguide.helper.InternalTestHelper;
import com.openclassrooms.tourguide.tracker.Tracker;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;

import tripPricer.Provider;
import tripPricer.TripPricer;

/**
 * Service class for managing tour guide operations including user tracking, rewards calculation,
 * and attraction recommendations.
 *
 * This service provides functionality to track user locations, calculate rewards,
 * get nearby attractions, and manage trip deals. It supports both synchronous and
 * asynchronous operations for better performance.
 */
@Service
public class TourGuideService {
	private Logger logger = LoggerFactory.getLogger(TourGuideService.class);
	private final GpsUtil gpsUtil;
	private final RewardsService rewardsService;
	private final TripPricer tripPricer = new TripPricer();
	public final Tracker tracker;
	boolean testMode = true;

	private final ExecutorService executor = Executors.newFixedThreadPool(100);

	/**
	 * Constructs a TourGuideService with dependencies.
	 *
	 * @param gpsUtil The GPS utility service for location tracking
	 * @param rewardsService The service for reward calculations
	 */
	public TourGuideService(GpsUtil gpsUtil, RewardsService rewardsService) {
		this.gpsUtil = gpsUtil;
		this.rewardsService = rewardsService;
		
		Locale.setDefault(Locale.US);

		if (testMode) {
			logger.info("TestMode enabled");
			logger.debug("Initializing users");
			initializeInternalUsers();
			logger.debug("Finished initializing users");
		}
		tracker = new Tracker(this);
		addShutDownHook();
	}

	/**
	 * Gets all rewards for a user.
	 *
	 * @param user The user to get rewards for
	 * @return List of user rewards
	 */
	public List<UserReward> getUserRewards(User user) {
		return user.getUserRewards();
	}

	/**
	 * Gets the user's last visited location or tracks a new location if none exists.
	 *
	 * @param user The user to get location for
	 * @return The visited location
	 */
	public VisitedLocation getUserLocation(User user) {
        return (!user.getVisitedLocations().isEmpty()) ? user.getLastVisitedLocation()
                : trackUserLocation(user);
	}

	/**
	 * Gets a user by username.
	 *
	 * @param userName The username to search for
	 * @return The User object or null if not found
	 */
	public User getUser(String userName) {
		return internalUserMap.get(userName);
	}

	/**
	 * Gets all users in the system.
	 *
	 * @return List of all users
	 */
	public List<User> getAllUsers() {
		return internalUserMap.values().stream().collect(Collectors.toList());
	}

	/**
	 * Adds a new user to the system if not already present.
	 *
	 * @param user The user to add
	 */
	public void addUser(User user) {
		if (!internalUserMap.containsKey(user.getUserName())) {
			internalUserMap.put(user.getUserName(), user);
		}
	}

	/**
	 * Gets trip deals for a user based on their preferences and reward points.
	 *
	 * @param user The user to get deals for
	 * @return List of trip providers with deals
	 */
	public List<Provider> getTripDeals(User user) {
		int cumulatativeRewardPoints = user.getUserRewards().stream().mapToInt(i -> i.getRewardPoints()).sum();
		List<Provider> providers = tripPricer.getPrice(tripPricerApiKey, user.getUserId(),
				user.getUserPreferences().getNumberOfAdults(), user.getUserPreferences().getNumberOfChildren(),
				user.getUserPreferences().getTripDuration(), cumulatativeRewardPoints);
		user.setTripDeals(providers);
		return providers;
	}

	/**
	 * Tracks a user's location synchronously and calculates rewards.
	 *
	 * @param user The user to track
	 * @return The visited location
	 */
	public VisitedLocation trackUserLocation(User user) {
		VisitedLocation visitedLocation = gpsUtil.getUserLocation(user.getUserId());
		user.addToVisitedLocations(visitedLocation);
		rewardsService.calculateRewards(user);
		return visitedLocation;
	}

	/**
	 * Tracks a user's location asynchronously and calculates rewards.
	 *
	 * @param user The user to track
	 * @return CompletableFuture containing the visited location
	 */
	public CompletableFuture<VisitedLocation> trackUserLocationAsync(User user) {
		return CompletableFuture.supplyAsync(() -> {
			VisitedLocation visitedLocation = gpsUtil.getUserLocation(user.getUserId());
			user.addToVisitedLocations(visitedLocation);
			rewardsService.calculateRewardsAsync(user);
			return visitedLocation;
		}, executor)
				.thenCompose(visitedLocation ->
						rewardsService.calculateRewardsAsync(user)
						.thenApply(ignore -> visitedLocation));
	}

	/**
	 * Tracks locations for all users asynchronously.
	 *
	 * @param users List of users to track
	 */
	public void trackAllUsersLocationAsync(List<User> users) {
		List<CompletableFuture<VisitedLocation>> futures = users.stream()
				.map(this::trackUserLocationAsync)
				.toList();

		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
	}

	/**
	 * Gets nearby attractions for a visited location.
	 *
	 * @param visitedLocation The location to search around
	 * @param user The user for reward point calculation
	 * @return List of nearby attractions with details
	 */
	public List<NearbyAttractionDTO> getNearByAttractions(VisitedLocation visitedLocation, User user) {
		return gpsUtil.getAttractions().stream()
				.map(attraction -> Map.entry(
						attraction,
						rewardsService.getDistance(attraction, visitedLocation.location)
				))
				.sorted(Comparator.comparingDouble(Map.Entry::getValue))
				.limit(5) //filtrage avant de calculer les rewardPoints
				.map(entry -> new NearbyAttractionDTO(
						entry.getKey(),
						visitedLocation,
						entry.getValue(),
						rewardsService.getRewardPoints(entry.getKey(), user)
				))
				.collect(Collectors.toList());
	}

	/**
	 * Adds a shutdown hook to stop tracking and shutdown executor service.
	 */
	private void addShutDownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			tracker.stopTracking();
			executor.shutdown();
		}));
	}

	/**********************************************************************************
	 * 
	 * Methods Below: For Internal Testing
	 * 
	 **********************************************************************************/
	private static final String tripPricerApiKey = "test-server-api-key";
	// Database connection will be used for external users, but for testing purposes
	// internal users are provided and stored in memory
	private final Map<String, User> internalUserMap = new HashMap<>();

	private void initializeInternalUsers() {
		IntStream.range(0, InternalTestHelper.getInternalUserNumber()).forEach(i -> {
			String userName = "internalUser" + i;
			String phone = "000";
			String email = userName + "@tourGuide.com";
			User user = new User(UUID.randomUUID(), userName, phone, email);
			generateUserLocationHistory(user);

			internalUserMap.put(userName, user);
		});
		logger.debug("Created " + InternalTestHelper.getInternalUserNumber() + " internal test users.");
	}

	private void generateUserLocationHistory(User user) {
		IntStream.range(0, 3).forEach(i -> {
			user.addToVisitedLocations(new VisitedLocation(user.getUserId(),
					new Location(generateRandomLatitude(), generateRandomLongitude()), getRandomTime()));
		});
	}

	private double generateRandomLongitude() {
		double leftLimit = -180;
		double rightLimit = 180;
		return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}

	private double generateRandomLatitude() {
		double leftLimit = -85.05112878;
		double rightLimit = 85.05112878;
		return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}

	private Date getRandomTime() {
		LocalDateTime localDateTime = LocalDateTime.now().minusDays(new Random().nextInt(30));
		return Date.from(localDateTime.toInstant(ZoneOffset.UTC));
	}

}

package com.yimei.modelbuilder;

public class ModelBuilder {
	
	public static int secondsPerDay = 24 * 3600; // seconds per day
	public static double walkingThreshold = 2.0; // 2km
	public static int defaultMaxNumOfTrips = 5;
	public static double defaultWalkingSpeed = 5.0/3600; // normal walking speed 5km/h = 5/3600 km/s
	public static int changeTime = 0; // 5min of change time
	public static double defaultMaxWalkingDistance = 1.0;
	public static double defaultMaxTailWalkingDistance = 2.0; // this is from source to the closest stop and from each stop to the target
}

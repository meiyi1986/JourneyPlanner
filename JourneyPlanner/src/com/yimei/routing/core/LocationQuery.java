package com.yimei.routing.core;

import com.yimei.modelbuilder.ModelBuilder;

// the query is not just from a stop to a stop, but from a location to a location

public class LocationQuery {

	private Location fromLoc;
	private Location toLoc;
	protected QueryTime now;
	protected int maxNumOfTrips;
	protected double maxWalkingDistance;
	protected double walkingSpeed;
	
	public LocationQuery(Location fromLoc, Location toLoc, String day, int now) {
		this.fromLoc = fromLoc;
		this.toLoc = toLoc;
		this.now = new QueryTime(now, day, 0);
		this.maxNumOfTrips = ModelBuilder.defaultMaxNumOfTrips;
		this.maxWalkingDistance = ModelBuilder.defaultMaxWalkingDistance;
		this.walkingSpeed = ModelBuilder.defaultWalkingSpeed;
	}
	
	public Location getFromLoc() {
		return fromLoc;
	}
	
	public Location getToLoc() {
		return toLoc;
	}
	
	public QueryTime getNow() {
		return now;
	}
	
	public String getDay() {
		return now.day;
	}
	
	public int getTime() {
		return now.time;
	}
	
	public int getOvernights() {
		return now.overnights;
	}
	
	public int getMaxNumOfTrips() {
		return maxNumOfTrips;
	}
	
	public double getMaxWalkingDistance() {
		return maxWalkingDistance;
	}
	
	public double getWalkingSpeed() {
		return walkingSpeed;
	}
	
	public void setNow(QueryTime now) {
		this.now = now;
	}
	
	
	public void printMe() {
		System.out.print("Query: from (" + fromLoc.lat + ", " + fromLoc.lon + ") to (" + toLoc.lat + ", " + toLoc.lon + ") at ");
		now.printIt();
	}
}

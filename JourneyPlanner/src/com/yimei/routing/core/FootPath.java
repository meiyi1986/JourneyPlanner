package com.yimei.routing.core;

/***
 * The foot-path from a stop to its neighboring stops
 * @author e04499
 *
 */

public class FootPath implements Comparable<FootPath> {

	String toStopId;
	double distance; // the Euclidean distance (shortest distance on the map)
	double walkingDistance; // by including the street information
	
	public FootPath (String toStopId, double distance) {
		this.toStopId = toStopId;
		this.distance = distance;
		this.walkingDistance = distance; // without further information, set walking distance to distance
	}
	
	public FootPath (String toStopId, double distance, double walkingDistance) {
		this.toStopId = toStopId;
		this.distance = distance;
		this.walkingDistance = walkingDistance;
	}
	
	public String getToStopId() {
		return toStopId;
	}
	
	public double getDistance() {
		return distance;
	}
	
	public double getWalkingDistance() {
		return walkingDistance;
	}
	
	public void setDistance(double distance) {
		this.distance = distance;
	}
	
	public void setWalkingDistance(double walkingDistance) {
		this.walkingDistance = walkingDistance;
	}
	
	
	
	public void printMe() {
		System.out.println(" walking to stop " + toStopId + ", distance = " + walkingDistance);
	}
	
	// sort by ascending order of walking distance
	public int compareTo(FootPath cmpFp) {
		double delta = this.walkingDistance - cmpFp.getWalkingDistance();
	    if (delta > 0.00001) return 1;
	    if (delta < -0.00001) return -1;
	    return 0;
	}
}

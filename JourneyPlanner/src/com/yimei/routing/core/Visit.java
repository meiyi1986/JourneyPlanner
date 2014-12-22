package com.yimei.routing.core;

public class Visit {

	private Location location;
	private int duration; // the duration of this visit (in seconds)
	
	public Visit(Location location, int duration) {
		this.location = location;
		this.duration = duration;
	}
	
	public Location getLocation() {
		return location;
	}
	
	public int getDuration() {
		return duration;
	}
}

package com.yimei.routing.core;

import com.yimei.util.GeoCalculator;

public class Location {

	public double lat;
	public double lon;
	
	public Location(double lat, double lon) {
		this.lat = lat;
		this.lon = lon;
	}
	
	public double distanceTo(Location loc) {
		return GeoCalculator.distFrom(this.lat, this.lon, loc.lat, loc.lon);
	}
}

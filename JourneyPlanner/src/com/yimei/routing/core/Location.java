package com.yimei.routing.core;

import com.google.gson.Gson;
import com.yimei.util.GeoCalculator;

public class Location {

	public double lat;
	public double lon;
	public String address;
	
	public Location() {
		
	}
	
	public Location(double lat, double lon, String address) {
		this.lat = lat;
		this.lon = lon;
		this.address = address;
	}
	
	public double distanceTo(Location loc) {
		return GeoCalculator.distFrom(this.lat, this.lon, loc.lat, loc.lon);
	}
	
	public static Location fromJSON(String jsonStr) {
		Location loc = new Location();
		
		Gson gson = new Gson();
		//convert the json string back to object
		loc = gson.fromJson(jsonStr, Location.class);
		
		return loc;
	}
	
	public void printMe() {
		System.out.println("LatLng: (" + lat + ", " + lon + ", Address: " + address);
	}
}

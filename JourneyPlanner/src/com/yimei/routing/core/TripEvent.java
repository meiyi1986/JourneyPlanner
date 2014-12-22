package com.yimei.routing.core;

public class TripEvent {

	private String stopId;
	private int arrivalTime;
	private int departureTime;
	
	/* constructor */
	
	public TripEvent(String id, int at, int dt) {
		this.stopId = id;
		this.arrivalTime = at;
		this.departureTime = dt;
	}
	
	/* methods */
	
	public String getStopId() {
		return stopId;
	}
	
	public int getArrivalTime() {
		return arrivalTime;
	}
	
	public int getDepartureTime() {
		return departureTime;
	}
}

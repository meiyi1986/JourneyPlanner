package com.yimei.routing.raptor;

import java.util.ArrayList;
import java.util.List;

/***
 * A route for search is an ordered sequence of stops. It can have several trips, departing at different times of the day
 * @author e04499
 *
 */

public class RaptorRoute {
	
	private String id; // route id
	private ArrayList<String> stopSequence; // the ordered sequence of stop ids
	private ArrayList<String> trips; // the trips belonging to the route
	
	/* constructor */
	
	public RaptorRoute(String id) {
		this.id = id;
		this.stopSequence = new ArrayList<String>();
		this.trips = new ArrayList<String>();
	}
	
	public RaptorRoute(String id, ArrayList<String> seq, ArrayList<String> trips) {
		this.id = id;
		this.stopSequence = seq;
		this.trips = trips;
	}
	
	/* methods */
	
	public String getId() {
		return id;
	}
	
	public ArrayList<String> getStopSequence() {
		return stopSequence;
	}
	
	public List<String> getStopSubSequence(int fromIndex) {
		return stopSequence.subList(fromIndex, stopSequence.size());
	}
	
	public List<String> getStopSubSequence(int fromIndex, int toIndex) {
		return stopSequence.subList(fromIndex, toIndex);
	}
	
	public String getStopId(int i) { // get the ith stop id in the route
		return stopSequence.get(i);
	}
	
	public int indexOfStopId(String id) {
		return stopSequence.indexOf(id);
	}
	
	public ArrayList<String> getTrips() {
		return trips;
	}
	
	public String getTripId(int i) {
		return trips.get(i);
	}
	
	public void addStopId(String id) {
		stopSequence.add(id);
	}
	
	public void addTripId(String id) {
		trips.add(id);
	}
	
	public boolean contrainsTrip(String id) {
		return trips.contains(id);
	}
	
	public boolean containsTripSequence(RaptorTrip tfs) {
		if (stopSequence.size() != tfs.getEventSequence().size()) {
			return false;
		}
		
		for (int i = 0; i < tfs.getEventSequence().size(); i++) {
			if (!tfs.getEvent(i).getStopId().equals(stopSequence.get(i))) {
				return false;
			}
		}
		
		return true;
	}
	
	public static RaptorRoute createWithTripSequence(RaptorTrip tfs, String routeId) {
		RaptorRoute rfs = new RaptorRoute(routeId);
		rfs.addTripId(tfs.getId());
		
		for (int i = 0; i < tfs.getEventSequence().size(); i++){
			rfs.addStopId(tfs.getEvent(i).getStopId());
		}
		
		return rfs;
	}
}

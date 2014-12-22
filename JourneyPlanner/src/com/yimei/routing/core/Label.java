package com.yimei.routing.core;

import com.yimei.routing.raptor.DepartureAtStop;
import com.yimei.util.TimeTransformer;

/***
 * The label
 * @author e04499
 *
 */

public class Label implements Comparable<Label>
{
	private String stopId; // the id of this stop
	private String prevId; // the previous stop id
	private String tripId; // the trip id from the previous stop to the current one
	private QueryTime departureTime; // the departure time from the previous stop
	private QueryTime arrivalTime; // the arrival time to this stop
	private DepartureAtStop nextDeparture; // the next departure based on the current arrival time, not used for now
	private int numOfTrips; // the number of trips to reach this stop from the source
	private int numOfWalkings; // the number of walkings to reach this stop from the source
	private Label preLabel; // the previous label (to fetch the journey recursively)
	
	public Label() {
		
	}
	
	public Label(String stopId) {
		this.stopId = stopId;
	}
	
	public Label(String stopId, String prevId, String tripId, QueryTime at) {
		this.stopId = stopId;
		this.prevId = prevId;
		this.tripId = tripId;
		this.arrivalTime = at;
	}
	
	public Label(String stopId, String prevId, String tripId, QueryTime dt, QueryTime at) {
		this.stopId = stopId;
		this.prevId = prevId;
		this.tripId = tripId;
		this.departureTime = dt;
		this.arrivalTime = at;
	}
	
	public Label(String stopId, String prevId, String tripId, QueryTime dt, QueryTime at, int ntr) {
		this.stopId = stopId;
		this.prevId = prevId;
		this.tripId = tripId;
		this.departureTime = dt;
		this.arrivalTime = at;
		this.numOfTrips = ntr;
	}
	
	public Label(String stopId, String prevId, String tripId, QueryTime dt, QueryTime at, int ntr, Label preLabel) {
		this.stopId = stopId;
		this.prevId = prevId;
		this.tripId = tripId;
		this.departureTime = dt;
		this.arrivalTime = at;
		this.numOfTrips = ntr;
		this.preLabel = preLabel;
	}
	
	public Label(String stopId, String prevId, String tripId, QueryTime dt, QueryTime at, DepartureAtStop nextDep, int ntr, int nwalk, Label preLabel) {
		this.stopId = stopId;
		this.prevId = prevId;
		this.tripId = tripId;
		this.departureTime = dt;
		this.arrivalTime = at;
		this.nextDeparture = nextDep;
		this.numOfTrips = ntr;
		this.numOfWalkings = nwalk;
		this.preLabel = preLabel;
	}
	
	public String getStopId() {
		return stopId;
	}
	
	public String getPrevId() {
		return prevId;
	}
	
	public String getTripId() {
		return tripId;
	}
	
	public QueryTime getDepartureTime() {
		return departureTime;
	}
	
	public QueryTime getArrivalTime() {
		return arrivalTime;
	}
	
	public DepartureAtStop getNextDeparture() {
		return nextDeparture;
	}
	
	public int getNumOfTrips() {
		return numOfTrips;
	}
	
	public int getNumOfWalkings() {
		return numOfWalkings;
	}
	
	public Label getPreLabel() {
		return preLabel;
	}
	
	public void setPrevId(String id) {
		this.prevId = id;
	}
	
	public void setTripId(String id) {
		this.tripId = id;
	}
	
	public void setDepatureTime(QueryTime dt) {
		this.departureTime = dt;
	}
	
	public void setArrivalTime(QueryTime at) {
		this.arrivalTime = at;
	}
	
	public void setNextDepartureTime(DepartureAtStop nextDep) {
		this.nextDeparture = nextDep;
	}
	
	public void setNumOfTrips(int ntr) {
		this.numOfTrips = ntr;
	}
	
	public void setNumOfWalkings(int nwalk) {
		this.numOfWalkings = nwalk;
	}
	
	public void setPreLabel(Label preLabel) {
		this.preLabel = preLabel;
	}
	
	public boolean arrivesEarlierThan(Label cmpLabel) {
		return this.arrivalTime.earlierThan(cmpLabel.getArrivalTime());
	}
	
	public int compareTo(Label cmpLabel) {
		if (this.arrivesEarlierThan(cmpLabel)) {
			if (this.numOfTrips <= cmpLabel.getNumOfTrips()){
				return -1;
			}
			else {
				return 0;
			}
		}
		else if (this.arrivalTime.equals(cmpLabel.getArrivalTime())) {
			if (this.numOfTrips < cmpLabel.getNumOfTrips()) {
				return -1;
			}
			else if (this.numOfTrips > cmpLabel.getNumOfTrips()) {
				return 1;
			}
			else {
				return 0;
			}
		}
		else {
			if (this.numOfTrips >= cmpLabel.getNumOfTrips()) {
				return 1;
			}
			else {
				return 0;
			}
		}
	}
	
	public void printMe() {
		System.out.println("Trip " + tripId + " from stop " + prevId + " at "
				+ TimeTransformer.IntegerToString(departureTime.time) + " on " + departureTime.day
				+ " to stop " + stopId + " at "
				+ TimeTransformer.IntegerToString(arrivalTime.time)
				+ " , currently with " + numOfTrips + " trips.");
	}
}

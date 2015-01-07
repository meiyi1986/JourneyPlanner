package com.yimei.routing.raptor;

import com.yimei.routing.query.QueryTime;

public class DepartureAtStop implements Comparable<DepartureAtStop> {

	private String tripId;
	private int index;
	private QueryTime departureTime;
	
	public DepartureAtStop(String tripId, int index, QueryTime dt) {
		this.tripId = tripId;
		this.index = index;
		this.departureTime = dt;
	}
	
	public String getTripId() {
		return tripId;
	}
	
	public int getIndex() {
		return index;
	}
	
	public QueryTime getDepartureTime() {
		return departureTime;
	}
	
	public static DepartureAtStop MAX_VALUE(String day) {
		DepartureAtStop das = new DepartureAtStop(null, 0, QueryTime.MAX_VALUE(day));
		return das;
	}
	
	
	public int compareTo(DepartureAtStop cmpObj) {
		return departureTime.compareTo(cmpObj.getDepartureTime());
	}
	
	public void printMe() {
		System.out.print("trip " + tripId + " departs from index " + index + " at ");
		departureTime.printIt();
	}
}

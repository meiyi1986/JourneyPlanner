package com.yimei.graph;

import java.util.ArrayList;

public class DailyTravelTimeFunc {
	private ArrayList<TemporalTravelTime> keyTravelTimes;
	
	/* constructor */
	
	public DailyTravelTimeFunc() {
		keyTravelTimes = new ArrayList<TemporalTravelTime>();
	}
	
	public DailyTravelTimeFunc(ArrayList<TemporalTravelTime> ttList) {
		keyTravelTimes = ttList;
	}
	
	/* methods */
	
	public ArrayList<TemporalTravelTime> getKeyTravelTimes() {
		return keyTravelTimes;
	}
	
	public void addTravelTime(TemporalTravelTime tt) {
		keyTravelTimes.add(tt);
	}
	
	// get the earliest key travel time (next departure)
	public TemporalTravelTime nextKeyTravelTime(int now) {
		for (int i = 0; i < keyTravelTimes.size(); i++) {
			if (keyTravelTimes.get(i).getTimePoint() == TemporalTravelTime.constantTimePoint || keyTravelTimes.get(i).getTimePoint() >= now) {
				return keyTravelTimes.get(i);
			}
		}
		
		return null; // no departure has been found (too late)
	}
	
	// get the travel time from now
	public int travelTimeFromNow(int now) {
		TemporalTravelTime nextKeyTT = nextKeyTravelTime(now);
		
		if (nextKeyTT.getTimePoint() == TemporalTravelTime.constantTimePoint) { // the travel time is constant independent of time
			return nextKeyTT.getValue();
		}
		
		return nextKeyTT.getValue() + nextKeyTT.getTimePoint() - now;
	}
}

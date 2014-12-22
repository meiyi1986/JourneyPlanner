package com.yimei.routing.raptor;

import java.util.ArrayList;
import java.util.List;

import com.conveyal.gtfs.GTFSFeed;
import com.yimei.routing.core.TripEvent;

/***
 * A trip for search is an ordered sequence of events. Each event has a stop id, arrival time and departure time
 * @author e04499
 *
 */

public class RaptorTrip {

	private String id; // trip id
	private String routeId; // the route id of this trip
	private ArrayList<TripEvent> eventSequence;
	private List<String> workingDays; // the working days of the route
	
	/* constructor */
	
	public RaptorTrip(String id, String rid) {
		this.id = id;
		this.routeId = rid;
		this.eventSequence = new ArrayList<TripEvent>();
		this.workingDays = new ArrayList<String>();
	}
	
	public RaptorTrip(String id, String rid, ArrayList<TripEvent> seq, List<String> wd) {
		this.id = id;
		this.routeId = rid;
		this.eventSequence = seq;
		this.workingDays = wd;
	}
	
	/* methods */
	
	public String getId() {
		return id;
	}
	
	public String getRouteId() {
		return routeId;
	}
	
	public ArrayList<TripEvent> getEventSequence() {
		return eventSequence;
	}
	
	public TripEvent getEvent(int i) { // get the ith index
		return eventSequence.get(i);
	}
	
	public int getArrivalTimeOfStop(int i) { // get the arrival time of the ith stop
		return eventSequence.get(i).getArrivalTime();
	}
	
	public int getDepartureTimeOfStop(int i) { // get the departure time of the ith stop
		return eventSequence.get(i).getDepartureTime();
	}
	
	public void addTripEvent(TripEvent te) {
		eventSequence.add(te);
	}
	
	public void setRouteId(String rid) {
		this.routeId = rid;
	}
	
	
	public List<String> getWorkingDays() {
		return workingDays;
	}
	
	public boolean containsWorkingDay(String day) {
		return workingDays.contains(day);
	}
	
	public void addWorkingDay(String day) {
		workingDays.add(day);
	}
	
	public void createWorkingDaysFromGTFSFeed(GTFSFeed feed) {
		workingDays.clear();
		String serviceId = feed.trips.get(id).service_id;
		
		if (feed.calendars.get(serviceId).monday == 1) {
			workingDays.add("Monday");
		}
		
		if (feed.calendars.get(serviceId).tuesday == 1) {
			workingDays.add("Tuesday");
		}
		
		if (feed.calendars.get(serviceId).wednesday == 1) {
			workingDays.add("Wednesday");
		}
		
		if (feed.calendars.get(serviceId).thursday == 1) {
			workingDays.add("Thursday");
		}
		
		if (feed.calendars.get(serviceId).friday == 1) {
			workingDays.add("Friday");
		}
		
		if (feed.calendars.get(serviceId).saturday == 1) {
			workingDays.add("Saturday");
		}
		
		if (feed.calendars.get(serviceId).sunday == 1) {
			workingDays.add("Sunday");
		}
	}
	
	public int firstIndexOfStop(String stopId, int startIdx) { // include startIdx
		int index = -1;
		for (int i = startIdx; i < eventSequence.size(); i++) {
			if (eventSequence.get(i).getStopId().equals(stopId)) {
				index = i;
				break;
			}
		}
		
		return index;
	}
	
	public int lastIndexOfStop(String stopId, int endIdx) { // exclude endIdx
		int index = -1;
		for (int i = 0; i < endIdx; i++) {
			if (eventSequence.get(i).getStopId().equals(stopId)) {
				index = i;
				break;
			}
		}
		
		return index;
	}
	
	public int indexOfDeparture(String stopId, int depTime) {
		int index = -1;
		for (int i = 0; i < eventSequence.size(); i++) {
			if (eventSequence.get(i).getDepartureTime() > depTime)
				break;
			
			if (eventSequence.get(i).getStopId().equals(stopId) && eventSequence.get(i).getDepartureTime() == depTime) {
				index = i;
				break;
			}
		}
		
		return index;
	}
	
	public int indexOfArrival(String stopId, int arrTime) {
		int index = -1;
		for (int i = 0; i < eventSequence.size(); i++) {
			if (eventSequence.get(i).getArrivalTime() > arrTime)
				break;
			
			if (eventSequence.get(i).getStopId().equals(stopId) && eventSequence.get(i).getArrivalTime() == arrTime) {
				index = i;
				break;
			}
		}
		
		return index;
	}
}

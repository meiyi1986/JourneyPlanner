package com.yimei.routing.core;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.conveyal.gtfs.model.Stop;
import com.yimei.routing.raptor.RaptorModel;
import com.yimei.sql.GtfsJDBC;
import com.yimei.util.TimeTransformer;

public class JourneyEvent {

	// for searching
	transient private String fromStopId;
	transient private String toStopId; // the previous stop id
	transient private String tripId; // the trip id from the previous stop to the current one
	transient private QueryTime departureTime;
	transient private QueryTime arrivalTime;
	
	// for showing the journey
	public Stop fromStop;
	public Stop toStop;
	public String route; // the route name
	public String departureTimeStr;
	public String arrivalTimeStr;
	public List<Location> trajectory;
	
	/* constructor*/
	
	public JourneyEvent (Label label) {
		this.fromStopId = label.getPrevId();
		this.toStopId = label.getStopId();
		this.tripId = label.getTripId();
		this.departureTime = label.getDepartureTime();
		this.arrivalTime = label.getArrivalTime();
	}
	
	public JourneyEvent (MoLabel mol) {
		this.fromStopId = mol.getPrevId();
		this.toStopId = mol.getStopId();
		this.tripId = mol.getTripId();
		this.departureTime = mol.getDepartureTime();
		this.arrivalTime = mol.getArrivalTime();
	}
	
	public JourneyEvent (String fsid, String tsid, String tid, QueryTime dt, QueryTime at) {
		this.fromStopId = fsid;
		this.toStopId = tsid;
		this.tripId = tid;
		this.departureTime = dt;
		this.arrivalTime = at;
	}
	
	/* methods */
	
	public String getFromStopId() {
		return fromStopId;
	}
	
	public String getToStopId() {
		return toStopId;
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
	
	public void setFromStopId(String fsid) {
		this.fromStopId = fsid;
	}
	
	public void setToStopId(String tsid) {
		this.toStopId = tsid;
	}
	
	public void setTripId(String tid) {
		this.tripId = tid;
	}
	
	public void setDepartureTime(QueryTime dt) {
		this.departureTime = dt;
	}
	
	public void setArrivalTime(QueryTime at) {
		this.arrivalTime = at;
	}
	
	public void getShowInfo(String database, RaptorModel model) throws ClassNotFoundException, SQLException {
		
		Connection conn = GtfsJDBC.connectDatabase(database);
		
		fromStop = GtfsJDBC.searchStopById(fromStopId, conn);
		toStop = GtfsJDBC.searchStopById(toStopId, conn);
		departureTimeStr = TimeTransformer.IntegerToString(departureTime.time) + ", " + departureTime.day;
		arrivalTimeStr = TimeTransformer.IntegerToString(arrivalTime.time) + ", " + arrivalTime.day;
		route = GtfsJDBC.routeNameOfTrip(tripId, conn);
		
		// get the trajectory
		trajectory = new ArrayList<Location>();
		if (tripId.equals("walking")) {
			trajectory.add(model.getStop(fromStopId).getLocation());
			trajectory.add(model.getStop(toStopId).getLocation());
		}
		else {
//			System.out.println("stop = " + fromStopId + ", time = " + departureTime.time + ", trip = " + tripId);
			int fromIndex = model.getTrip(tripId).indexOfDeparture(fromStopId, departureTime.time);
			int toIndex = model.getTrip(tripId).indexOfArrival(toStopId, arrivalTime.time);
			
			for (int i = fromIndex; i <= toIndex; i ++) {
				String stopId = model.getTrip(tripId).getEvent(i).getStopId();
				trajectory.add(model.getStop(stopId).getLocation());
			}
		}
	}
	
	public static void showJourney(List<JourneyEvent> journey) {
		if (journey == null) {
			System.out.println("Sorry, couldn't find any journey...");
			return;
		}
		
		for (int i = 0; i < journey.size(); i++) {
			System.out.println("Take trip " + journey.get(i).getTripId()
					+ " from stop " + journey.get(i).getFromStopId()
					+ " at " + TimeTransformer.IntegerToString(journey.get(i).getDepartureTime().time)
					+ " on " + journey.get(i).getDepartureTime().day
					+ ", arriving at stop " + journey.get(i).getToStopId()
					+ " on " + journey.get(i).getArrivalTime().day
					+ " at " + TimeTransformer.IntegerToString(journey.get(i).getArrivalTime().time));
		}
	}
}

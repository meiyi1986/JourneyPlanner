package com.yimei.routing.csa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.conveyal.gtfs.GTFSFeed;
import com.yimei.util.TimeTransformer;

/***
 * This is the elementary connection of the graph
 * @author Administrator
 *
 */

public class Connection implements Comparable<Connection> {

	private String id;
	private String fromStopId;
	private String toStopId;
	private String tripId;
	private int departureTime; // from fromStopId
	private int arrivalTime; // to toStopId
	
	/* constructor*/
	
	public Connection (String id) {
		this.id = id;
	}
	
	public Connection (String id, String fsid, String tsid, String tid, int dt, int at) {
		this.id = id;
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
	
	public int getDepartureTime() {
		return departureTime;
	}
	
	public int getArrivalTime() {
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
	
	public void setDepartureTime(int dt) {
		this.departureTime = dt;
	}
	
	public void setArrivalTime(int at) {
		this.arrivalTime = at;
	}
	
	
	
	public void printMe() {
		System.out.println(id + " from stop " + fromStopId + " at "
				+ TimeTransformer.IntegerToString(departureTime)
				+ " to stop " + toStopId + " at "
				+ TimeTransformer.IntegerToString(arrivalTime));
	}
	
	// sort by ascending order of departure time
	public int compareTo(Connection cmpConnection) {
		return this.departureTime - cmpConnection.departureTime;
	}
	

}

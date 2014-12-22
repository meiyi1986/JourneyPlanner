package com.yimei.graph;

import java.util.Map;

/***
 * This is the station vertex, each of which indicates a station.
 * @author e04499
 *
 */
public class StationVertex extends Vertex
{
	String stopId; // the id of the station that the vertex represents
	int numOfDepartures;
	int numOfArrivals;
	
	/* constructor */
	
	public StationVertex (int index, double lat, double lon, String stopId) {
		super(index, lat, lon);
		this.stopId = stopId;
		this.numOfDepartures = 0;
		this.numOfArrivals = 0;
	}
	
	/* methods */
	
	public String getStopId() {
		return stopId;
	}
}

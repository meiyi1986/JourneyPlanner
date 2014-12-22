package com.yimei.graph;

import java.util.Map;

/***
 * This is a vertex that supports transfer. Each stop consists of one stop vertex, with route index of null, and one route vertex
 * for each route passing through it.
 * @author e04499
 *
 */
public class TransferVertex extends Vertex
{
	String stopId; // the id of the stop
	String routeId; // route id, null if it is a stop vertex
	
	/* constructor */
	
	public TransferVertex (int index, double lat, double lon, String stopId, String routeId) {
		super(index, lat, lon);
		this.stopId = stopId;
		this.routeId = routeId;
	}
	
	/* methods */
	
	public String getStopId() {
		return stopId;
	}
	
	public String getRouteId() {
		return routeId;
	}
}

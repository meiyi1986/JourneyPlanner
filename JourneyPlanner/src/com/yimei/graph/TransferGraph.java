package com.yimei.graph;

import java.util.Map;

import org.mapdb.Fun.Tuple2;

import com.google.common.collect.Maps;

/***
 * The graph that supports transfer.
 * @author e04499
 *
 */

public class TransferGraph extends Graph
{
	private Map<Tuple2<String,String>, Vertex> vertexMap;
	
	public TransferGraph() {
		super();
		vertexMap = Maps.newHashMap();
	}
	
	public Vertex getStopRouteVertex(String stopId, String routeId) {
		Tuple2<String,String> key = new Tuple2<String,String>(stopId, routeId);
		return vertexMap.get(key);
	}
	
	public Arc getStopRouteArc(String fromStopId, String toStopId, String routeId) {
		Vertex fromV = getStopRouteVertex(fromStopId, routeId);
		Vertex toV = getStopRouteVertex(toStopId, routeId);
		return getVerticesArc(fromV, toV);
	}
	
	public void addVertexMap(String stopId, String routeId, Vertex v) {
		Tuple2<String,String> key = new Tuple2<String,String>(stopId, routeId);
		vertexMap.put(key, v);
	}
	
	public boolean vertexMapContains(String stopId, String routeId) {
		Tuple2<String,String> key = new Tuple2<String,String>(stopId, routeId);
		return vertexMap.containsKey(key);
	}
	
	public int travelTimeFromNow(Vertex fromV, Vertex toV, String day, int now) {
		TimeDependentArc a = (TimeDependentArc) getVerticesArc(fromV, toV);
		return a.travelTimeFromNow(day, now);
	}
	
	public int travelTimeFromNow(String fromStopId, String toStopId, String routeId, String day, int now) {
		Vertex fromV = getStopRouteVertex(fromStopId, routeId);
		Vertex toV = getStopRouteVertex(toStopId, routeId);
		return travelTimeFromNow(fromV, toV, day, now);
	}
	
	public int getNumOfStopVertices() {
		int n = 0;
		for (Tuple2<String,String> key : vertexMap.keySet()) {
			if (key.b == null) {
				n ++;
			}
		}
		
		return n;
	}
}

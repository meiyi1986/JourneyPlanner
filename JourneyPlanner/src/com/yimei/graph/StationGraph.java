package com.yimei.graph;

import java.util.Map;

import com.google.common.collect.Maps;

public class StationGraph extends Graph
{
	private Map<String, Vertex> vertexOfStop; // the vertex of each stop id, not the same as stop id
	
	public StationGraph() {
		super();
		vertexOfStop = Maps.newHashMap();
	}
	
	public Vertex getStopVertex(String stopId) {
		return vertexOfStop.get(stopId);
	}
	
	public void addStopVertex(String stopId, Vertex v) {
		vertexOfStop.put(stopId, v);
	}
	
	public Arc getStopsArc(String fromStopId, String toStopId) {
		Vertex fromV = getStopVertex(fromStopId);
		Vertex toV = getStopVertex(toStopId);
		return getVerticesArc(fromV, toV);
	}
}

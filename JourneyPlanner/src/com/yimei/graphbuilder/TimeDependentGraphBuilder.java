package com.yimei.graphbuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.SortedSet;
import java.util.TreeSet;

import org.mapdb.Fun.Tuple2;

import com.conveyal.gtfs.GTFSFeed;
import com.conveyal.gtfs.model.Calendar;
import com.conveyal.gtfs.model.Stop;
import com.conveyal.gtfs.model.StopTime;
import com.conveyal.gtfs.model.Trip;
import com.google.gson.Gson;
import com.yimei.graph.Arc;
import com.yimei.graph.ArcWeightInfo;
import com.yimei.graph.StationGraph;
import com.yimei.graph.StationVertex;
import com.yimei.graph.TemporalTravelTime;
import com.yimei.graph.TimeDependentArc;
import com.yimei.graph.TransferGraph;
import com.yimei.graph.TransferVertex;
import com.yimei.graph.TravelTimeFunc;
import com.yimei.graph.Vertex;

/***
 * The graph supports transfer, including walking paths
 * @author e04499
 *
 */

public class TimeDependentGraphBuilder
{
	public static double walkingThreshold = 0.1; // 0.1km = 100m
	public static double walkingSpeed = 5.0/3600; // normal walking speed 5km/h = 5/3600 km/s
	
	public static TransferGraph fromGTFSFeed(GTFSFeed feed) {
		TransferGraph tdGraph = new TransferGraph();
		
		tdGraph.feedLabel = feed.label;
		
		// create the stop vertices for stops by scanning stops.txt
		SortedSet<String> stopKeys = new TreeSet<String>(feed.stops.keySet());
		for (String key : stopKeys) {
			Stop s = feed.stops.get(key);
			
			// create the stop vertex
			TransferVertex v = new TransferVertex(tdGraph.getNumOfVertices(), s.stop_lat, s.stop_lon, s.stop_id, null);
			tdGraph.addVertex(v);
			tdGraph.addVertexMap(s.stop_id, null, v);
		}
		
		// add the walking arcs between stop vertices
		for (int i = 0; i < tdGraph.getNumOfVertices()-1; i++) {
			TransferVertex v1 = (TransferVertex) tdGraph.getVertex(i);
			for (int j = i+1; j < tdGraph.getNumOfVertices(); j++) {
				TransferVertex v2 = (TransferVertex) tdGraph.getVertex(j);
				
				double distance = Vertex.distanceBetween(v1, v2);
				if (distance < walkingThreshold) {
					double value = distance/walkingSpeed;
					
//					System.out.println(distance + "/" + walkingSpeed + " = " + value);
					
					TravelTimeFunc ttf = TravelTimeFunc.createConstantFunc((int)value, "Walking");
					TimeDependentArc a1 = new TimeDependentArc(tdGraph.getNumOfArcs(), v1, v2, ttf);
					tdGraph.addArc(a1);
					TimeDependentArc a2 = new TimeDependentArc(tdGraph.getNumOfArcs(), v2, v1, ttf);
					tdGraph.addArc(a2);
					
//					System.out.println("Add walk between stop " + v1.getStopId() + " to stop " + v2.getStopId() + " for " + a1.travelTimeFromNow("Monday", 100));
				}
			}
		}
		
		// create the route vertices for the stops by scanning stop_times.txt
		SortedSet<Tuple2> stopTimeKeys = new TreeSet<Tuple2>(feed.stop_times.keySet());
		for (Tuple2 key : stopTimeKeys) {
			StopTime tmpStopTime = feed.stop_times.get(key);
			String stopId = tmpStopTime.stop_id;
			String routeId = feed.trips.get(tmpStopTime.trip_id).route_id;
			if (tdGraph.vertexMapContains(stopId, routeId)) {
				continue;
			}
			
			Stop tmpStop = feed.stops.get(stopId);
			TransferVertex v = new TransferVertex(tdGraph.getNumOfVertices(), tmpStop.stop_lat, tmpStop.stop_lon, stopId, routeId);
			tdGraph.addVertex(v);
			tdGraph.addVertexMap(stopId, routeId, v);
			
			// add arcs between route vertices and stop vertex
			TransferVertex stopVertex = (TransferVertex) tdGraph.getStopRouteVertex(stopId, null);
			TimeDependentArc a = new TimeDependentArc(tdGraph.getNumOfArcs(), stopVertex, v);
			a.getTravelTimeFunc().createConstantFunc(0, "Outgoing transfer");
			tdGraph.addArc(a);
			
			TimeDependentArc a2 = new TimeDependentArc(tdGraph.getNumOfArcs(), v, stopVertex);
			a2.getTravelTimeFunc().createConstantFunc(0, "Incoming transfer");
			tdGraph.addArc(a2);
			
//			System.out.println("Add vertex " + v.getId() + " for stop " + stopId + " and route " + routeId);
		}
		
		// create edges and time-dependent travel time functions
		StopTime fromStopTime = null; // initialize a null from
		for (Tuple2 key : stopTimeKeys) {
//			System.out.println(key.a + ", " + key.b + " : " + feed.stop_times.get(key).stop_id);
			
			StopTime toStopTime = feed.stop_times.get(key);
			
			if (fromStopTime != null) { // not the beginning
				if (toStopTime.trip_id.equals(fromStopTime.trip_id)) { // from and to are in the same trip, causing an arc
					String routeId = feed.trips.get(toStopTime.trip_id).route_id;
					// find the vertices in the graph
					TransferVertex fromV = (TransferVertex) tdGraph.getStopRouteVertex(fromStopTime.stop_id, routeId);
					TransferVertex toV = (TransferVertex) tdGraph.getStopRouteVertex(toStopTime.stop_id, routeId);
					
//					System.out.println("trip " + toStopTime.trip_id + ", from index " + fromV.getStopId() + " to " + toV.getStopId());
					
					// create or obtain the arc <fromV,toV> from the graph
					TimeDependentArc a = (TimeDependentArc) tdGraph.getVerticesArc(fromV, toV);
					if (a == null) {
						// create an arc and add to the graph
						a = new TimeDependentArc(tdGraph.getNumOfArcs(), fromV, toV);
						tdGraph.addArc(a);
					}
					
					// update the travel time function of a
					// get the temporal travel time
					TemporalTravelTime tt = new TemporalTravelTime(fromStopTime.departure_time, 
							toStopTime.arrival_time-fromStopTime.departure_time,
							new ArcWeightInfo(feed.routes.get(routeId).route_long_name));
					
					// get all the available day labels of this trip
					Trip tmpTrip = feed.trips.get(toStopTime.trip_id);
					Calendar tmpCal = feed.calendars.get(tmpTrip.service_id);
					if (tmpCal.monday == 1) { // trip available on Monday
						a.getTravelTimeFunc().addTravelTime("Monday", tt);
					}
					if (tmpCal.tuesday == 1) {
						a.getTravelTimeFunc().addTravelTime("Tuesday", tt);
					}
					if (tmpCal.wednesday == 1) {
						a.getTravelTimeFunc().addTravelTime("Wednesday", tt);
					}
					if (tmpCal.thursday == 1) {
						a.getTravelTimeFunc().addTravelTime("Thursday", tt);
					}
					if (tmpCal.friday == 1) {
						a.getTravelTimeFunc().addTravelTime("Friday", tt);
					}
					if (tmpCal.saturday == 1) {
						a.getTravelTimeFunc().addTravelTime("Saturday", tt);
					}
					if (tmpCal.sunday == 1) {
						a.getTravelTimeFunc().addTravelTime("Sunday", tt);
					}
				}
			}
			
			// move on
			fromStopTime = toStopTime;
		}
		
		String fromStopId = "6693";
		String toStopId = "6694";
		String routeId = "GAWC";
		System.out.println("Time " + tdGraph.travelTimeFromNow(fromStopId, toStopId, routeId, "Sunday", 86300));
		
		return tdGraph;
	}

	public static void graphToJSON(TransferGraph tdGraph) {
		// set directory
		String dirStr = "data/JSON/" + tdGraph.feedLabel;
		File directory = new File(dirStr);
		if (!directory.exists()) {
			try{
				directory.mkdir();
		    } catch(SecurityException se){
			   //handle it
			}        
		}
		
		// 
	}
	
	// main for testing
	public static void main (String[] args) {
		String INPUT = "data/GTFS/Adelaide.zip";
		
        GTFSFeed feed = GTFSFeed.fromFile(INPUT);
//        feed.findPatterns();
        
//        for(Map.Entry<String, Trip> entry: feed.trips.entrySet()) {
//            System.out.println(entry.getKey() + " : " + entry.getValue().service_id);
//        }
        
        TransferGraph tdGraph = TimeDependentGraphBuilder.fromGTFSFeed(feed);
        
        int n = tdGraph.getNumOfStopVertices();
        
        System.out.println("There are " + tdGraph.getNumOfVertices() + " vertices, " + tdGraph.getNumOfArcs() + " arcs.");
        System.out.println("There are " + n + " stop vertices.");
        
        
//        Gson gson = new Gson();
//		String json = gson.toJson(tdGraph);
////        System.out.println(json);
//		
//        try {
//        	//write converted json data to a file named "file.json"
//    		FileWriter writer = new FileWriter("data/JSON/AdelaideTDGraph.json");
//    		writer.write(json);
//    		writer.close();
//     
//    	} catch (IOException e) {
//    		e.printStackTrace();
//    	}
    }
}

package com.yimei.routing.csa;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.mapdb.Fun;
import org.mapdb.Fun.Tuple2;

import com.conveyal.gtfs.GTFSFeed;
import com.conveyal.gtfs.model.Stop;
import com.conveyal.gtfs.model.StopTime;
import com.conveyal.gtfs.model.Trip;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.yimei.modelbuilder.ModelBuilder;
import com.yimei.routing.core.FootPath;
import com.yimei.routing.core.RoutingTrip;
import com.yimei.util.TimeTransformer;

/***
 * The Connection Scanning Algorithm model
 * @author Administrator
 *
 */

public class CsaModel {
	
	private Map<String, CsaStop> stops;
	private List<Connection> connections;
	private Map<String, RoutingTrip> trips;
	private Map<String, List<FootPath>> stopFootPaths; // the foot paths of each stop

	
	public CsaModel() {
		stops = Maps.newHashMap();
		connections = new ArrayList<Connection>();
		trips = Maps.newHashMap();
		stopFootPaths = Maps.newHashMap();
		
	}
	
	public Map<String, CsaStop> getStops() {
		return stops;
	}
	
	public CsaStop getStop(String stopId) {
		return stops.get(stopId);
	}
	
	public List<Connection> getConnections() {
		return connections;
	}
	
	public Map<String, RoutingTrip> getTrips() {
		return trips;
	}
	
	public RoutingTrip getTrip(String tripId) {
		return trips.get(tripId);
	}
	
	public Map<String, List<FootPath>> getFootPaths() {
		return stopFootPaths;
	}
	
	public List<FootPath> getFootPathOfStop(String stopId) {
		return stopFootPaths.get(stopId);
	}
	
	public boolean containsStop(String stopId) {
		return stops.containsKey(stopId);
	}
	
	public void addStop(CsaStop cs) {
		stops.put(cs.getId(), cs);
	}
	
	public void addConnection(Connection cc) {
		connections.add(cc);
	}
	
	public void addTrip(RoutingTrip rt) {
		trips.put(rt.getId(), rt);
	}
	
	public void addStopFootPaths(String stopId, List<FootPath> footPaths) {
		stopFootPaths.put(stopId, footPaths);
	}
	
	// the earliest connection index which departs no earlier than now
	public int earliestConnectionIndex(int now) {
		for (int i = 0; i < connections.size(); i++) {
			if (connections.get(i).getDepartureTime() >= now)
				return i;
		}
		
		return connections.size();
	}
	
//	private int pivotCeilingIndex(int now, int low, int high) {
//		if (high == low+1) {
//			return high;
//		}
//		
//		int pivotIndex = low + (high-low)/2;
//		int pivot = connections.get(pivotIndex).getDepartureTime();
//		if (pivot >= now) {
//			if (connections.get(pivotIndex-1).getDepartureTime() < now) {
//				return pivotIndex;
//			}
//			else {
//				return pivotCeilingIndex(now, low, pivotIndex);
//			}
//		}
//		else {
//			if (connections.get(pivotIndex+1).getDepartureTime() >= now) {
//				return pivotIndex+1;
//			}
//			else {
//				return pivotCeilingIndex(now, pivotIndex+1, high);
//			}
//		}
//	}
	
	
	
	
	
	public static CsaModel fromGTFSFeed(GTFSFeed feed) {
		CsaModel model = new CsaModel();
		
		// construct all the stops by reading GTFS - stops
		for (Stop s : feed.stops.values()) {
			CsaStop cs = new CsaStop(s.stop_id);
			model.addStop(cs);
		}
		
		// construct all the connections by reading GTFS trips and stop_times
		for (Trip t : feed.trips.values()) {
			
			// find all the serving days of this trip
			List<String> wd = t.getWorkingDaysFromGTFSFeed(feed);
			
			RoutingTrip rt = new RoutingTrip(t.trip_id, wd);
			model.addTrip(rt);
			
//			System.out.print("Trip " + tfs.getId() + " works on ");
//			for (String str : tfs.getWorkingDays()) {
//				System.out.print(str + ", ");
//			}
//			System.out.println("");
			
			// get the submap of this trip in stop_times
			SortedMap<Tuple2, StopTime> tripStopTimes = 
					feed.stop_times.subMap(Fun.t2(t.trip_id, null), Fun.t2(t.trip_id, Fun.HI));

//			System.out.println("Sequence for trip " + t.trip_id);
//			for (StopTime st : tripStopTimes.values()) {
//				System.out.print(st.stop_id + " ");
//			}
//			System.out.println("");
			
			StopTime st = tripStopTimes.get(tripStopTimes.firstKey());
			Connection cc = new Connection(st.stop_id + "-" + st.trip_id + "-" + TimeTransformer.IntegerToString(st.departure_time));
			cc.setFromStopId(st.stop_id);
			cc.setTripId(st.trip_id);
			cc.setDepartureTime(st.departure_time);
			
			for (Map.Entry<Tuple2, StopTime> entry : tripStopTimes.entrySet()) {
				if (entry.getKey().equals(tripStopTimes.firstKey())) { // skip the first key
					continue;
				}
				
				st = entry.getValue();
				cc.setToStopId(st.stop_id);
				cc.setArrivalTime(st.arrival_time);
				
				model.addConnection(cc);
				
				cc = new Connection(st.stop_id + "-" + st.trip_id + "-" + TimeTransformer.IntegerToString(st.departure_time));
				cc.setFromStopId(st.stop_id);
				cc.setTripId(st.trip_id);
				cc.setDepartureTime(st.departure_time);
			}
			
//			for (Connection c : model.getConnections()) {
//				c.printIt();
//			}
//			System.exit(0);
		}
		
		// sort the connections in the ascending order ot departure time
		Collections.sort(model.getConnections());
		
		// add the index of each connection to the stop it departs from
		for (int i = 0; i < model.getConnections().size(); i++) {
			String fromStopId = model.getConnections().get(i).getFromStopId();
			model.getStop(fromStopId).addConnection(i);
		}
		
		// add foot-paths of stops
		for (CsaStop csaStop1 : model.getStops().values()) {
			// create the list of foot-paths of this stop
			List<FootPath> fpList = new ArrayList<FootPath>();
			
			// Stop of s1
			Stop s1 = feed.stops.get(csaStop1.getId());
			for (CsaStop csaStop2 : model.getStops().values()) {
				if (csaStop1.equals(csaStop2)) {
					continue;
				}
				
				// Stop of s2
				Stop s2 = feed.stops.get(csaStop2.getId());
				double distance = Stop.distanceBetween(s1, s2);
				
//				System.out.println("Distance between " + s1.stop_id + " and " + s2.stop_id + " is " + distance);
				
				if (distance < ModelBuilder.walkingThreshold) {
					FootPath fp = new FootPath(csaStop2.getId(), distance);
					fpList.add(fp);
				}
			}
			
			// sort the foot-paths in ascending order of walking distance
			Collections.sort(fpList);
			model.addStopFootPaths(csaStop1.getId(), fpList);
		}
		
		return model;
	}
	
	public void toJSON(String dirStr) {
		// set directory
		File directory = new File(dirStr);
		if (!directory.exists()) {
			try{
				directory.mkdir();
		    } catch(SecurityException se){
			   //handle it
			}        
		}
		
		Gson gson = new Gson();
		String json = gson.toJson(this);
//	      System.out.println(json);
			
		try {
			//write converted json data to a file named "file.json"
			FileWriter writer = new FileWriter(dirStr + "/CsaModel.json");
			writer.write(json);
			writer.close();
		} catch (IOException e) {
	  		e.printStackTrace();
	  	}
	}
	
	public static CsaModel fromJSON(String jsonFile) {
		CsaModel model = new CsaModel();
		
		Gson gson = new Gson();
		try {
	 
			BufferedReader br = new BufferedReader(
				new FileReader(jsonFile));
	 
			//convert the json string back to object
			Type typeOfQueryList = new TypeToken<CsaModel>(){}.getType();
			model = gson.fromJson(br, typeOfQueryList);
	 
//			System.out.println(model);
	 
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return model;
	}
	
//	// serialization methods
//	public static CsaModel deserialize(String fileName) {
//		CsaModel model = null;
//		try {
//			FileInputStream fileIn = new FileInputStream(fileName);
//			ObjectInputStream in = new ObjectInputStream(fileIn);
//			model = (CsaModel) in.readObject();
//			in.close();
//			fileIn.close();
//    	}
//		catch(IOException i) {
//			i.printStackTrace();
//		}
//		catch(ClassNotFoundException c) {
//			System.out.println("Employee class not found");
//			c.printStackTrace();
//		}
//	    
//		return model;
//    }
//
//    public void serialize(String fileName){
//    	System.out.println(fileName);
//    	try {  
//    		FileOutputStream fileOut = new FileOutputStream(fileName);
//    		ObjectOutputStream out = new ObjectOutputStream(fileOut);
//    		out.writeObject(this);
//    		out.close();
//    		fileOut.close();
//    		System.out.printf("Serialized data is saved in " + fileName);
//    	}
//    	catch(IOException i) { 
//    		i.printStackTrace();
//        }
//    }
	
	public static void main(String[] args) {
		String database = "Adelaide";
		String gtfsDir = "data/GTFS/" + database;
		String jsonDir = "data/JSON/" + database;
		
		String INPUT = "data/GTFS/" + database + ".zip";
        GTFSFeed feed = GTFSFeed.fromFile(INPUT);
        CsaModel model = CsaModel.fromGTFSFeed(feed);
        model.toJSON("data/JSON/Adelaide");

       
	}
}

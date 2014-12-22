package com.yimei.routing.raptor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.mapdb.Fun;
import org.mapdb.Fun.Tuple2;

import com.conveyal.gtfs.GTFSFeed;
import com.conveyal.gtfs.model.Route;
import com.conveyal.gtfs.model.Stop;
import com.conveyal.gtfs.model.StopTime;
import com.conveyal.gtfs.model.Trip;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.yimei.modelbuilder.ModelBuilder;
import com.yimei.routing.core.FootPath;
import com.yimei.routing.core.Location;
import com.yimei.routing.core.TransferForSearch;
import com.yimei.routing.core.TripEvent;
import com.yimei.routing.csa.CsaStop;

/***
 * The model of RAPTOR: 
 * @author e04499
 *
 */

public class RaptorModel extends ModelBuilder {
	
	private Map<String, RaptorStop> stops; // the key is the stop id
	private Map<String, RaptorRoute> routes; // the key is the route id
	private Map<String, RaptorTrip> trips; // the key is the trip id
	private Map<String, List<FootPath>> stopFootPaths; // the foot paths of each stop
	
	/* constructor */
	
	public RaptorModel() {
		stops = Maps.newHashMap();
		routes = Maps.newHashMap();
		trips = Maps.newHashMap();
		stopFootPaths = Maps.newHashMap();
	}
	

	/* methods */
	
	public Map<String, RaptorStop> getStops() {
		return stops;
	}
	
	public Map<String, RaptorRoute> getRoutes() {
		return routes;
	}
	
	public Map<String, RaptorTrip> getTrips() {
		return trips;
	}
	
	public Map<String, List<FootPath>> getStopFootPaths() {
		return stopFootPaths;
	}
	
	public List<FootPath> getFootPathOfStop(String stopId) {
		return stopFootPaths.get(stopId);
	}
	
	public RaptorStop getStop(String id) {
		return stops.get(id);
	}
	
	public RaptorRoute getRoute(String id) {
		return routes.get(id);
	}
	
	public RaptorTrip getTrip(String id) {
		return trips.get(id);
	}
	
	public void addStop(RaptorStop stop) {
		stops.put(stop.getId(), stop);
	}
	
	public void addRoute(RaptorRoute route) {
		routes.put(route.getId(), route);
	}
	
	public void addTrip(RaptorTrip trip) {
		trips.put(trip.getId(), trip);
	}
	
	public void addEventToTrip(String tripId, TripEvent te) {
		trips.get(tripId).addTripEvent(te);
	}
	
	public void addTripToRoute(String routeId, String tripId) {
		routes.get(routeId).addTripId(tripId);
	}
	
	public void addStopFootPaths(String stopId, List<FootPath> footPaths) {
		stopFootPaths.put(stopId, footPaths);
	}
	
	// the route id of the stop sequence 
	public String routeIdOfTripSequence (RaptorTrip tfs, String routeId) {
		String id = null;

		int currNum = 0;
		String currRouteId = routeId + "-" + currNum;
		while (routes.containsKey(currRouteId)) {
			if (routes.get(currRouteId).containsTripSequence(tfs)) {
				id = currRouteId;
				break;
			}
			
			currNum ++;
			currRouteId = routeId + "-" + currNum;
		}
		
		return id;
	}
	
	public String routeOfTrip(String tid) {
		return trips.get(tid).getRouteId();
	}
	
	public int arrivalTimeOfStopInTrip(String stopId, String tripId, String routeId) {
		int index = routes.get(routeId).getStopSequence().indexOf(stopId);
		return trips.get(tripId).getArrivalTimeOfStop(index);
	}
	
	public int departureTimeOfStopInTrip(String stopId, String tripId, String routeId) {
		int index = routes.get(routeId).getStopSequence().indexOf(stopId);
		return trips.get(tripId).getDepartureTimeOfStop(index);
	}
	
	// the trip starts from fromIndex
	public int arrivalTimeOfStopInTrip(String stopId, String tripId, String routeId, int fromIndex) {
		int index = routes.get(routeId).getStopSubSequence(fromIndex).indexOf(stopId);
		index += fromIndex;
		return trips.get(tripId).getArrivalTimeOfStop(index);
	}
	
	public int departureTimeOfStopInTrip(String stopId, String tripId, String routeId, int fromIndex) {
		int index = routes.get(routeId).getStopSubSequence(fromIndex).indexOf(stopId);
		index += fromIndex;
		return trips.get(tripId).getDepartureTimeOfStop(index);
	}
	
	
	// sort the trips from earliest to latest
	public void sortRouteTrips(String routeId) {
		tripQuickSort(routes.get(routeId).getTrips(), 0, routes.get(routeId).getTrips().size()-1);
	}
	
	private void tripQuickSort(ArrayList<String> tripList, int low, int high) {
		int i = low, j = high;
	    // Get the pivot element from the middle of the list
		
	    int pivot = trips.get(tripList.get(low + (high-low)/2)).getArrivalTimeOfStop(0);

	    // Divide into two lists
	    while (i <= j) {
	    	// If the current value from the left list is smaller then the pivot
	    	// element then get the next element from the left list
	    	while (trips.get(tripList.get(i)).getArrivalTimeOfStop(0) < pivot) {
	    		i++;
	    	}	
	    	// If the current value from the right list is larger then the pivot
	    	// element then get the next element from the right list
	    	while (trips.get(tripList.get(j)).getArrivalTimeOfStop(0) > pivot) {
	    		j--;
	    	}

	    	// If we have found a values in the left list which is larger then
	    	// the pivot element and if we have found a value in the right list
	    	// which is smaller then the pivot element then we exchange the
	    	// values.
	    	// As we are done we can increase i and j
	    	if (i <= j) {
	    		exchange(tripList, i, j);
	    		i++;
	    		j--;
	    	}
	    }
	    // Recursion
	    if (low < j) {
	    	tripQuickSort(tripList, low, j);
	    }
	      
	    if (i < high) {
	    	tripQuickSort(tripList, i, high);
	    }
	}
	
	private void exchange(ArrayList<String> list, int i, int j) {
		String temp = list.get(i);
		list.set(i, list.get(j));
		list.set(j, temp);
	}
	
	
	public static RaptorModel fromGTFSFeed(GTFSFeed feed) {
		RaptorModel model = new RaptorModel();
		
		// construct all the stops by reading GTFS - stops
		for (Stop s : feed.stops.values()) {
			RaptorStop sfs = new RaptorStop(s.stop_id, s.stop_lat, s.stop_lon);
			model.addStop(sfs);
		}
		
		// construct all the trips by reading GTFS trips and stop_times
		for (Trip t : feed.trips.values()) {
			
			RaptorTrip tfs = new RaptorTrip(t.trip_id, t.route_id);
			
			// find all the serving days of this trip
			tfs.createWorkingDaysFromGTFSFeed(feed);
			
//			System.out.print("Trip " + tfs.getId() + " works on ");
//			for (String str : tfs.getWorkingDays()) {
//				System.out.print(str + ", ");
//			}
//			System.out.println("");
			
			// add this trip to the model
			model.addTrip(tfs);
			
			// get the submap of this trip in stop_times
			Map<Tuple2, StopTime> tripStopTimes = 
					feed.stop_times.subMap(Fun.t2(t.trip_id, null), Fun.t2(t.trip_id, Fun.HI));

//			System.out.println("Sequence for trip " + t.trip_id);
//			for (StopTime st : tripStopTimes.values()) {
//				System.out.print(st.stop_id + " ");
//			}
//			System.out.println("");
			
			for (StopTime st : tripStopTimes.values()) {
				TripEvent te = new TripEvent(st.stop_id, st.arrival_time, st.departure_time);
				model.addEventToTrip(st.trip_id, te);
			}
		}
		
		// the number of different patterns found in each route
		Map<String, Integer> numOfTripsInRoute = Maps.newHashMap();
		for (Route r : feed.routes.values()) {
			numOfTripsInRoute.put(r.route_id, 0);
		}
		
		// update stops and routes for search based on trips
		for (RaptorTrip tfs : model.getTrips().values()) {
			// get the route id of the trip
			String baseRouteId = tfs.getRouteId();
			int currNumOfTrips = numOfTripsInRoute.get(baseRouteId);
			
//			System.out.println("Trip " + tfs.getId() + ", base route " + baseRouteId);
			String currRouteId = model.routeIdOfTripSequence(tfs, baseRouteId);
//			System.out.println("Found: " + currRouteId);
			if (currRouteId == null) {
				// create a new route with this trip sequence
				String tmpRouteId = baseRouteId + "-" + currNumOfTrips;
				tfs.setRouteId(tmpRouteId);
				RaptorRoute rfs = RaptorRoute.createWithTripSequence(tfs, tmpRouteId);
				model.addRoute(rfs);
				numOfTripsInRoute.replace(baseRouteId, currNumOfTrips+1);
				
//				System.out.println("Adding route " + rfs.getId());
//				for (int i = 0; i < rfs.getStopSequence().size(); i++) {
//					System.out.print(rfs.getStopId(i) + " ");
//				}
//				System.out.println("");
			}
			else
			{
				tfs.setRouteId(currRouteId);
				model.getRoute(currRouteId).addTripId(tfs.getId());
			}
		}
		
		// sort the trips from earliest to latest for each route
		for (RaptorRoute rfs : model.getRoutes().values()) {
			model.sortRouteTrips(rfs.getId());
		}
		
//		for (RaptorRoute rfs : model.getRoutes().values()) {
//			System.out.println("Trips for route " + rfs.getId());
//			for (String t : rfs.getTrips()) {
//				System.out.print(t + " (" + model.getTrip(t).getArrivalTimeOfStop(0) + ") ");
//			}
//			System.out.println("");
//		}
		
//		for (RouteForSearch rfs : model.getRoutes().values()) {
//			System.out.println("Stop sequence for route " + rfs.getId());
//			for (String s : rfs.getStopSequence()) {
//				System.out.print(s + " ");
//			}
//			System.out.println("");
//			System.out.println("Trips for route " + rfs.getId());
//			for (String t : rfs.getTrips()) {
//				System.out.print(t + " ");
//			}
//			System.out.println("");
//		}
		
		// add the routes belonging to each stop
		for (RaptorRoute rfs : model.getRoutes().values()) {
			for (int i = 0; i < rfs.getStopSequence().size(); i++) {
				String sid = rfs.getStopId(i);
				model.getStop(sid).addRouteIdx(rfs.getId(), i);
			}
		}
		
//		for (StopForSearch sfs : model.getStops().values()) {
//			System.out.println("Routes serving stop " + sfs.getId());
//			for (String r : sfs.getRoutes()) {
//				System.out.print(r + " ");
//			}
//			System.out.println("");
//		}
		
		// add foot-paths of stops
		for (RaptorStop raptorStop1 : model.getStops().values()) {
			// create the list of foot-paths of this stop
			List<FootPath> fpList = new ArrayList<FootPath>();
			
			// Stop of s1
			Stop s1 = feed.stops.get(raptorStop1.getId());
			
			for (RaptorStop raptorStop2 : model.getStops().values()) {
				if (raptorStop1.equals(raptorStop2)) {
					continue;
				}
				
				// Stop of s2
				Stop s2 = feed.stops.get(raptorStop2.getId());
				
				double distance = Stop.distanceBetween(s1, s2);
				
//				System.out.println("Distance between " + s1.stop_id + " and " + s2.stop_id + " is " + distance);
				
				if (distance < ModelBuilder.walkingThreshold) {
					
					// check whether s1 and s2 are in the same route. If so, then don't walk
					Collection<String> routeSet1 = raptorStop1.getRoutes();
					Collection<String> routeSet2 = raptorStop2.getRoutes();
					boolean overlapped = false;
					for (String r1 : routeSet1) {
						for (String r2 : routeSet2) {
							if (r1.equals(r2)) {
								overlapped = true;
								break;
							}
						}
					}
					
					if (overlapped) {
						continue;
					}
					
//					System.out.print("s1 belongs to routes ");
//					for (String r1 : routeSet1) {
//						System.out.print(r1 + " ");
//					}
//					System.out.println("");
//					System.out.print("s2 belongs to routes ");
//					for (String r2 : routeSet2) {
//						System.out.print(r2 + " ");
//					}
//					System.out.println("");
					
					
					FootPath fp = new FootPath(raptorStop2.getId(), distance);
					fpList.add(fp);
				}
			}
			
			// sort the foot-paths in ascending order of walking distance
			Collections.sort(fpList);
			
			// check if there are stops that are closer and whose route set contains that of the current one
			for (int i = fpList.size()-1; i > -1; i--) {
				Collection<String> routeSet1 = model.getStop(fpList.get(i).getToStopId()).getRoutes();
				
				for (int j = 0; j < i; j++) {
					Collection<String> routeSet2 = model.getStop(fpList.get(j).getToStopId()).getRoutes();
					
					if (routeSet2.containsAll(routeSet1)) {
						fpList.remove(i);
						break;
					}
				}
			}
			
			model.addStopFootPaths(raptorStop1.getId(), fpList);
			
//			for (FootPath fp : fpList) {
//				System.out.println("neighbor stop " + fp.getToStopId());
//				for (String rid : model.getStop(fp.getToStopId()).getRoutes()){
//					System.out.print(rid + " ");
//				}
//				System.out.println("");
//			}
//			new java.util.Scanner(System.in).nextLine();
		}
		
		// get the stop ids with the same name
		for (RaptorStop raptorStop1 : model.getStops().values()) {
			
			Stop s1 = feed.stops.get(raptorStop1.getId());
			
			for (RaptorStop raptorStop2 : model.getStops().values()) {
				if (raptorStop1.equals(raptorStop2)) {
					continue;
				}
				
				Stop s2 = feed.stops.get(raptorStop2.getId());
				
				if (s1.stop_name.equals(s2.stop_name)) {
					
					// check whether they are close to each other
					double distance = Stop.distanceBetween(s1, s2);
					
					if (distance < 0.1) { // less than 100m
						// add ids with the same name
						raptorStop1.getSameNameIds().add(raptorStop2.getId());
					}
				}
			}
		}
		
//		for (StopForSearch sfs : model.getStops().values()) {
//			System.out.println("Waling stops of stop " + sfs.getId());
//			for (TransferForSearch wt : sfs.getWalkingTransfers()) {
//				System.out.print(wt.toStopId + " ");
//			}
//			System.out.println("");
//		}
		
		return model;
	}
	
	
	public List<FootPath> searchFootPaths(RaptorStop stop) {
		
		List<FootPath> fpList = new ArrayList<FootPath>();
		
		for (RaptorStop rs : stops.values()) {
			double distance = stop.getLocation().distanceTo(rs.getLocation());
			
//			if (rs.getId().equals("101357")) {
//				System.out.println("(" + stop.getLocation().lat + ", " + stop.getLocation().lon + ")");
//				System.out.println("(" + rs.getLocation().lat + ", " + rs.getLocation().lon + ")");
//				System.out.println("distance = " + distance);
//			}
			
			if (distance < walkingThreshold) {
				
				// check whether s1 and s2 are in the same route. If so, then don't walk
				boolean overlapped = false;
				for (String r1 : stop.getRoutes()) {
					for (String r2 : rs.getRoutes()) {
						if (r1.equals(r2)) {
							overlapped = true;
							break;
						}
					}
				}
				
				if (overlapped) {
					continue;
				}
				
//				System.out.print("s1 belongs to routes ");
//				for (String r1 : routeSet1) {
//					System.out.print(r1 + " ");
//				}
//				System.out.println("");
//				System.out.print("s2 belongs to routes ");
//				for (String r2 : routeSet2) {
//					System.out.print(r2 + " ");
//				}
//				System.out.println("");
				
				
				FootPath fp = new FootPath(rs.getId(), distance);
				fpList.add(fp);
			}
		}
		
		// sort the foot-paths in ascending order of walking distance
		Collections.sort(fpList);
		
		// check if there are stops that are closer and whose route set contains that of the current one
		for (int i = fpList.size()-1; i > -1; i--) {
			Collection<String> routeSet1 = stops.get(fpList.get(i).getToStopId()).getRoutes();
			
			for (int j = 0; j < i; j++) {
				Collection<String> routeSet2 = stops.get(fpList.get(j).getToStopId()).getRoutes();
				
				if (routeSet2.containsAll(routeSet1)) {
					fpList.remove(i);
					break;
				}
			}
		}
		
		return fpList;
	}
	
	public void toJSON(String label) {
		// set directory
		String dirStr = "data/JSON/" + label;
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
			FileWriter writer = new FileWriter("data/JSON/" + label + "/RaptorModel.json");
			writer.write(json);
			writer.close();
		} catch (IOException e) {
	  		e.printStackTrace();
	  	}
	}
	
	public static RaptorModel fromJSON(File jsonFile) {
		RaptorModel model = new RaptorModel();
		
		Gson gson = new Gson();
		try {
	 
			BufferedReader br = new BufferedReader(
				new FileReader(jsonFile));
	 
			//convert the json string back to object
			model = gson.fromJson(br, RaptorModel.class);
	 
//			System.out.println(model);
	 
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return model;
	}
	
	public void serialize(String label) throws FileNotFoundException {
		// set directory
		String dirStr = "data/JSON/" + label;
		File directory = new File(dirStr);
		if (!directory.exists()) {
			try{
				directory.mkdir();
		    } catch(SecurityException se){
			   //handle it
			}        
		}
		
		Kryo kryo = new Kryo();
		Output output = new Output(new FileOutputStream(directory + "/RaptorModel.bin"));
		
		kryo.writeObject(output, this);
	    output.close();
	}
	
	public static RaptorModel deserialize(String fileName) throws FileNotFoundException {
		
		Kryo kryo = new Kryo();
		Input input = new Input(new FileInputStream(fileName));
		
		RaptorModel model = kryo.readObject(input, RaptorModel.class);
	    input.close();
	    
	    return model;
	}
	
	// main for testing
	public static void main (String[] args) throws FileNotFoundException {
		
		/*** create the model file ***/
		
		String INPUT = "data/GTFS/Adelaide.zip";
		GTFSFeed feed = GTFSFeed.fromFile(INPUT);
		RaptorModel model = RaptorModel.fromGTFSFeed(feed);
		model.toJSON("Adelaide");
//		model.serialize("Adelaide");
		
		/*** load the model file ***/
        
//		long tStart = System.currentTimeMillis();
//		
//		File file = new File("data/JSON/Adelaide/RaptorModel.JSON");
//		RaptorModel model = RaptorModel.fromJSON(file);
//		
//		long tEnd = System.currentTimeMillis();
//		long tDelta = tEnd - tStart;
//		double elapsedSeconds = tDelta / 1000.0;
//		
//		System.out.println(elapsedSeconds);
//		
//		for (RaptorStop rs : model.getStops().values()) {
//			if (rs.getSameNameIds().isEmpty()) {
//				continue;
//			}
//			
//			System.out.print("stop " + rs.getId() + " has the same name as ");
//			for (String id : rs.getSameNameIds()) {
//				System.out.print(id + " ");
//			}
//			System.out.println("");
//		}
		
//		for (RaptorStop rs : model.getStops().values()) {
//			
//			for (String routeId : rs.getRoutes()) {
//				if (rs.getIdxOfRoute(routeId).size() > 1) {
//				System.out.println("For stop " + rs.getId());
//				System.out.print("in route " + routeId + " idx = (");
//				for (int i : rs.getIdxOfRoute(routeId)) {
//					System.out.print(i + " ");
//				}
//				System.out.println(")");
//				}
//			}
//		}
		
//		for (RaptorRoute rr : model.getRoutes().values()) {
//			System.out.println("Trips for route " + rr.getId());
//			for (String t : rr.getTrips()) {
//				System.out.print(t + " (" + model.getTrip(t).getArrivalTimeOfStop(0) + ") ");
//			}
//			System.out.println("");
//		}
		
	}
}

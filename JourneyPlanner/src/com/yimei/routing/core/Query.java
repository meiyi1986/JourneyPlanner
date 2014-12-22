package com.yimei.routing.core;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.yimei.modelbuilder.ModelBuilder;
import com.yimei.routing.raptor.RaptorModel;
import com.yimei.routing.raptor.RaptorStop;

/***
 * The query of the journey planner
 * @author e04499
 *
 */

public class Query {

	protected String fromStopId;
	protected String toStopId;
	protected QueryTime now;
	protected int maxNumOfTrips;
	protected double maxWalkingDistance;
	protected double walkingSpeed;
	
	public Query(String fromId, String toId, String day, int now) {
		this.fromStopId = fromId;
		this.toStopId = toId;
		this.now = new QueryTime(now, day, 0);
		this.maxNumOfTrips = ModelBuilder.defaultMaxNumOfTrips;
		this.maxWalkingDistance = ModelBuilder.defaultMaxWalkingDistance;
		this.walkingSpeed = ModelBuilder.defaultWalkingSpeed;
	}
	
	public Query(String fromId, String toId, QueryTime now) {
		this.fromStopId = fromId;
		this.toStopId = toId;
		this.now = now;
		this.maxNumOfTrips = ModelBuilder.defaultMaxNumOfTrips;
		this.maxWalkingDistance = ModelBuilder.defaultMaxWalkingDistance;
		this.walkingSpeed = ModelBuilder.defaultWalkingSpeed;
	}
	
	public Query(String fromId, String toId, QueryTime now, double maxWD, double ws) {
		this.fromStopId = fromId;
		this.toStopId = toId;
		this.now = now;
		this.maxWalkingDistance = maxWD;
		this.walkingSpeed = ws;
	}
	
	public String getFromStopId() {
		return fromStopId;
	}
	
	public String getToStopId() {
		return toStopId;
	}
	
	public QueryTime getNow() {
		return now;
	}
	
	public String getDay() {
		return now.day;
	}
	
	public int getTime() {
		return now.time;
	}
	
	public int getOvernights() {
		return now.overnights;
	}
	
	public int getMaxNumOfTrips() {
		return maxNumOfTrips;
	}
	
	public double getMaxWalkingDistance() {
		return maxWalkingDistance;
	}
	
	public double getWalkingSpeed() {
		return walkingSpeed;
	}
	
	public void setNow(QueryTime now) {
		this.now = now;
	}
	
	
	
	public static List<Query> randomGenerate(int n, RaptorModel model) {
		List<Query> queries = new ArrayList<Query>();
		
		List<RaptorStop> stops = new ArrayList<RaptorStop>(model.getStops().values());
		List<String> days = new ArrayList<String>(
				Arrays.asList("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"));
		
		for (int i = 0; i < n; i++) {
			Random rd = new Random();
			
			// randomly generate source and target stops
			int id1 = rd.nextInt(model.getStops().size());
			int id2 = rd.nextInt(model.getStops().size());
			while (id2 == id1) {
				id2 = rd.nextInt(model.getStops().size());
			}
			
			// randomly generate day
			int id = rd.nextInt(7);
			String day = days.get(id);
			
			// randomly generate now
			int now = rd.nextInt(ModelBuilder.secondsPerDay);
			
			Query query = new Query(stops.get(id1).getId(), stops.get(id2).getId(), new QueryTime(now, day, 0));
			queries.add(query);
		}
		
		return queries;
	}
	
	public static void toJSON(List<? extends Query> queries, String dirStr) {
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
		String json = gson.toJson(queries);
//	      System.out.println(json);
			
		try {
			//write converted json data to a file named "file.json"
			FileWriter writer = new FileWriter(dirStr + "/Queries.json");
			writer.write(json);
			writer.close();
		} catch (IOException e) {
	  		e.printStackTrace();
	  	}
	}
	
	public static List<Query> readQueryFromJSON(String jsonFile) {
		List<Query> queries = new ArrayList<Query>();
		
		Gson gson = new Gson();
		try {
	 
			BufferedReader br = new BufferedReader(
				new FileReader(jsonFile));
	 
			Type typeOfQueryList = new TypeToken<List<Query>>(){}.getType();
			//convert the json string back to object
			queries = gson.fromJson(br, typeOfQueryList);
	 
//			System.out.println(model);
	 
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return queries;
	}
	
	public void printMe() {
		System.out.print("Query: from stop " + fromStopId + " to stop " + toStopId + " at ");
		now.printIt();
	}
	
	public static void main(String[] args) {
		File file = new File("data/JSON/Adelaide/RaptorModel.json");
		RaptorModel model = RaptorModel.fromJSON(file);
		List<Query> queries = Query.randomGenerate(100, model);
		
		Query.toJSON(queries, "data/JSON/Adelaide");
	}
}

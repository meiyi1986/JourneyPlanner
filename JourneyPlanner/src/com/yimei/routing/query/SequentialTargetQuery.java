package com.yimei.routing.query;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
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
import com.yimei.util.TimeTransformer;

public class SequentialTargetQuery extends Query {

	List<String> targetSequence;
	
	public SequentialTargetQuery(String fromId, String toId, String day, int now, List<String> ts) {
		super(fromId, toId, new QueryTime(now, day, 0));
		this.targetSequence = ts;
	}
	
	public SequentialTargetQuery(String fromId, String toId, String day, int now, double maxWD, double ws, List<String> ts) {
		super(fromId, toId, new QueryTime(now, day, 0), maxWD, ws);
		this.targetSequence = ts;
	}
	
	public List<String> getTargetSequence() {
		return targetSequence;
	}
	
	public String getTarget(int index) {
		return targetSequence.get(index);
	}
	
	public void setTargetSequence(List<String> ts) {
		this.targetSequence = ts;
	}
	
	// n = number of queries, m = sequence size
	public static List<SequentialTargetQuery> randomGenerate(int n, int m, RaptorModel model) {
		List<SequentialTargetQuery> queries = new ArrayList<SequentialTargetQuery>();
		
		List<RaptorStop> stops = new ArrayList<RaptorStop>(model.getStops().values());
//		List<String> days = new ArrayList<String>(
//				Arrays.asList("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"));
		List<String> days = new ArrayList<String>(
				Arrays.asList("Monday", "Tuesday", "Wednesday", "Thursday", "Friday"));
		
		List<Integer> sampledIds = new ArrayList<Integer>();
		
		for (int i = 0; i < n; i++) {
			Random rd = new Random();
			
			// randomly generate source and target stops
			int sourceId = rd.nextInt(model.getStops().size());
			sampledIds.add(new Integer(sourceId));
			
			List<String> ts = new ArrayList<String>();
			for (int j = 0; j < m; j++) {
				int targetId;
				do {
					targetId = rd.nextInt(model.getStops().size());
				} while (sampledIds.contains(new Integer(targetId)));
				ts.add(stops.get(targetId).getId());
				sampledIds.add(new Integer(sourceId));
			}
			
			// randomly generate day
			int id = rd.nextInt(days.size());
			String day = days.get(id);
			
			// randomly generate now
			int now = rd.nextInt(ModelBuilder.secondsPerDay);
			
			SequentialTargetQuery query = new SequentialTargetQuery(stops.get(sourceId).getId(), null, day, now, ts);
			queries.add(query);
		}
		
		return queries;
	}
	
	public static List<SequentialTargetQuery> readSequentialTargetQueryFromJSON(String jsonFile) {
		List<SequentialTargetQuery> queries = new ArrayList<SequentialTargetQuery>();
		
		Gson gson = new Gson();
		try {
	 
			BufferedReader br = new BufferedReader(
				new FileReader(jsonFile));
	 
			Type typeOfQueryList = new TypeToken<List<SequentialTargetQuery>>(){}.getType();
			//convert the json string back to object
			queries = gson.fromJson(br, typeOfQueryList);
	 
//			System.out.println(model);
	 
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return queries;
	}
	
	
	
	public void printMe() {
		System.out.print("Query from stop " + fromStopId + " to (");
		for (String id : targetSequence) {
			System.out.print(id + " ");
		}
		System.out.println(") on " + now.day + " at " + TimeTransformer.IntegerToString(now.time));
	}
	
	
	public void printMe(PrintWriter writer) {
		writer.print("Query from stop " + fromStopId + " to (");
		for (String id : targetSequence) {
			writer.print(id + " ");
		}
		writer.println(") on " + now.day + " at " + TimeTransformer.IntegerToString(now.time));
	}
	
	public static void main(String[] args) {
		File file = new File("data/JSON/Adelaide/RaptorModel.json");
		RaptorModel model = RaptorModel.fromJSON(file);
		List<SequentialTargetQuery> queries = SequentialTargetQuery.randomGenerate(100, 5, model);
		
		SequentialTargetQuery.toJSON(queries, "data/JSON/Adelaide");
	}
}

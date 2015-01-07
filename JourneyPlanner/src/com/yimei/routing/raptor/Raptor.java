package com.yimei.routing.raptor;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.mapdb.Fun.Tuple2;

import com.conveyal.gtfs.model.Calendar;
import com.google.common.collect.Maps;
import com.yimei.modelbuilder.ModelBuilder;
import com.yimei.routing.core.FootPath;
import com.yimei.routing.core.Label;
import com.yimei.routing.core.Location;
import com.yimei.routing.journey.Journey;
import com.yimei.routing.journey.JourneyEvent;
import com.yimei.routing.query.LocationQuery;
import com.yimei.routing.query.Query;
import com.yimei.routing.query.QueryTime;

/***
 * The RAPTOR algorithm proposed in the paper:
 * D. Delling, T. Pajor, R.F. Werneck, "Round-Based Public Transit Routing", In Workshop on Algorithms Engineering and Experiments,
 * pp. 130-140, 2012.
 * @author e04499
 *
 */

public class Raptor {
	
	private final RaptorModel model; // model cannot be changed
	private List<Map<String, Label>> roundStopLabels;
	private Map<String, Label> eaStopLabels; // earliest arrival labels
	private Map<String, Label> ldStopLabels; // latest departure labels
	
	
	// measurse
	private int numOfRelaxedRoutes;
	private int numOfRelaxedStops;
	private int numOfLabelComparisons;
	private int numOfScannedFootPaths;
	
	public void printMeasures() {
		System.out.println(roundStopLabels.size() + " rounds, relaxed " + numOfRelaxedRoutes + " routes, " + numOfRelaxedStops + " stops, and "
				+ numOfLabelComparisons + " label comparisons, scanned " + numOfScannedFootPaths + " foot paths.");
	}
	
	public Raptor(RaptorModel model) {
		this.model = model;
	}
	
	public Journey earliestArrivalJourney(Query query) {
		numOfRelaxedRoutes = 0;
		numOfRelaxedStops = 0;
		numOfLabelComparisons = 0;
		
		/*** initialization ***/
		
		roundStopLabels = new ArrayList<Map<String, Label>>();
		
		Map<String, Label> stopLabels0 = Maps.newHashMap();
		for (RaptorStop raptStop : model.getStops().values()) {
			Label label = new Label(raptStop.getId(), null, null, query.getNow(), QueryTime.MAX_VALUE(query.getNow().day), 0, null);
			stopLabels0.put(raptStop.getId(), label);
		}
		Label fromLabel = new Label(query.getFromStopId(), null, null, query.getNow(), query.getNow(), 0, null);
		stopLabels0.put(query.getFromStopId(), fromLabel);
		
		// mark list
		Map<String, Boolean> marked = Maps.newHashMap();
		for (RaptorStop raptStop : model.getStops().values()) {
			marked.put(raptStop.getId(), false);
		}
		
		// the list of marked stops
		List<String> markedStops = new ArrayList<String>();
		
		markedStops.add(query.getFromStopId());
		marked.put(query.getFromStopId(), true);
		
		// mark all the stops within the walking distance of the source stop since their arrival times are updated
		for (FootPath fp : model.getFootPathOfStop(query.getFromStopId())) {
			numOfScannedFootPaths ++;
			
			if (fp.getWalkingDistance() > ModelBuilder.defaultMaxTailWalkingDistance) { // the foot-path is too long
				break;
			}
			
			// compute the walking time with the walking speed
			double walkingTime = 1.0*fp.getWalkingDistance()/query.getWalkingSpeed();
			
			QueryTime walkingAqt = new QueryTime(query.getNow());
			walkingAqt.moveForward((int) walkingTime);
			
			Label walkingLabel = new Label(fp.getToStopId(), query.getFromStopId(), "walking",
					query.getNow(), walkingAqt, 0, fromLabel);
			
//			walkingLabel.printMe();
			
			stopLabels0.put(fp.getToStopId(), walkingLabel);
			markedStops.add(fp.getToStopId());
			marked.put(fp.getToStopId(), true);
		}
	
		roundStopLabels.add(stopLabels0);
		
		// earlist arrival label list, initialized to be the labelList0
		eaStopLabels = Maps.newHashMap(stopLabels0);
		
		/*** search ***/
		
		int k = 1; // round: number of trips in the journey
		
		while(!markedStops.isEmpty()) {
			Map<String, Label> stopLabelsk = Maps.newHashMap(roundStopLabels.get(k-1));
			
			System.out.println("k = " + k + ", number of marked stops = " + markedStops.size() + "/" + model.getStops().size());
			
			// create a new empty Q, and put the pairs (route id, earliest marked stop) into Q
			Map<String, String> Q = Maps.newHashMap();
			for (String sid : markedStops) {
				marked.put(sid, false);
				
//				System.out.println("stop = " + sid);
				for (String rid : model.getStop(sid).getRoutes()) { // for each route serving this marked stop
					if (Q.containsKey(rid)) { // this route has already been in Q
						int index1 = model.getStop(sid).getIdxOfRoute(rid).get(0);
						int index2 = model.getStop(Q.get(rid)).getIdxOfRoute(rid).get(0);
						
						if (index1 < index2) { // the current stop appears earlier than the previous one in the route, replace
							
							Q.replace(rid, sid);
						}
					}
					else { // the route is new to Q, add it into Q
						Q.put(rid, sid);
					}
				}
			}
			
			// empty the marked stops
			markedStops.clear();
			
//			for (Map.Entry<String, String> entry : Q.entrySet()) {
//				System.out.print("(" + entry.getKey() + ", " + entry.getValue() + ") ");
//			}
//			System.out.println("");
			
			// traverse each route
			for (String rid : Q.keySet()) {
				numOfRelaxedRoutes ++;
				
				// fromIndex is the starting index of the sequence of the route: the index of the corresponding stop in Q
				int fromIndex = model.getRoute(rid).getStopSequence().indexOf(Q.get(rid));
				
				Map<String, Boolean> scanned = Maps.newHashMap();
				for (int i = fromIndex; i < model.getRoute(rid).getStopSequence().size(); i++) {
					String sid = model.getRoute(rid).getStopId(i);
					scanned.put(sid, false);
				}
				
				String currTrip = null;
				String boardingStop = null;
				QueryTime boardingDqt = null;
				
//				System.out.println("route " + rid + ": starting from stop " + subseq.get(0));
				for (int i = fromIndex; i < model.getRoute(rid).getStopSequence().size(); i++) {
					
					numOfRelaxedStops ++;
					
					String sid = model.getRoute(rid).getStopId(i);
					if (scanned.get(sid)) {
						// its arrival time has been updated before
						
						// re-calculate the earliest trip, which must be no later than the current one
						QueryTime stopAqt = roundStopLabels.get(k-1).get(sid).getArrivalTime();
						Tuple2<String, QueryTime> earliestTripInfo = earliestRouteDepartureAtStop(rid, sid, i, stopAqt);
						currTrip = earliestTripInfo.a;
						boardingStop = sid;
						boardingDqt = earliestTripInfo.b;
						
						continue;
					}
					
					scanned.put(sid, true);
					
//					System.out.print(sid + " ");
					
					if (currTrip != null) {
						// update the arrival time of the kth round and best known
						
						QueryTime bkAqt1 = eaStopLabels.get(sid).getArrivalTime();
						QueryTime bkAqt2 = eaStopLabels.get(query.getToStopId()).getArrivalTime();
						
						int tmpAt = model.getTrip(currTrip).getEvent(i).getArrivalTime();
						QueryTime tmpAqt = new QueryTime(tmpAt, boardingDqt.day, boardingDqt.overnights);
						
						numOfLabelComparisons ++;
						
						if (tmpAqt.earlierThan(bkAqt1) && tmpAqt.earlierThan(bkAqt2)) {
							
							// update the label and arrival times
							
							Label label = new Label(sid, boardingStop, currTrip, boardingDqt, tmpAqt, 
									k, roundStopLabels.get(k-1).get(boardingStop));
							
//							if (sid.equals("101485")) {
//							System.out.println("old label");
//							eaStopLabels.get(sid).printMe();
//							System.out.println("new label");
//							label.printMe();
////							new java.util.Scanner(System.in).nextLine();
//							}
							
							stopLabelsk.put(sid, label);
							eaStopLabels.put(sid, label);
							if (!marked.get(sid)) {
								markedStops.add(sid);
								marked.put(sid, true);
							}
							
							
							// check if target can be walked from the stop 
							double walkingDistance = model.getStop(sid).getLocation()
									.distanceTo(model.getStop(query.getToStopId()).getLocation());
							
							
							
							if (walkingDistance <= ModelBuilder.defaultMaxTailWalkingDistance) {
								double walkingTime = walkingDistance/query.getWalkingSpeed();
								// calculate the new arrival time of the walk
								QueryTime walkingAqt = new QueryTime(tmpAqt);
								walkingAqt.moveForward((int) walkingTime);
								
								if (walkingAqt.earlierThan(eaStopLabels.get(query.getToStopId()).getArrivalTime())) {
									Label walkingLabel = new Label(query.getToStopId(), sid, "walking", 
											tmpAqt, walkingAqt, k, label);
									stopLabelsk.put(query.getToStopId(), walkingLabel);
									eaStopLabels.put(query.getToStopId(), walkingLabel);
								}
							}
						}
					}
					
					if (currTrip == null) {
						// create the current trip as the earliest trip of the current stop
						QueryTime stopAqt = roundStopLabels.get(k-1).get(sid).getArrivalTime();
						Tuple2<String, QueryTime> earliestTripInfo = earliestRouteDepartureAtStop(rid, sid, i, stopAqt);
						currTrip = earliestTripInfo.a;
						boardingStop = sid;
						boardingDqt = earliestTripInfo.b;
					}
					else {
						int tmpDt = model.getTrip(currTrip).getEvent(i).getDepartureTime();
						QueryTime tmpDqt = new QueryTime(tmpDt, boardingDqt.day, boardingDqt.overnights);
						
						QueryTime lastAqt = new QueryTime(roundStopLabels.get(k-1).get(sid).getArrivalTime());
						if (lastAqt.differenceFrom(tmpDqt) < ModelBuilder.changeTime) {
							// lastAqt + changeTime < tmpDqt
							// calculate the earliest trip based on the current stop
							QueryTime stopAqt = roundStopLabels.get(k-1).get(sid).getArrivalTime();
							Tuple2<String, QueryTime> earliestTripInfo = earliestRouteDepartureAtStop(rid, sid, i, stopAqt);
							if (!earliestTripInfo.a.equals(currTrip)) {
								currTrip = earliestTripInfo.a;
								boardingStop = sid;
								boardingDqt = earliestTripInfo.b;
							}
						}
					}
					
//					stopLabelsk.get(sid).printMe();
				}
//				System.out.println("");
			}
			

			// update the walking marked stops
			Map<String, Label> walkingStopLabels = Maps.newHashMap();
			for (String sid : markedStops) {
//				System.out.println("walking from stop " + sid);
				for (FootPath fp : model.getFootPathOfStop(sid)) {
					numOfScannedFootPaths ++;
					
					if (roundStopLabels.get(k-1).get(fp.getToStopId()).getTripId() != null) {
						// it's reached by walking, cannot walk any more
						if (roundStopLabels.get(k-1).get(fp.getToStopId()).getTripId().equals("walking")) {
							continue;
						}
					}
					
					if (fp.getWalkingDistance() > query.getMaxWalkingDistance()) { // the foot-path is too long
						break;
					}
					
					// compute the walking time with the walking speed
					double walkingTime = 1.0*fp.getWalkingDistance()/query.getWalkingSpeed();
					
					QueryTime walkingAqt = new QueryTime(stopLabelsk.get(sid).getArrivalTime());
					walkingAqt.moveForward((int) walkingTime);
					
					if (walkingAqt.earlierThan(stopLabelsk.get(fp.getToStopId()).getArrivalTime())) {
						Label walkingLabel = new Label(fp.getToStopId(), sid, "walking", 
								eaStopLabels.get(sid).getArrivalTime(), walkingAqt, k, stopLabelsk.get(sid));
						
//						if (fp.getToStopId().equals("2085")) {
//							roundStopLabels.get(k-1).get(fp.getToStopId()).printMe();
//							walkingLabel.printMe();
//						}
						
//						if (sid.equals("2085")) {
//							roundStopLabels.get(k-1).get(fp.getToStopId()).printMe();
//							walkingLabel.printMe();
//						}
						
						if (walkingStopLabels.containsKey(fp.getToStopId())) {
							// check if the new walking arrival time is earlier
							if (walkingLabel.arrivesEarlierThan(walkingStopLabels.get(fp.getToStopId()))) {
								walkingStopLabels.put(fp.getToStopId(), walkingLabel);
							}
						}
						else {
							walkingStopLabels.put(fp.getToStopId(), walkingLabel);
						}
					}
				}
			}
			
			for (String sid : walkingStopLabels.keySet()) {
				stopLabelsk.put(sid, walkingStopLabels.get(sid));
				eaStopLabels.put(sid, walkingStopLabels.get(sid));
				
				if (!marked.get(sid)) {
					markedStops.add(sid);
					marked.put(sid, true);
				}
			}
			
			roundStopLabels.add(stopLabelsk);
			
//			stopLabelsk.get("3477").printMe();
//			stopLabelsk.get("3485").printMe();
//			stopLabelsk.get("3156").printMe();
			
//			System.out.println("k = " + k);
//			new java.util.Scanner(System.in).nextLine();
			
			k ++;
			
			if (k > 3)
				break;
			
//			System.out.println("k = " + k + ", marked stops: " + markedStops.size());
//			for (String sid : markedStops) {
//				System.out.print(sid + ", ");
//			}
//			System.out.println("");
		}
		
		Journey journey = new Journey();
		
		if (eaStopLabels.get(query.getToStopId()).getPrevId() == null)
			return journey;
		
		// from stopLabels to journey
		
		Label currLabel = eaStopLabels.get(query.getToStopId());
		do {
			JourneyEvent je = new JourneyEvent(currLabel);
			journey.addJourneyEvent(0, je);
			
			currLabel = currLabel.getPreLabel();
//			currLabel = eaStopLabels.get(currLabel.getPrevId());
		} while (!currLabel.getStopId().equals(query.getFromStopId()));
		
		return journey;
	}
	
	
	public Journey earliestArrivalJourney(LocationQuery query) {
		numOfRelaxedRoutes = 0;
		numOfRelaxedStops = 0;
		numOfLabelComparisons = 0;
		
		/*** initialization ***/
		
		roundStopLabels = new ArrayList<Map<String, Label>>();
		
		Map<String, Label> stopLabels0 = Maps.newHashMap();
		for (RaptorStop raptStop : model.getStops().values()) {
			Label label = new Label(raptStop.getId(), null, null, query.getNow(), QueryTime.MAX_VALUE(query.getNow().day), 0, null);
			stopLabels0.put(raptStop.getId(), label);
		}
		// create an artificial label for the source and target locations
		Label fromLocLabel = new Label("source", null, null, query.getNow(), query.getNow(), 0, null);
		stopLabels0.put("source", fromLocLabel);
		Label toLoclabel = new Label("target", null, null, query.getNow(), QueryTime.MAX_VALUE(query.getNow().day), 0, null);
		stopLabels0.put("target", toLoclabel);
		
		// mark list
		Map<String, Boolean> marked = Maps.newHashMap();
		for (RaptorStop raptStop : model.getStops().values()) {
			marked.put(raptStop.getId(), false);
		}
		marked.put("target", false);
		
		// the list of marked stops
		List<String> markedStops = new ArrayList<String>();
		
		// find all the stops within the walking distance of the source location
		RaptorStop sourceStop = new RaptorStop("source", query.getFromLoc().lat, query.getFromLoc().lon);
		List<FootPath> sourceFpList = model.searchFootPaths(sourceStop);
		
		for (FootPath fp : sourceFpList) {
			
			numOfScannedFootPaths ++;
			
			if (fp.getWalkingDistance() > ModelBuilder.defaultMaxTailWalkingDistance) {
				break;
			}
			
			// compute the walking time with the walking speed
			double walkingTime = fp.getWalkingDistance()/query.getWalkingSpeed();
			
			QueryTime walkingAqt = new QueryTime(query.getNow());
			walkingAqt.moveForward((int) walkingTime);
			
			Label walkingLabel = new Label(fp.getToStopId(), "source", "walking",
					query.getNow(), walkingAqt, 0, fromLocLabel);
			
			stopLabels0.put(fp.getToStopId(), walkingLabel);
			markedStops.add(fp.getToStopId());
			marked.put(fp.getToStopId(), true);
		}
	
		roundStopLabels.add(stopLabels0);
		
		// earlist arrival label list, initialized to be the labelList0
		eaStopLabels = Maps.newHashMap(stopLabels0);
		
		/*** search ***/
		
		int k = 1; // round: number of trips in the journey
		
		while(!markedStops.isEmpty()) {
			Map<String, Label> stopLabelsk = Maps.newHashMap(roundStopLabels.get(k-1));
			
			System.out.println("k = " + k + ", number of marked stops = " + markedStops.size() + "/" + model.getStops().size());
			
			// create a new empty Q, and put the pairs (route id, earliest marked stop) into Q
			Map<String, String> Q = Maps.newHashMap();
			for (String sid : markedStops) {
				marked.put(sid, false);
				
//				System.out.println("stop = " + sid);
				for (String rid : model.getStop(sid).getRoutes()) { // for each route serving this marked stop
					if (Q.containsKey(rid)) { // this route has already been in Q
						int index1 = model.getStop(sid).getIdxOfRoute(rid).get(0);
						int index2 = model.getStop(Q.get(rid)).getIdxOfRoute(rid).get(0);
						
						if (index1 < index2) { // the current stop appears earlier than the previous one in the route, replace
							
							Q.replace(rid, sid);
						}
					}
					else { // the route is new to Q, add it into Q
						Q.put(rid, sid);
					}
				}
			}
			
			// empty the marked stops
			markedStops.clear();
			
//			for (Map.Entry<String, String> entry : Q.entrySet()) {
//				System.out.print("(" + entry.getKey() + ", " + entry.getValue() + ") ");
//			}
//			System.out.println("");
			
			// traverse each route
			for (String rid : Q.keySet()) {
				numOfRelaxedRoutes ++;
				
				int fromIndex = model.getRoute(rid).getStopSequence().indexOf(Q.get(rid));
				
				Map<String, Boolean> scanned = Maps.newHashMap();
				for (int i = fromIndex; i < model.getRoute(rid).getStopSequence().size(); i++) {
					String sid = model.getRoute(rid).getStopId(i);
					scanned.put(sid, false);
				}
				
				String currTrip = null;
				String boardingStop = null;
				QueryTime boardingDqt = null;
				
//				System.out.println("route " + rid + ": starting from stop " + subseq.get(0));
				for (int i = fromIndex; i < model.getRoute(rid).getStopSequence().size(); i++) {
					
					numOfRelaxedStops ++;
					
					String sid = model.getRoute(rid).getStopId(i);
					if (scanned.get(sid)) {
						// its arrival time has been updated before
						
						// re-calculate the earliest trip, which must be no later than the current one
						QueryTime stopAqt = roundStopLabels.get(k-1).get(sid).getArrivalTime();
						Tuple2<String, QueryTime> earliestTripInfo = earliestRouteDepartureAtStop(rid, sid, i, stopAqt);
						currTrip = earliestTripInfo.a;
						boardingStop = sid;
						boardingDqt = earliestTripInfo.b;
						
						continue;
					}
					
					scanned.put(sid, true);
					
//					System.out.print(sid + " ");
					
					if (currTrip != null) {
						// update the arrival time of the kth round and best known
						
						QueryTime bkAqt1 = eaStopLabels.get(sid).getArrivalTime();
						QueryTime bkAqt2 = eaStopLabels.get("target").getArrivalTime();
						
						int tmpAt = model.getTrip(currTrip).getEvent(i).getArrivalTime();
						QueryTime tmpAqt = new QueryTime(tmpAt, boardingDqt.day, boardingDqt.overnights);
						
						numOfLabelComparisons ++;
						
						if (tmpAqt.earlierThan(bkAqt1) && tmpAqt.earlierThan(bkAqt2)) {
							
							// update the label and arrival times
							
							Label label = new Label(sid, boardingStop, currTrip, boardingDqt, tmpAqt, 
									k, roundStopLabels.get(k-1).get(boardingStop));
							
//							if (sid.equals("2085")) {
//							System.out.println("old label");
//							eaStopLabels.get(sid).printMe();
//							System.out.println("new label");
//							label.printMe();
////							new java.util.Scanner(System.in).nextLine();
//							}
							
							stopLabelsk.put(sid, label);
							eaStopLabels.put(sid, label);
							if (!marked.get(sid)) {
								markedStops.add(sid);
								marked.put(sid, true);
							}
							
							// check if target can be walked from the stop 
							double walkingDistance = model.getStop(sid).getLocation().distanceTo(query.getToLoc());
							
							if (walkingDistance <= ModelBuilder.defaultMaxTailWalkingDistance) {
								double walkingTime = walkingDistance/query.getWalkingSpeed();
								// calculate the new arrival time of the walk
								QueryTime walkingAqt = new QueryTime(tmpAqt);
								walkingAqt.moveForward((int) walkingTime);
								
								if (walkingAqt.earlierThan(eaStopLabels.get("target").getArrivalTime())) {
									Label walkingLabel = new Label("target", sid, "walking", 
											tmpAqt, walkingAqt, k, label);
									stopLabelsk.put("target", walkingLabel);
									eaStopLabels.put("target", walkingLabel);
								}
							}
						}
					}
					
					if (currTrip == null) {
						// create the current trip as the earliest trip of the current stop
						QueryTime stopAqt = roundStopLabels.get(k-1).get(sid).getArrivalTime();
						Tuple2<String, QueryTime> earliestTripInfo = earliestRouteDepartureAtStop(rid, sid, i, stopAqt);
						currTrip = earliestTripInfo.a;
						boardingStop = sid;
						boardingDqt = earliestTripInfo.b;
					}
					else {
						int tmpDt = model.getTrip(currTrip).getEvent(i).getDepartureTime();
						QueryTime tmpDqt = new QueryTime(tmpDt, boardingDqt.day, boardingDqt.overnights);
						
						QueryTime lastAqt = new QueryTime(roundStopLabels.get(k-1).get(sid).getArrivalTime());
						if (lastAqt.differenceFrom(tmpDqt) < ModelBuilder.changeTime) {
							// lastAqt + changeTime < tmpDqt
							// calculate the earliest trip based on the current stop
							QueryTime stopAqt = roundStopLabels.get(k-1).get(sid).getArrivalTime();
							Tuple2<String, QueryTime> earliestTripInfo = earliestRouteDepartureAtStop(rid, sid, i, stopAqt);
							currTrip = earliestTripInfo.a;
							boardingStop = sid;
							boardingDqt = earliestTripInfo.b;
						}
					}
					
//					stopLabelsk.get(sid).printMe();
				}
//				System.out.println("");
			}
			

			// update the walking marked stops
			Map<String, Label> walkingStopLabels = Maps.newHashMap();
			for (String sid : markedStops) {
//				System.out.println("walking from stop " + sid);
				for (FootPath fp : model.getFootPathOfStop(sid)) {
					numOfScannedFootPaths ++;
					
					if (roundStopLabels.get(k-1).get(fp.getToStopId()).getTripId() != null) {
						// it's reached by walking, cannot walk any more
						if (roundStopLabels.get(k-1).get(fp.getToStopId()).getTripId().equals("walking")) {
							continue;
						}
					}
					
					if (fp.getWalkingDistance() > query.getMaxWalkingDistance()) { // the foot-path is too long
						break;
					}
					
					// compute the walking time with the walking speed
					double walkingTime = 1.0*fp.getWalkingDistance()/query.getWalkingSpeed();
					
					QueryTime walkingAqt = new QueryTime(stopLabelsk.get(sid).getArrivalTime());
					walkingAqt.moveForward((int) walkingTime);
					
					if (walkingAqt.earlierThan(stopLabelsk.get(fp.getToStopId()).getArrivalTime())) {
						Label walkingLabel = new Label(fp.getToStopId(), sid, "walking", 
								eaStopLabels.get(sid).getArrivalTime(), walkingAqt, k, stopLabelsk.get(sid));
						
//						if (fp.getToStopId().equals("2085")) {
//							roundStopLabels.get(k-1).get(fp.getToStopId()).printMe();
//							walkingLabel.printMe();
//						}
						
//						if (sid.equals("2085")) {
//							roundStopLabels.get(k-1).get(fp.getToStopId()).printMe();
//							walkingLabel.printMe();
//						}
						
						if (walkingStopLabels.containsKey(fp.getToStopId())) {
							// check if the new walking arrival time is earlier
							if (walkingLabel.arrivesEarlierThan(walkingStopLabels.get(fp.getToStopId()))) {
								walkingStopLabels.put(fp.getToStopId(), walkingLabel);
							}
						}
						else {
							walkingStopLabels.put(fp.getToStopId(), walkingLabel);
						}
					}
				}
			}
			
			for (String sid : walkingStopLabels.keySet()) {
				stopLabelsk.put(sid, walkingStopLabels.get(sid));
				eaStopLabels.put(sid, walkingStopLabels.get(sid));
				
				if (!marked.get(sid)) {
					markedStops.add(sid);
					marked.put(sid, true);
				}
			}
			
			roundStopLabels.add(stopLabelsk);
			
//			stopLabelsk.get("3477").printMe();
//			stopLabelsk.get("3485").printMe();
//			stopLabelsk.get("3156").printMe();
			
//			System.out.println("k = " + k);
//			new java.util.Scanner(System.in).nextLine();
			
			k ++;
			
			if (k > 3)
				break;
			
//			System.out.println("k = " + k + ", marked stops: " + markedStops.size());
//			for (String sid : markedStops) {
//				System.out.print(sid + ", ");
//			}
//			System.out.println("");
		}
		
		Journey journey = new Journey();
		
		if (eaStopLabels.get("target").getPrevId() == null)
			return journey;
		
		// from stopLabels to journey
		
		Label currLabel = eaStopLabels.get("target");
		do {
			JourneyEvent je = new JourneyEvent(currLabel);
			journey.addJourneyEvent(0, je);
			
			currLabel = currLabel.getPreLabel();
//			currLabel = eaStopLabels.get(currLabel.getPrevId());
		} while (!currLabel.getStopId().equals("source"));
		
		return journey;
	}
	
	
	// index = the index of the stop in the route
	public Tuple2<String, QueryTime> earliestRouteDepartureAtStop(String routeId, String stopId, int index, QueryTime at) {
		
//		at.printIt();
		
		String etId = null;
		int etDt = -1;
		String today = at.day;
		int overnights = 0;
		
		for (String tid : model.getRoute(routeId).getTrips()) {
			
			// if this trip works today?
			if (!model.getTrip(tid).containsWorkingDay(today)) {
//				System.out.println("trip " + tid + " does not work on " + today);
				continue;
			}
			
			int tmpDt = model.getTrip(tid).getEvent(index).getDepartureTime();
			if (tmpDt > at.time + ModelBuilder.changeTime) {
				etId = tid;
				etDt = tmpDt;
				break;
			}
		}
		
		while (etId == null) { // there is no trip today, find trips in the next day
			today = Calendar.nextWeekDay(today);
			overnights ++;
			for (String tid : model.getRoute(routeId).getTrips()) {
				// if this trip works this day?
				if (!model.getTrip(tid).containsWorkingDay(today)) {
//					System.out.println("trip " + tid + " does not work on " + query.getDay());
					continue;
				}
				
				etId = tid;
				etDt = model.getTrip(tid).getEvent(index).getDepartureTime();
				break;
			}
			
//			System.out.println("On " + currDay + ", et = " + et);
		}
		
		QueryTime etDqt = new QueryTime(etDt, today, at.overnights + overnights);
		
//		System.out.println("route = " + routeId + ", stop = " + stopId + ", index = " + index);
//		System.out.println("time = ");
//		at.printIt();
//		System.out.println("trip = " + etId + ", dt = ");
//		etDqt.printIt();
//		new java.util.Scanner(System.in).nextLine();
		
		return new Tuple2<String, QueryTime>(etId, etDqt);
		
	}
	
	// index = the index of the stop in the route
		public Tuple2<String, QueryTime> latestRouteArrivalAtStop(String routeId, String stopId, int index, QueryTime dt) {
			
//			at.printIt();
			
			String ltId = null;
			int ltAt = -1;
			String today = dt.day;
			int overnights = 0;
			
			for (int i = model.getRoute(routeId).getTrips().size()-1; i > -1; i--) {
				String tid = model.getRoute(routeId).getTripId(i);
				
				// if this trip works today?
				if (!model.getTrip(tid).containsWorkingDay(today)) {
//					System.out.println("trip " + tid + " does not work on " + today);
					continue;
				}
				
				int tmpAt = model.getTrip(tid).getEvent(index).getArrivalTime();
				if (tmpAt <= dt.time - ModelBuilder.changeTime) {
					ltId = tid;
					ltAt = tmpAt;
					break;
				}
			}
			
			while (ltId == null) { // there is no trip today, find trips in the next day
				today = Calendar.prevWeekDay(today);
				overnights --;
				for (int i = model.getRoute(routeId).getTrips().size()-1; i > -1; i--) {
					String tid = model.getRoute(routeId).getTripId(i);
					
					// if this trip works this day?
					if (!model.getTrip(tid).containsWorkingDay(today)) {
//						System.out.println("trip " + tid + " does not work on " + query.getDay());
						continue;
					}
					
					ltId = tid;
					ltAt = model.getTrip(tid).getEvent(index).getArrivalTime();
					break;
				}
				
//				System.out.println("On " + currDay + ", et = " + et);
			}
			
			QueryTime ltAqt = new QueryTime(ltAt, today, dt.overnights + overnights);
			ltAqt.within24Hours();
			
//			System.out.println("route = " + routeId + ", stop = " + stopId + ", index = " + index);
//			System.out.println("time = ");
//			at.printIt();
//			System.out.println("trip = " + etId + ", dt = ");
//			etDqt.printIt();
//			new java.util.Scanner(System.in).nextLine();
			
			return new Tuple2<String, QueryTime>(ltId, ltAqt);
			
		}
	
	
	public Journey fromStopLabels(Label targetLabel, String sourceId) {
		Journey journey = new Journey();
		Label currLabel = targetLabel;
		
		// add the last journey event
		JourneyEvent je = new JourneyEvent(currLabel);
		journey.addJourneyEvent(0, je);
		
		currLabel = currLabel.getPreLabel();
		
		while (!currLabel.getStopId().equals(sourceId)) {
			je = new JourneyEvent(currLabel);
			
			if (je.getTripId().equals("walking")) {
				// check if one can take a public transport instead of walking between the two stations
				JourneyEvent nextJe = journey.firstEvent();
				int nextDepIndex = model.getTrip(nextJe.getTripId()).indexOfDeparture(nextJe.getFromStopId(), nextJe.getDepartureTime().time);
				int currDepIndex =  model.getTrip(nextJe.getTripId()).lastIndexOfStop(je.getFromStopId(), nextDepIndex);
				
				if (currDepIndex > 0) {
					// this stop is before the next stop in the same trip
					int currDepTime = model.getTrip(nextJe.getTripId()).getEvent(currDepIndex).getDepartureTime();
					
					int changeTime = ModelBuilder.changeTime;
					if (currLabel.getPreLabel().getStopId().equals(sourceId)) {
						changeTime = 0;
					}
					if (currDepTime - changeTime >= currLabel.getPreLabel().getArrivalTime().time) {
						// there is enough time to change to the trip
						
						// change the trip
						nextJe.setFromStopId(je.getFromStopId());
						nextJe.setDepartureTime(new QueryTime(currDepTime, nextJe.getArrivalTime().day, nextJe.getArrivalTime().overnights));
						
					}
				}
			}
			
			journey.addJourneyEvent(0, je);
			currLabel = currLabel.getPreLabel();
		}
		
		return journey;
	}
	
	
	
	public Journey latestDepartureJourney(Query query) {
		numOfRelaxedRoutes = 0;
		numOfRelaxedStops = 0;
		numOfLabelComparisons = 0;
		
		/*** initialization ***/
		
		roundStopLabels = new ArrayList<Map<String, Label>>();
		
		Map<String, Label> stopLabels0 = Maps.newHashMap();
		for (RaptorStop raptStop : model.getStops().values()) {
			Label label = new Label(raptStop.getId(), null, null, QueryTime.MIN_VALUE(query.getNow().day), query.getNow(), 0, null);
			stopLabels0.put(raptStop.getId(), label);
		}
		Label toLabel = new Label(query.getToStopId(), null, null, query.getNow(), query.getNow(), 0, null);
		stopLabels0.put(query.getToStopId(), toLabel);
		
		// mark list
		Map<String, Boolean> marked = Maps.newHashMap();
		for (RaptorStop raptStop : model.getStops().values()) {
			marked.put(raptStop.getId(), false);
		}
		
		// the list of marked stops
		List<String> markedStops = new ArrayList<String>();
		
		markedStops.add(query.getToStopId());
		marked.put(query.getToStopId(), true);
		
		// mark all the stops within the walking distance of the target stop since their departure times are updated
		for (FootPath fp : model.getFootPathOfStop(query.getToStopId())) {
			numOfScannedFootPaths ++;
			
			if (fp.getWalkingDistance() > ModelBuilder.defaultMaxTailWalkingDistance) { // the foot-path is too long
				break;
			}
			
			// compute the walking time with the walking speed
			double walkingTime = 1.0*fp.getWalkingDistance()/query.getWalkingSpeed();
			
			QueryTime walkingDqt = new QueryTime(query.getNow());
			walkingDqt.moveBackward((int) walkingTime);
			
			Label walkingLabel = new Label(fp.getToStopId(), query.getToStopId(), "walking",
					walkingDqt, query.getNow(), 0, toLabel);
			
//			walkingLabel.printMe();
			
			stopLabels0.put(fp.getToStopId(), walkingLabel);
			markedStops.add(fp.getToStopId());
			marked.put(fp.getToStopId(), true);
		}
	
		roundStopLabels.add(stopLabels0);
		
		// latest departure label list, initialized to be the labelList0
		ldStopLabels = Maps.newHashMap(stopLabels0);
		
		/*** search ***/
		
		int k = 1; // round: number of trips in the journey
		
		while(!markedStops.isEmpty()) {
			Map<String, Label> stopLabelsk = Maps.newHashMap(roundStopLabels.get(k-1));
			
			System.out.println("k = " + k + ", number of marked stops = " + markedStops.size() + "/" + model.getStops().size());
			
			// create a new empty Q, and put the pairs (route id, latest marked stop) into Q
			Map<String, String> Q = Maps.newHashMap();
			for (String sid : markedStops) {
				marked.put(sid, false);
				
//				System.out.println("stop = " + sid);
				for (String rid : model.getStop(sid).getRoutes()) { // for each route serving this marked stop
					if (Q.containsKey(rid)) { // this route has already been in Q
						int index1 = model.getStop(sid).getLastIdxOfRoute(rid);
						int index2 = model.getStop(Q.get(rid)).getLastIdxOfRoute(rid);
						
						if (index1 > index2) { // the current stop appears later than the previous one in the route, replace
							
							Q.replace(rid, sid);
						}
					}
					else { // the route is new to Q, add it into Q
						Q.put(rid, sid);
					}
				}
			}
			
			// empty the marked stops
			markedStops.clear();
			
//			for (Map.Entry<String, String> entry : Q.entrySet()) {
//				System.out.print("(" + entry.getKey() + ", " + entry.getValue() + ") ");
//			}
//			System.out.println("");
			
			// traverse each route
			for (String rid : Q.keySet()) {
				numOfRelaxedRoutes ++;
				
				// toIndex is the finishing index of the sequence of the route: the index of the corresponding stop in Q
				int toIndex = model.getRoute(rid).getStopSequence().lastIndexOf(Q.get(rid));
				
				Map<String, Boolean> scanned = Maps.newHashMap();
				for (int i = toIndex; i > -1; i--) {
					String sid = model.getRoute(rid).getStopId(i);
					scanned.put(sid, false);
				}
				
				String currTrip = null;
				String arrivalStop = null;
				QueryTime arrivalAqt = null;
				
//				System.out.println("route " + rid + ": starting from stop " + subseq.get(0));
				for (int i = toIndex; i > -1; i--) {
					
					numOfRelaxedStops ++;
					
					String sid = model.getRoute(rid).getStopId(i);
					if (scanned.get(sid)) {
						// its departure time has been updated before
						
						// re-calculate the latest trip, which must be no earlier than the current one
						QueryTime stopDqt = roundStopLabels.get(k-1).get(sid).getDepartureTime();
						Tuple2<String, QueryTime> latestTripInfo = latestRouteArrivalAtStop(rid, sid, i, stopDqt);
						currTrip = latestTripInfo.a;
						arrivalStop = sid;
						arrivalAqt = latestTripInfo.b;
						
						continue;
					}
					
					scanned.put(sid, true);
					
//					System.out.print(sid + " ");
					
					if (currTrip != null) {
						// update the departure time of the kth round and best known
						
						QueryTime bkDqt1 = ldStopLabels.get(sid).getDepartureTime();
						QueryTime bkDqt2 = ldStopLabels.get(query.getFromStopId()).getDepartureTime();
						
						int tmpDt = model.getTrip(currTrip).getEvent(i).getDepartureTime();
						QueryTime tmpDqt = new QueryTime(tmpDt, arrivalAqt.day, arrivalAqt.overnights);
						
						numOfLabelComparisons ++;
						
						if (bkDqt1.earlierThan(tmpDqt) && bkDqt2.earlierThan(tmpDqt)) {
							
							// update the label and arrival times
							
							Label label = new Label(sid, arrivalStop, currTrip, tmpDqt, arrivalAqt, 
									k, roundStopLabels.get(k-1).get(arrivalStop));
							
//							if (sid.equals("101485")) {
//							System.out.println("old label");
//							eaStopLabels.get(sid).printMe();
//							System.out.println("new label");
//							label.printMe();
////							new java.util.Scanner(System.in).nextLine();
//							}
							
							stopLabelsk.put(sid, label);
							eaStopLabels.put(sid, label);
							if (!marked.get(sid)) {
								markedStops.add(sid);
								marked.put(sid, true);
							}
							
							
							// check if source can be walked from the stop 
							double walkingDistance = model.getStop(sid).getLocation()
									.distanceTo(model.getStop(query.getFromStopId()).getLocation());
							
							if (walkingDistance <= ModelBuilder.defaultMaxTailWalkingDistance) {
								double walkingTime = walkingDistance/query.getWalkingSpeed();
								// calculate the new departure time of the walk
								QueryTime walkingDqt = new QueryTime(tmpDqt);
								walkingDqt.moveBackward((int) walkingTime);
								
								if (walkingDqt.laterThan(eaStopLabels.get(query.getToStopId()).getArrivalTime())) {
									Label walkingLabel = new Label(query.getFromStopId(), sid, "walking", 
											walkingDqt, tmpDqt, k, label);
									stopLabelsk.put(query.getToStopId(), walkingLabel);
									eaStopLabels.put(query.getToStopId(), walkingLabel);
								}
							}
						}
					}
					
					if (currTrip == null) {
						// create the current trip as the earliest trip of the current stop
						QueryTime stopDqt = roundStopLabels.get(k-1).get(sid).getDepartureTime();
						Tuple2<String, QueryTime> latestTripInfo = latestRouteArrivalAtStop(rid, sid, i, stopDqt);
						currTrip = latestTripInfo.a;
						arrivalStop = sid;
						arrivalAqt = latestTripInfo.b;
					}
					else {
						int tmpDt = model.getTrip(currTrip).getEvent(i).getDepartureTime();
						QueryTime tmpDqt = new QueryTime(tmpDt, arrivalAqt.day, arrivalAqt.overnights);
						
						QueryTime lastDqt = new QueryTime(roundStopLabels.get(k-1).get(sid).getDepartureTime());
						if (lastDqt.differenceFrom(tmpDqt) > ModelBuilder.changeTime) {
							// lastDqt - changeTime > tmpDqt
							// calculate the earliest trip based on the current stop
							QueryTime stopDqt = roundStopLabels.get(k-1).get(sid).getDepartureTime();
							Tuple2<String, QueryTime> latestTripInfo = latestRouteArrivalAtStop(rid, sid, i, stopDqt);
							if (!latestTripInfo.a.equals(currTrip)) {
								currTrip = latestTripInfo.a;
								arrivalStop = sid;
								arrivalAqt = latestTripInfo.b;
							}
						}
					}
					
//					stopLabelsk.get(sid).printMe();
				}
//				System.out.println("");
			}
			

			// update the walking marked stops
			Map<String, Label> walkingStopLabels = Maps.newHashMap();
			for (String sid : markedStops) {
//				System.out.println("walking from stop " + sid);
				for (FootPath fp : model.getFootPathOfStop(sid)) {
					numOfScannedFootPaths ++;
					
					if (roundStopLabels.get(k-1).get(fp.getToStopId()).getTripId() != null) {
						// it's reached by walking, cannot walk any more
						if (roundStopLabels.get(k-1).get(fp.getToStopId()).getTripId().equals("walking")) {
							continue;
						}
					}
					
					if (fp.getWalkingDistance() > query.getMaxWalkingDistance()) { // the foot-path is too long
						break;
					}
					
					// compute the walking time with the walking speed
					double walkingTime = 1.0*fp.getWalkingDistance()/query.getWalkingSpeed();
					
					QueryTime walkingDqt = new QueryTime(stopLabelsk.get(sid).getDepartureTime());
					walkingDqt.moveBackward((int) walkingTime);
					
					if (walkingDqt.laterThan(stopLabelsk.get(fp.getToStopId()).getDepartureTime())) {
						Label walkingLabel = new Label(fp.getToStopId(), sid, "walking", 
								walkingDqt, ldStopLabels.get(sid).getDepartureTime(), k, stopLabelsk.get(sid));
						
//						if (fp.getToStopId().equals("2085")) {
//							roundStopLabels.get(k-1).get(fp.getToStopId()).printMe();
//							walkingLabel.printMe();
//						}
						
//						if (sid.equals("2085")) {
//							roundStopLabels.get(k-1).get(fp.getToStopId()).printMe();
//							walkingLabel.printMe();
//						}
						
						if (walkingStopLabels.containsKey(fp.getToStopId())) {
							// check if the new walking departure time is later
							if (walkingLabel.departsLaterThan(walkingStopLabels.get(fp.getToStopId()))) {
								walkingStopLabels.put(fp.getToStopId(), walkingLabel);
							}
						}
						else {
							walkingStopLabels.put(fp.getToStopId(), walkingLabel);
						}
					}
				}
			}
			
			for (String sid : walkingStopLabels.keySet()) {
				stopLabelsk.put(sid, walkingStopLabels.get(sid));
				ldStopLabels.put(sid, walkingStopLabels.get(sid));
				
				if (!marked.get(sid)) {
					markedStops.add(sid);
					marked.put(sid, true);
				}
			}
			
			roundStopLabels.add(stopLabelsk);
			
//			stopLabelsk.get("3477").printMe();
//			stopLabelsk.get("3485").printMe();
//			stopLabelsk.get("3156").printMe();
			
//			System.out.println("k = " + k);
//			new java.util.Scanner(System.in).nextLine();
			
			k ++;
			
			if (k > 3)
				break;
			
//			System.out.println("k = " + k + ", marked stops: " + markedStops.size());
//			for (String sid : markedStops) {
//				System.out.print(sid + ", ");
//			}
//			System.out.println("");
		}
		
		Journey journey = new Journey();
		
		if (ldStopLabels.get(query.getFromStopId()).getPrevId() == null)
			return journey;
		
		// from stopLabels to journey
		
		Label currLabel = ldStopLabels.get(query.getFromStopId());
		do {
			JourneyEvent je = new JourneyEvent(currLabel, true);
			journey.addJourneyEvent(0, je);
			
			currLabel = currLabel.getPreLabel();
//			currLabel = eaStopLabels.get(currLabel.getPrevId());
		} while (!currLabel.getStopId().equals(query.getToStopId()));
		
		return journey;
	}
	
	
	
	// main for testing
	public static void main (String[] args) {
		// generate queries
//		List<Query> queries = Query.randomQueries(100, model);
//		Query.toJSON(queries, "data/JSON/Adelaide/");
	
		File file = new File("data/JSON/Adelaide/RaptorModel.json");
		RaptorModel model = RaptorModel.fromJSON(file);
//		List<Query> queries = Query.readQueryFromJSON("data/JSON/Adelaide/Queries.json");
		
		RaptorStop s1 = model.getStop("101485");
		RaptorStop s2 = model.getStop("100627");
		double distance = s1.getLocation().distanceTo(s2.getLocation());
		System.out.println("dist = " + distance);
		
		

		
//		for (TripForSearch tfs : model.getTrips().values()) {
//			if (tfs.getId().equals("139923")) {
//				System.out.println("Trip " + tfs.getId() + " belongs to route " + tfs.getRouteId());
//				for (TripEvent te : tfs.getEventSequence()) {
//					System.out.println("arrive at stop " + te.getStopId() + " at time " + te.getArrivalTime());
//				}
//			}	
//		}

//		for (RaptorRoute rr : model.getRoutes().values()) {
//			if (rr.getId().equals("C1-3")) {
//				System.out.println("Route " + rr.getId() + " has trips ");
//				for (String t : rr.getTrips()) {
//					System.out.print(t + " ");
//				}
//				System.out.println("");
//			}
//		}
		
//		for (StopForSearch sfs : model.getStops().values()) {
//			System.out.println("Routes for stop " + sfs.getId());
//			for (String rid : sfs.getRoutes()) {
//				System.out.print(rid + " ");
//			}
//			System.out.println("");
//		}
		
		PrintWriter writer = new PrintWriter(System.out);
		
		Raptor raptor = new Raptor(model);
		
		/*********** query ***********/
		
//		String from = "101357";
//		String to = "101383";
//		String day = "Wednesday";
//		int now = 59870;
//		
//		Query query = new Query(from, to, day, now);
//		
//		query.printMe();
//		
//		// run raptor
//		long tStart = System.currentTimeMillis();
//		
//		Journey journey = raptor.earliestArrivalJourney(query);
////		Journey journey = raptor.earliestArrivalMinChangeJourney(query);
//		journey.showMe();
//		
//		raptor.printMeasures();
//		
//		long tEnd = System.currentTimeMillis();
//		long tDelta = tEnd - tStart;
//		double elapsedSeconds = tDelta;
//		
////		csa.printMeasures();
//		System.out.println("Entire query time = " + elapsedSeconds + " ms.");
//		System.out.println("----------------------------------------------------");
		

		/*******************************************************************************************************/
		
		/***** location query ***/
		
		Location from = new Location(-34.906453,138.576063,"source");
		Location to = new Location(-34.849143,138.494925,"target");
		String day = "Wednesday";
		int now = 59870;
		
		LocationQuery query = new LocationQuery(from, to, day, now);
		
		query.printMe();
		
		// run raptor
		long tStart = System.currentTimeMillis();
		
		Journey journey = raptor.earliestArrivalJourney(query);
//		Journey journey = raptor.earliestArrivalMinChangeJourney(query);
		journey.showMe();
		
		raptor.printMeasures();
		
		long tEnd = System.currentTimeMillis();
		long tDelta = tEnd - tStart;
		double elapsedSeconds = tDelta;
		
//		csa.printMeasures();
		System.out.println("Entire query time = " + elapsedSeconds + " ms.");
		System.out.println("----------------------------------------------------");
	}
}

package com.yimei.routing.csa;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.conveyal.gtfs.model.Calendar;
import com.google.common.collect.Collections2;
import com.google.common.collect.Maps;
import com.yimei.modelbuilder.ModelBuilder;
import com.yimei.routing.core.Criteria;
import com.yimei.routing.core.FootPath;
import com.yimei.routing.core.Label;
import com.yimei.routing.core.LabelBag;
import com.yimei.routing.core.MoLabel;
import com.yimei.routing.core.MoLabelBag;
import com.yimei.routing.core.ObjectiveValues;
import com.yimei.routing.core.RoutingTrip;
import com.yimei.routing.journey.Journey;
import com.yimei.routing.journey.JourneyEvent;
import com.yimei.routing.query.Query;
import com.yimei.routing.query.QueryTime;
import com.yimei.routing.query.SequentialTargetQuery;

/***
 * The Connection Scanning Algorithm, based on the paper:
 * J. Dibbelt, T. Pajor, B. Strasser, and D. Wagner, "Intriguingly Simple and Fast Transit Routing", LNCS 2013.
 * @author e04499
 *
 */

public class Csa {

	private final CsaModel model; // model cannot be changed
	
	// measures
	private int numOfScannedArcs;
	private int numOfReachableArcs;
	private int numOfRelaxedArcs;
	private int numOfScannedFootPaths;
	
	public Csa(CsaModel model) {
		
		this.model = model;
	}
	
	public int getNumOfScannedArcs() {
		return numOfScannedArcs;
	}
	
	public int getNumOfReachableArcs() {
		return numOfReachableArcs;
	}
	
	public int getNumOfRelaxedArcs() {
		return numOfRelaxedArcs;
	}
	
	public void printMeasures() {
		System.out.println("Scanned = " + numOfScannedArcs + ", Reachable = " + numOfReachableArcs
				+ ", Relaxed = " + numOfRelaxedArcs + ", FootPaths = " + numOfScannedFootPaths);
	}
	
	
	public Journey searchEarliestArrivalJourney(Query query) {
		numOfScannedArcs = 0;
		numOfReachableArcs = 0;
		numOfRelaxedArcs = 0;
		numOfScannedFootPaths = 0;
		
//		int numOfSkippedArcs = 0;
		
		Map<String, Label> stopLabels = Maps.newHashMap();
		for (CsaStop cstop : model.getStops().values()) {
			Label label = new Label(cstop.getId(), null, null, QueryTime.MAX_VALUE(), QueryTime.MAX_VALUE());
			stopLabels.put(cstop.getId(), label);
		}
		Label fromLabel = new Label(query.getFromStopId(), null, null, query.getNow(), query.getNow());
		stopLabels.put(query.getFromStopId(), fromLabel);
		
		Map<String, Integer> tripFlag = Maps.newHashMap(); // the number of overnights when the trip is reached
		for (RoutingTrip rt : model.getTrips().values()) {
			tripFlag.put(rt.getId(), -1); // -1 implies that the trip has not been reached
		}
		
		int startingIndex = model.earliestConnectionIndex(query.getNow().time);
		String today = query.getDay();
		int overnights = 0;
		
//		for (Label label : stopLabels.values()) {
//			label.printMe();
//		}
		
		
		while (stopLabels.get(query.getToStopId()).getPrevId() == null) {
//			System.out.println("check timetable on " + today + ", overnights = " + overnights);
			
			if (overnights > 1) { // a journey crossing more than one day is unrealistic
				break;
			}
			
			// search for today's timetable
			for (int i = startingIndex; i < model.getConnections().size(); i++) {
				numOfScannedArcs ++;
				
				Connection c = model.getConnections().get(i);
				
				
				if (c.getDepartureTime() > stopLabels.get(query.getToStopId()).getArrivalTime().adjustedTime(overnights)) {
					break;
				}
				
				if (!model.getTrip(c.getTripId()).workingOn(today)) { // the trip is not working today
//					System.out.println("trip " + c.getTripId() + " doesn't work on " + today);
					continue;
				}
				
				if (tripFlag.get(c.getTripId()) < overnights) { // the trip is either unreached or reached in previous days
				
					if (tripFlag.get(c.getTripId()) > -1) { // the trip is already reached in previous days, skip
//						numOfSkippedArcs ++;
						continue;
					}
					
					// the trip is unreached
					int dailyAdjustedArrivalTime = stopLabels.get(c.getFromStopId()).getArrivalTime().adjustedTime(overnights);
					
					if (dailyAdjustedArrivalTime > c.getDepartureTime()-ModelBuilder.changeTime) { // cannot catch the connection
						continue;
					}
					
					// c is reachable, update the number of overnights when the trip is reached
					tripFlag.replace(c.getTripId(), overnights);
				}
				
				numOfReachableArcs ++;
				
				// c is reachable, check if it can improve the label of its arrival stop
				int dailyAdjustedArrivalTime = stopLabels.get(c.getToStopId()).getArrivalTime().adjustedTime(overnights);
				
				
				if (c.getArrivalTime() < dailyAdjustedArrivalTime) {
					numOfRelaxedArcs ++;
					
//					System.out.println(dailyAdjustedArrivalTime);
					
					QueryTime dt = new QueryTime(c.getDepartureTime(), today, overnights);
					dt.within24Hours();
					QueryTime at = new QueryTime(c.getArrivalTime(), today, overnights);
					at.within24Hours();
					
					Label label = new Label(c.getToStopId(), c.getFromStopId(), c.getTripId(), dt, at);
					stopLabels.replace(c.getToStopId(), label);
					
					// updating all the outgoing foot-paths from the stop that c arrives at
					for (FootPath fp : model.getFootPathOfStop(c.getToStopId())) {
						
						numOfScannedFootPaths ++;
						
						if (fp.getWalkingDistance() > query.getMaxWalkingDistance()) { // the foot-path is too long
							break;
						}
						
						// compute the walking time with the walking speed
						double walkingTime = 1.0*fp.getWalkingDistance()/query.getWalkingSpeed();
						
						QueryTime walkingArrivalTime = new QueryTime(label.getArrivalTime());
						walkingArrivalTime.within24Hours();
						walkingArrivalTime.moveForward((int) walkingTime);
						
						Label walkingLabel = new Label(fp.getToStopId(), c.getToStopId(), "walking", label.getArrivalTime(), walkingArrivalTime);
						
						if (walkingLabel.arrivesEarlierThan(stopLabels.get(fp.getToStopId()))) {
							stopLabels.replace(fp.getToStopId(), walkingLabel);
						}
						
//						if (tmpAdjustedAT < stopLabels.get(fp.getToStopId()).getArrivalTime().adjustedTime(overnights)) {
//							
//							QueryTime walkingDt = stopLabels.get(c.getToStopId()).getArrivalTime();
//							QueryTime walkingAt = new QueryTime(walkingDt);
//							walkingAt.moveForward((int) walkingTime);
//							walkingAt.within24Hours();
//							
//							label = new Label(fp.getToStopId(), c.getToStopId(), "walking", walkingDt, walkingDt);
//							
////							label.printMe();
//							
//							stopLabels.replace(fp.getToStopId(), label);
//						}
					}
				}
			}
			
			startingIndex = 0;
			today = Calendar.nextWeekDay(today);
			overnights ++;
		}
		
//		System.out.println("Skipped " + numOfSkippedArcs + " arcs.");
		
//		System.out.println("OK");
		
		Journey journey = new Journey();
		
		if (stopLabels.get(query.getToStopId()).getPrevId() == null)
			return journey;
		
		// from stopLabels to journey
		
		Label currLabel = stopLabels.get(query.getToStopId());
		while (!currLabel.getPrevId().equals(query.getFromStopId())) {
			JourneyEvent je = new JourneyEvent(currLabel);
			
//			currLabel.printMe();
			
			Label prevLabel = stopLabels.get(currLabel.getPrevId());
			while (prevLabel.getTripId().equals(je.getTripId())) {
				je.setFromStopId(prevLabel.getPrevId());
				je.setDepartureTime(prevLabel.getDepartureTime());
				
				if (prevLabel.getPrevId().equals(query.getFromStopId())) {
					break;
				}
				
				prevLabel = stopLabels.get(prevLabel.getPrevId());
			}
			
			journey.addJourneyEvent(0, je);
			currLabel = prevLabel;
		}
		
//		
//		
//		String currStopId = query.getToStopId();
//		String currTripId = stopLabels.get(currStopId).getTripId();
//		int currArrivalTime = stopLabels.get(currStopId).getArrivalTime();
//		int currDepartureTime = stopLabels.get(currStopId).getDepartureTime();
//		String currDay = stopLabels.get(currStopId).getDay();
//		while (!currStopId.equals(query.getFromStopId())) {
////			stopLabels.get(currStopId).printMe();
////			new java.util.Scanner(System.in).nextLine();
//			// find the first stop in the same trip
//			String prevStopId = stopLabels.get(currStopId).getPrevId();
//			while (stopLabels.get(prevStopId).getTripId().equals(currTripId)) {
//				
//				currDepartureTime = stopLabels.get(prevStopId).getDepartureTime();
//				prevStopId = stopLabels.get(prevStopId).getPrevId();
//				
//				if (prevStopId.equals(query.getFromStopId())) {
//					break;
//				}
//				
//				stopLabels.get(prevStopId).printMe();
//				System.out.println(stopLabels.get(prevStopId).getTripId() + ", " + currTripId);
//				System.out.println(stopLabels.get(prevStopId).getTripId().equals(currTripId));
//			}
//			
////			System.out.println("current stop = " + currStopId + ", prev stop = " + prevStopId + ", trip = " + currTripId
////					+ ", departure time = " + currDepartureTime);
//			
//			JourneyEvent je = new JourneyEvent(prevStopId, currStopId, currTripId, currDepartureTime, currArrivalTime, currDay);
//			journey.add(0, je);
//
//			currStopId = prevStopId;
//			currTripId = stopLabels.get(currStopId).getTripId();
//			currArrivalTime = stopLabels.get(currStopId).getArrivalTime();
//			currDepartureTime = stopLabels.get(currStopId).getDepartureTime();
//		}
		
		return journey;
	}
	
	
	public Journey searchMaxNtrEarliestArrivalJourney(Query query) {
		numOfScannedArcs = 0;
		numOfReachableArcs = 0;
		numOfRelaxedArcs = 0;
		numOfScannedFootPaths = 0;
		
//		int numOfSkippedArcs = 0;
		
		Map<String, LabelBag> stopLabelBags = Maps.newHashMap();
		for (CsaStop cstop : model.getStops().values()) {
			stopLabelBags.put(cstop.getId(), new LabelBag());
		}
		Label fromLabel = new Label(query.getFromStopId(), null, null, query.getNow(), query.getNow(), 0, null);
		stopLabelBags.get(query.getFromStopId()).updateWithLabel(fromLabel);
		
		Map<String, Integer> tripFlag = Maps.newHashMap(); // the number of overnights when the trip is reached
		for (RoutingTrip rt : model.getTrips().values()) {
			tripFlag.put(rt.getId(), -1); // -1 implies that the trip has not been reached
		}
		
		int startingIndex = model.earliestConnectionIndex(query.getNow().time);
		String today = query.getDay();
		int overnights = 0;
		
//		for (Label label : stopLabels.values()) {
//			label.printMe();
//		}
		
		
		while (stopLabelBags.get(query.getToStopId()).isEmpty()) {
//			System.out.println("check timetable on " + today + ", overnights = " + overnights);
			
			if (overnights > 1) { // a journey crossing more than one day is unrealistic
				break;
			}
			
			// search for today's timetable
			for (int i = startingIndex; i < model.getConnections().size(); i++) {
				numOfScannedArcs ++;
				
				Connection c = model.getConnections().get(i);
				
				if (!model.getTrip(c.getTripId()).workingOn(today)) { // the trip is not working today
//					System.out.println("trip " + c.getTripId() + " doesn't work on " + today);
					continue;
				}
				
				if (stopLabelBags.get(c.getFromStopId()).getNumOfTripLb() > query.getMaxNumOfTrips()) {
					continue;
				}
				
				for (Label label : stopLabelBags.get(query.getToStopId()).getLabelSet()) {
					if (stopLabelBags.get(c.getFromStopId()).dominatedBy(label)) { // the stop bag is dominated by that of the target
						continue;
					}
				}
				
				if (tripFlag.get(c.getTripId()) < overnights) { // the trip is either unreached or reached in previous days
				
					if (tripFlag.get(c.getTripId()) > -1) { // the trip is already reached in previous days, skip
//						numOfSkippedArcs ++;
						continue;
					}
					
					// the trip is unreached, test whether c is reachable
					if (stopLabelBags.get(c.getFromStopId()).isEmpty()) { // c is unreachable
						continue;
					}
					
					// find the earliest arrival time of the from stop of c
					int dailyAdjustedArrivalTime = stopLabelBags.get(c.getFromStopId()).getArrivalTimeLb().adjustedTime(overnights);
					
					if (dailyAdjustedArrivalTime > c.getDepartureTime()-ModelBuilder.changeTime) { // cannot catch the connection
						continue;
					}
					
					// c is reachable, update the number of overnights when the trip is reached
					tripFlag.replace(c.getTripId(), overnights);
				}
				
				numOfReachableArcs ++;
				
//				System.out.println("start printing label bag of connection: (" + c.getFromStopId() + ", " + c.getToStopId() + ")");
//				stopLabelBags.get(c.getFromStopId()).printLabelSet();
				
				c.printMe();
				System.out.println("label bag of " + c.getFromStopId());
				stopLabelBags.get(c.getFromStopId()).printLabelSet();
				
				
				// c is reachable
				// find the label with the least trips, and whose arrival time is earlier than the departure time of c
				Label preLabel = null;
				int minNumOfTrips = Integer.MAX_VALUE;
				for (Label label : stopLabelBags.get(c.getFromStopId()).getLabelSet()) {
					
					if (!c.getTripId().equals(label.getTripId()) && 
							label.getArrivalTime().adjustedTime(overnights) > c.getDepartureTime()-ModelBuilder.changeTime) {
						continue;
					}
					
//					label.printMe();
					
					int tmpNumOfTrips = label.getNumOfTrips();
					if (!c.getTripId().equals(label.getTripId())) {
						tmpNumOfTrips ++;
					}
					
					if (tmpNumOfTrips < minNumOfTrips) {
						minNumOfTrips = tmpNumOfTrips;
						preLabel = label;
					}
				}
				
				System.out.println("pre label");
				preLabel.printMe();
					
				QueryTime cDT = new QueryTime(c.getDepartureTime(), today, overnights);
				cDT.within24Hours();
				QueryTime cAT = new QueryTime(c.getArrivalTime(), today, overnights);
				cAT.within24Hours();
				
				Label tmpLabel = new Label(c.getToStopId(), c.getFromStopId(), c.getTripId(), cDT, cAT, minNumOfTrips, preLabel);
				
				System.out.println("tmp label");
				tmpLabel.printMe();
					
				if (stopLabelBags.get(c.getToStopId()).updateWithLabel(tmpLabel)) {
					System.out.println("label bag of " + c.getToStopId());
					stopLabelBags.get(c.getToStopId()).printLabelSet();
					
					// update foot-paths
					for (FootPath fp : model.getFootPathOfStop(c.getToStopId())) {
						
						numOfScannedFootPaths ++;
						
						if (fp.getWalkingDistance() > query.getMaxWalkingDistance()) { // the foot-path is too long
							break;
						}
						
						// compute the walking time with the walking speed
						double walkingTime = 1.0*fp.getWalkingDistance()/query.getWalkingSpeed();
						
						QueryTime walkingArrivalTime = new QueryTime(tmpLabel.getArrivalTime());
						walkingArrivalTime.moveForward((int) walkingTime);
						
						Label walkingLabel = new Label(fp.getToStopId(), c.getToStopId(), "walking", 
								tmpLabel.getArrivalTime(), walkingArrivalTime, tmpLabel.getNumOfTrips()+1, tmpLabel);
						
//						walkingMol.updateMOLabelBag(stopLabelBag.get(fp.getToStopId()), criteria);
						boolean a = stopLabelBags.get(fp.getToStopId()).updateWithLabel(walkingLabel);
//						stopLabelBags.get(fp.getToStopId()).printLabelSet();
//						if (a) {
//							System.out.println("this label");
//							walkingMol.printMe();
//							System.out.println("new label bag:");
//							stopLabelBag.get(fp.getToStopId()).printLabelSet();
//							new java.util.Scanner(System.in).nextLine();
//						}
							
					}
				}
			}
			
			startingIndex = 0;
			today = Calendar.nextWeekDay(today);
			overnights ++;
		}
		
//		System.out.println("Skipped " + numOfSkippedArcs + " arcs.");
		
//		System.out.println("OK");
		
		Journey journey = new Journey();
		
		if (stopLabelBags.get(query.getToStopId()).isEmpty())
			return journey;
		
		// choose the label with the earliest arrival time
		int index = -1;
		QueryTime eat = QueryTime.MAX_VALUE();
		for (int i = 0; i < stopLabelBags.get(query.getToStopId()).getLabelSet().size(); i++) {
			if (stopLabelBags.get(query.getToStopId()).getLabel(i).getArrivalTime().earlierThan(eat)) {
				eat = stopLabelBags.get(query.getToStopId()).getLabel(i).getArrivalTime();
				index = i;
			}
		}

		
		Label currLabel = stopLabelBags.get(query.getToStopId()).getLabel(index);
		while (!currLabel.getPrevId().equals(query.getFromStopId())) {
			JourneyEvent je = new JourneyEvent(currLabel);
			
//			currLabel.printMe();
			
			Label prevLabel = currLabel.getPreLabel();
			while (prevLabel.getTripId().equals(je.getTripId())) {
				je.setFromStopId(prevLabel.getPrevId());
				je.setDepartureTime(prevLabel.getDepartureTime());
				
				if (prevLabel.getPrevId().equals(query.getFromStopId())) {
					break;
				}
				
				prevLabel = prevLabel.getPreLabel();
			}
			
			journey.addJourneyEvent(0, je);
			currLabel = prevLabel;
		}
		
		return journey;
	}
	
	
	
	
	
	public List<Journey> searchMOJourneys(Query query, Criteria criteria) {
		numOfScannedArcs = 0;
		numOfReachableArcs = 0;
		numOfRelaxedArcs = 0;
		numOfScannedFootPaths = 0;
		
//		int numOfSkippedArcs = 0;
		
		Map<String, MoLabelBag> stopLabelBag = Maps.newHashMap();
		for (CsaStop cstop : model.getStops().values()) {
			// initialize an empty label bag for each stop
			stopLabelBag.put(cstop.getId(), new MoLabelBag());
		}
		// initialize the label for the source stop
		// initialize the objective values of the source
		ObjectiveValues ovsFrom = new ObjectiveValues(query.getNow(), 0, 0.0, query.getNow());
		MoLabel fromLabel = new MoLabel(query.getFromStopId(), null, null, query.getNow(), null, ovsFrom);
		MoLabelBag fromLabelBag = new MoLabelBag();
		fromLabelBag.addLabel(fromLabel);
		stopLabelBag.put(query.getFromStopId(), fromLabelBag);
		
		Map<String, Integer> tripFlag = Maps.newHashMap(); // the number of overnights when the trip is reached
		for (RoutingTrip rt : model.getTrips().values()) {
			tripFlag.put(rt.getId(), -1); // -1 implies that the trip has not been reached
		}
		
//		Map<String, Integer> stopFlag = Maps.newHashMap(); // the number of connections checked from each stop
//		for (CsaStop cstop : model.getStops().values()) {
//			stopFlag.put(cstop.getId(), 0);
//		}

//		Map<String, ObjectiveValues> frontierStops = Maps.newHashMap();
//		ObjectiveValues fromOvsLb = new ObjectiveValues(nowNormTime, 0, 0.0, nowNormTime);
//		frontierStops.put(key, value)
//		BinHeap<String> frontierStops = new BinHeap<String>();
//		frontierStops.insert(query.getFromStopId(), 0.0); // initially, only the source stop is in the frontier, with min number of transfers = 0

//		BinHeap<String> frontierStops = new BinHeap<String>();
//		frontierStops.insert(query.getFromStopId(), 0.0); // initially, only the source stop is in the frontier, with min number of transfers = 0
//		
//		ObjectiveValues ovsLB = new ObjectiveValues(QueryTime.forever(), Integer.MAX_VALUE, Double.MAX_VALUE, QueryTime.forever());


//		Map<String, ObjectiveValues> stopOvsLB
//		BinHeap<String> frontierStops = new BinHeap<String>();
//		frontierStops.insert(query.getFromStopId(), 0.0); // initially, only the source stop is in the frontier, with min number of transfers = 0


		
		int startingIndex = model.earliestConnectionIndex(query.getNow().time);
		String today = query.getDay();
		int overnights = 0;
		
		while (overnights < 2) { // a journey crossing more than one day is unrealistic
//			System.out.println("check timetable on " + today + ", overnights = " + overnights);
			
			// search for today's timetable
			for (int i = startingIndex; i < model.getConnections().size(); i++) {
				numOfScannedArcs ++;
				
				Connection c = model.getConnections().get(i);
				
				if (!model.getTrip(c.getTripId()).workingOn(today)) { // the trip is not working today
//					System.out.println("trip " + c.getTripId() + " doesn't work on " + today);
					continue;
				}
				
				if (tripFlag.get(c.getTripId()) < overnights) { // the trip is either unreached or reached in previous days
				
					if (tripFlag.get(c.getTripId()) > -1) { // the trip is already reached, skip
//						numOfSkippedArcs ++;
						continue;
					}
					
					// the trip is unreached, test whether c is reachable
					if (stopLabelBag.get(c.getFromStopId()).isEmpty()) { // c is unreachable
						continue;
					}
					
					// find the earliest arrival time of the from stop of c, which is the first label
					int dailyAdjustedArrivalTime = stopLabelBag.get(c.getFromStopId()).getLabel(0).getArrivalTime().adjustedTime(overnights);
					
					if (dailyAdjustedArrivalTime > c.getDepartureTime()-ModelBuilder.changeTime) { // cannot catch the connection
						continue;
					}
					
					// c is reachable, update the number of overnights when the trip is reached
					tripFlag.replace(c.getTripId(), overnights);
				}
				
				
				
				numOfReachableArcs ++;
				
				// c is reachable, check if it can improve the labels of its arrival stop
				// find the previous label with min number of transfers at the from stop of c
				MoLabel prevMol = null;
				int minNTransfers = Integer.MAX_VALUE;
//				System.out.println("printing previous labels: " + stopLabelBag.get(c.getFromStopId()).getLabelSet().size());
				for (MoLabel mol : stopLabelBag.get(c.getFromStopId()).getLabelSet()) {
					
//					mol.printMe();
					
//					if (!c.getTripId().equals(mol.getTripId())) {
//						if (mol.getArrivalTime().adjustedTime(overnights) > c.getDepartureTime()-ModelBuilder.changeTime) {
//							break;
//						}
//					}
					
					int tmpNTransfers = mol.getNumOfTransfers();
					if (!c.getTripId().equals(mol.getTripId())) {
						tmpNTransfers ++;
					}
					
					if (tmpNTransfers < minNTransfers) {
						prevMol = mol;
						minNTransfers = tmpNTransfers;
					}
				}
				
				QueryTime cDT = new QueryTime(c.getDepartureTime(), today, overnights);
				cDT.within24Hours();
				QueryTime cAT = new QueryTime(c.getArrivalTime(), today, overnights);
				cAT.within24Hours();
				ObjectiveValues cOvs = new ObjectiveValues(cAT, minNTransfers, 0.0, cAT);
				MoLabel tmpMol = new MoLabel(c.getToStopId(), c.getFromStopId(), c.getTripId(), cDT, prevMol, cOvs);
				
//				System.out.println("this label:");
//				tmpMol.printMe();
				
				// update label bag of to stop of c
//				if (tmpMol.updateMOLabelBag(stopLabelBag.get(c.getToStopId()), criteria)) {
				if (stopLabelBag.get(c.getToStopId()).updateWithLabel(tmpMol, criteria)) {

//					System.out.println("this label");
//					tmpMol.printMe();
//					System.out.println("new label bag:");
//					stopLabelBag.get(c.getToStopId()).printLabelSet();
//					new java.util.Scanner(System.in).nextLine();

//						System.out.println("this label");
//						tmpMol.printMe();
//					System.out.println("prev label");
//					prevMol.printMe();
					prevMol.setNextDepartureTime(cDT);
					
					// tmpMol is non-dominated, further update the outgoing foot-paths from the stop that c arrives at
					for (FootPath fp : model.getFootPathOfStop(c.getToStopId())) {
						
						numOfScannedFootPaths ++;
						
						if (fp.getWalkingDistance() > query.getMaxWalkingDistance()) { // the foot-path is too long
							break;
						}
						
						// compute the walking time with the walking speed
						double walkingTime = 1.0*fp.getWalkingDistance()/query.getWalkingSpeed();
						
						QueryTime walkingArrivalTime = new QueryTime(tmpMol.getArrivalTime());
						walkingArrivalTime.within24Hours();
						walkingArrivalTime.moveForward((int) walkingTime);
						ObjectiveValues walkingOvs = new ObjectiveValues(walkingArrivalTime, tmpMol.getNumOfTransfers()+1, 
								tmpMol.getTotalWalkingDistance()+fp.getWalkingDistance(), walkingArrivalTime);
						
						MoLabel walkingMol = new MoLabel(fp.getToStopId(), c.getToStopId(), "walking", tmpMol.getArrivalTime(), tmpMol, walkingOvs);
						
//						if (fp.getToStopId().equals("1179")) {
//							System.out.println("label bag");
//							stopLabelBag.get(fp.getToStopId()).printLabelSet();
//						}
						
//						walkingMol.updateMOLabelBag(stopLabelBag.get(fp.getToStopId()), criteria);
						boolean a = stopLabelBag.get(fp.getToStopId()).updateWithLabel(walkingMol, criteria);
//						if (a) {
//							System.out.println("this label");
//							walkingMol.printMe();
//							System.out.println("new label bag:");
//							stopLabelBag.get(fp.getToStopId()).printLabelSet();
//							new java.util.Scanner(System.in).nextLine();
//						}
							
					}
				}
			}
			
			startingIndex = 0;
			today = Calendar.nextWeekDay(today);
			overnights ++;
		}
		
//		System.out.println("Skipped " + numOfSkippedArcs + " arcs.");
		
		if (stopLabelBag.get(query.getToStopId()) == null)
			return null;
		
//		System.out.println("OK");
		
		List<Journey> journeys = new ArrayList<Journey>();
		
		for (MoLabel mol : stopLabelBag.get(query.getToStopId()).getLabelSet()) {

			Journey journey = new Journey();
			journey.setObjectiveValues(mol.getObjectiveValues());

			// from stopLabels to journey
			
			MoLabel currLabel = mol;
			while (!currLabel.getPrevId().equals(query.getFromStopId())) {
				JourneyEvent je = new JourneyEvent(currLabel);
				
//				currLabel.printMe();
				
				MoLabel prevLabel = currLabel.getPrevLabel();
				while (prevLabel.getTripId().equals(je.getTripId())) {
					je.setFromStopId(prevLabel.getPrevId());
					je.setDepartureTime(prevLabel.getDepartureTime());
					
					if (prevLabel.getPrevId().equals(query.getFromStopId())) {
						break;
					}
					
					prevLabel = prevLabel.getPrevLabel();
				}
				
				journey.addJourneyEvent(0, je);
				currLabel = prevLabel;
			}
			
			journeys.add(journey);
		}
		
		return journeys;
	}
	
	
	
//	public Map<String, ProfileToTarget> searchProfilesToTarget(String toStopId) {
//		
//		Map<String, ProfileToTarget> stopProfiles = Maps.newHashMap();
//
//		// initialize an empty profile for each stop
//		for (CsaStop cstop : model.getStops().values()) {
//			ProfileToTarget ptt = new ProfileToTarget(toStopId);
//			stopProfiles.put(cstop.getId(), ptt);
//		}
//		
//		
//	}
	
	public Journey searchEarliestArrivalBestSequentialTargetJourney(SequentialTargetQuery query, PrintWriter writer, boolean write) {
		Journey journey = new Journey();
		List<String> targetSet = query.getTargetSequence();
		
		Collection<List<String>> targetPermutations = Collections2.orderedPermutations(targetSet);
		
		
		for (List<String> ts : targetPermutations) {
			long tStart = System.currentTimeMillis();
			
			query.setTargetSequence(ts);
			Journey perJourney = searchEarliestArrivalFixedSequentialTargetJourney(query, writer, write);
			
			long tEnd = System.currentTimeMillis();
			long tDelta = tEnd - tStart;
			double elapsedSeconds = tDelta;
			
			if (write) {
				writer.println("permutation query time = " + elapsedSeconds + " ms.");
				writer.println("---");
			}
			
			if (perJourney.isEmpty()) {
				continue;
			}
			
			if (perJourney.arrivesEarlierThan(journey)) {
				System.out.println("new best arrival time:");
				perJourney.getArrivalTime().printIt();
				
				journey = perJourney;
			}
		}
		
		return journey;
	}
	
	
	public Journey searchEarliestArrivalFixedSequentialTargetJourney(SequentialTargetQuery query, PrintWriter writer, boolean write) {
		
		Journey journey = new Journey();
		
		long tStart = System.currentTimeMillis();
		
		String today = query.getDay();
		Query singleTargetQuery = new Query(query.getFromStopId(), query.getTarget(0), query.getNow());
		Journey subJourney = searchEarliestArrivalJourney(singleTargetQuery);
		journey.append(subJourney.getSequence(), 1);
		
		if (write)
			subJourney.showMe(writer);
		
		long tEnd = System.currentTimeMillis();
		long tDelta = tEnd - tStart;
		double elapsedSeconds = tDelta;
		
		if (write) {
			writer.println("query time = " + elapsedSeconds + " ms.");
			writer.println("---");
		}
		
		if (subJourney.isEmpty()) {
			return journey;
		}
		
		for (int i = 0; i < query.getTargetSequence().size()-1; i++) {
			tStart = System.currentTimeMillis();
			
			QueryTime now = subJourney.getArrivalTime();
			singleTargetQuery = new Query(query.getTarget(i), query.getTarget(i+1), now);
			subJourney = searchEarliestArrivalJourney(singleTargetQuery);
			journey.append(subJourney.getSequence(), 1);
			
			if (write)
				subJourney.showMe(writer);
			
			tEnd = System.currentTimeMillis();
			tDelta = tEnd - tStart;
			elapsedSeconds = tDelta;
			
			if (write) {
				writer.println("query time = " + elapsedSeconds + " ms.");
				writer.println("---");
			}
			
			if (subJourney.isEmpty()) {
				journey.clear();
				return journey;
			}
		}
		
		return journey;
	}
	
	
	public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException {
		
		
		
		String database = "Adelaide";
		
		String jsonDir = "data/JSON/" + database;
		String resDir = "data/Results/" + database;
		
		CsaModel model = CsaModel.fromJSON(jsonDir + "/CsaModel.json");
//		List<SequentialTargetQuery> queries = SequentialTargetQuery.readSequentialTargetQueryFromJSON(jsonDir + "/SequentialQueries.json");
//		List<Query> queries = Query.readQueryFromJSON(jsonDir + "/SingleTargetQueries.json");
		File resFile = new File(resDir + "BestSequentialTargetJourneys.txt");
		resFile.getParentFile().mkdirs();
//		PrintWriter writer = new PrintWriter(resFile, "UTF-8");
		PrintWriter writer = new PrintWriter(System.out);
		
//		// print the footpaths of the stops
//		for (Map.Entry<String, List<FootPath>> entry : model.getFootPaths().entrySet()) {
//			System.out.println("Foot-paths of stop " + entry.getKey() + ": ");
//			for (FootPath fp : entry.getValue()) {
//				System.out.format(" to stop " + fp.getToStopId() + " with %.2f km. %n", fp.getDistance());
//			}
//		}
		
		Csa csa = new Csa(model);
		
//		for (int i = 0; i < queries.size(); i++) {
//			SequentialTargetQuery query = queries.get(i);
//			writer.print(i + ". ");
//			query.printMe(writer);
//			Query query = queries.get(i);
			
			String from = "6693";
			String to = "6665";
			String day = "Wednesday";
			int now = 30000;
			
			Query query = new Query(from, to, day, now);
			
			// run raptor
			long tStart = System.currentTimeMillis();
			
			Journey journey = csa.searchEarliestArrivalJourney(query);
//			Journey journey = csa.searchMaxNtrEarliestArrivalJourney(query);
			journey.showMe();
			
//			Journey journey = csa.searchEarliestArrivalFixedSequentialTargetJourney(query, writer, true);
//			writer.println("Final journey:");
//			journey.showMe(writer);
			
//			Criteria criteria = new Criteria(true, true, false, false);
//			List<Journey> journeys = csa.searchMOJourneys(query, criteria);
//			for (int j = 0; j < journeys.size(); j++) {
//				System.out.println("Journey " + j);
//				journeys.get(j).showMe();
//			}
			
			long tEnd = System.currentTimeMillis();
			long tDelta = tEnd - tStart;
			double elapsedSeconds = tDelta;
			
//			csa.printMeasures();
			writer.println("Entire query time = " + elapsedSeconds + " ms.");
			writer.println("----------------------------------------------------");
			
//			System.out.println("Press Any Key To Continue...");
//	          	new java.util.Scanner(System.in).nextLine();
			
//			System.out.format("Process: " + i + "/" + queries.size() + " = %.2f.\n", 1.0*i/queries.size());
//		}
		
		writer.close();
	}
}

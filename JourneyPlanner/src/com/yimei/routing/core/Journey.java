
package com.yimei.routing.core;

import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.yimei.routing.raptor.RaptorModel;
import com.yimei.util.TimeTransformer;

public class Journey {

	private List<JourneyEvent> sequence;
	private ObjectiveValues ovs;
	
	public Journey() {
		sequence = new ArrayList<JourneyEvent>();
	}
	
	public List<JourneyEvent> getSequence() {
		return sequence;
	}
	
	public JourneyEvent getEvent(int index) {
		return sequence.get(index);
	}
	
	public ObjectiveValues getObjectiveValues() {
		return ovs;
	}
	
	public void addJourneyEvent(int index, JourneyEvent je) {
		sequence.add(index, je);
	}
	
	public void setObjectiveValues(ObjectiveValues ovs) {
		this.ovs = ovs;
	}
	
	public void append(List<JourneyEvent> seq, int startIndex, int endIndex) {
		for (int i = startIndex; i < endIndex; i++) {
			this.sequence.add(seq.get(i));
		}
	}
	
	public void append(List<JourneyEvent> seq, int startIndex) {
		for (int i = startIndex; i < seq.size(); i++) {
			this.sequence.add(seq.get(i));
		}
	}
	
	public void append(List<JourneyEvent> seq) {
		for (int i = 0; i < seq.size(); i++) {
			this.sequence.add(seq.get(i));
		}
	}
	
	public JourneyEvent firstEvent() {
		return sequence.get(0);
	}
	
	public JourneyEvent lastEvent() {
		return sequence.get(sequence.size()-1);
	}
	
	public void clear() {
		sequence.clear();
	}
	
	public boolean isEmpty() {
		return sequence.isEmpty();
	}
	
	public QueryTime getArrivalTime() {
		return lastEvent().getArrivalTime();
	}
	
	public boolean arrivesEarlierThan(Journey journey) {
		if (journey.isEmpty()) {
			return true;
		}
		return getArrivalTime().earlierThan(journey.getArrivalTime());
	}
	
	public void getShowInfo(String database, RaptorModel model) throws ClassNotFoundException, SQLException {
		for (JourneyEvent je : sequence) {
			je.getShowInfo(database, model);
		}
	}
	
	public void showMe() {
		if (sequence.isEmpty()) {
			System.out.println("Sorry, couldn't find any journey...");
		}
		
		for (int i = 0; i < sequence.size(); i++) {
			System.out.println("Take trip " + sequence.get(i).getTripId()
					+ " from stop " + sequence.get(i).getFromStopId()
					+ " at " + TimeTransformer.IntegerToString(sequence.get(i).getDepartureTime().time)
					+ " on " + sequence.get(i).getDepartureTime().day
					+ ", arriving at stop " + sequence.get(i).getToStopId()
					+ " on " + sequence.get(i).getArrivalTime().day
					+ " at " + TimeTransformer.IntegerToString(sequence.get(i).getArrivalTime().time));
		}
	}
	
	public void showMe(PrintWriter writer) {
		if (sequence.isEmpty()) {
			writer.println("Sorry, couldn't find any journey...");
		}
		
		for (int i = 0; i < sequence.size(); i++) {
			writer.println("Take trip " + sequence.get(i).getTripId()
					+ " from stop " + sequence.get(i).getFromStopId()
					+ " at " + TimeTransformer.IntegerToString(sequence.get(i).getDepartureTime().time)
					+ " on " + sequence.get(i).getDepartureTime().day
					+ ", arriving at stop " + sequence.get(i).getToStopId()
					+ " on " + sequence.get(i).getArrivalTime().day
					+ " at " + TimeTransformer.IntegerToString(sequence.get(i).getArrivalTime().time));
		}
	}
}

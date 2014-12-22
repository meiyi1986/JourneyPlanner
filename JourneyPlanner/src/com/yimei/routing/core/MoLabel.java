package com.yimei.routing.core;

import java.util.List;

import com.yimei.util.TimeTransformer;

/***
 * The multiple-objective label
 * @author e04499
 *
 */

public class MoLabel {
	
	private String stopId;
	private String prevId; // the previous stop id
	private String tripId; // the trip id from the previous stop to the current one
	private QueryTime departureTime;
	private ObjectiveValues ovs;
	MoLabel prevLabel;
	
	public MoLabel () {
		
	}
	
	public MoLabel (String stopId) {
		this.stopId = stopId;
	}
	
	public MoLabel(String stopId, String prevId, String tripId, QueryTime dt, MoLabel prevLabel, ObjectiveValues ovs) {
		this.stopId = stopId;
		this.prevId = prevId;
		this.tripId = tripId;
		this.departureTime = dt;
		this.ovs = ovs;
		this.prevLabel = prevLabel;
	}
	
	public MoLabel (MoLabel lmo) {
		this.stopId = lmo.getStopId();
		this.prevId = lmo.getPrevId();
		this.tripId = lmo.getTripId();
		this.departureTime = lmo.getDepartureTime();
		this.ovs = lmo.ovs;
		this.prevLabel = lmo.getPrevLabel();
	}
	
	public String getStopId() {
		return stopId;
	}
	
	public String getPrevId() {
		return prevId;
	}
	
	public String getTripId() {
		return tripId;
	}
	
	public QueryTime getDepartureTime() {
		return departureTime;
	}
	
	public ObjectiveValues getObjectiveValues() {
		return ovs;
	}
	
	public MoLabel getPrevLabel() {
		return prevLabel;
	}
	
	
	
	public QueryTime getArrivalTime() {
		return ovs.getArrivalTime();
	}
	
	public int getNumOfTransfers() {
		return ovs.getNumOfTransfers();
	}
	
	public double getTotalWalkingDistance() {
		return ovs.getTotalWalkingDistance();
	}
	
	public QueryTime getNextDepartureTime() {
		return ovs.getNextDepartureTime();
	}
	
	public void setNumberOfTransfers(int nt) {
		ovs.setNumOfTransfers(nt);
	}
	
	public void setNextDepartureTime(QueryTime ndt) {
		ovs.setNextDepartureTime(ndt);
	}
	
	// -1 if it dominates cmpMol, 1 if it is dominated by cmpMol, and 0 if both are nondominated
	public int compareTo (MoLabel cmpMol, Criteria criteria) {
		return this.ovs.compareTo(cmpMol.getObjectiveValues(), criteria);
	}
	
	
	public boolean updateMOLabelBag(List<MoLabel> labelBag, Criteria criteria) {

		int dominatedBy = 0;
		boolean insert = false;
		for (int i = labelBag.size()-1; i > -1; i --) {
			MoLabel mol = labelBag.get(i);
			if (this.compareTo(mol, criteria) == -1) {
				labelBag.remove(i);
				insert = true;
			}
			else if (dominatedBy == 0) {
				if (this.compareTo(mol, criteria) == 1) {
					dominatedBy ++;
				}
			}
		}
		
		
		
		if (dominatedBy == 0 || insert) {
			labelBag.add(this);
			return true;
		}
		
		return false;
	}
	
	
	
	public void printMe() {
		System.out.println("Trip " + tripId + " from stop " + prevId + " at "
				+ TimeTransformer.IntegerToString(departureTime.time) + " on " + departureTime.day
				+ " to stop " + stopId + " at "
				+ TimeTransformer.IntegerToString(ovs.getArrivalTime().time)
				+ ", number of transfers = " + ovs.getNumOfTransfers());
	}
}

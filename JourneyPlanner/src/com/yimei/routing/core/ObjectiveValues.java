package com.yimei.routing.core;

import com.yimei.routing.query.QueryTime;

/***
 * The objective values of the journey planning.
 * @author e04499
 *
 */

public class ObjectiveValues {

	private QueryTime arrivalTime;
	private int numOfTransfers;
	private double totalWalkingDistance;
	private QueryTime nextDepatureTime; // this is the time of departure of this stop, instead of that of the previous stop (departureTime in Label)
	
	public ObjectiveValues(QueryTime at, int nt, double twd, QueryTime ndt) {
		this.arrivalTime = at;
		this.numOfTransfers = nt;
		this.totalWalkingDistance = twd;
		this.nextDepatureTime = ndt;
	}
	
	public QueryTime getArrivalTime() {
		return arrivalTime;
	}
	
	public int getNumOfTransfers() {
		return numOfTransfers;
	}
	
	public double getTotalWalkingDistance() {
		return totalWalkingDistance;
	}
	
	public QueryTime getNextDepartureTime() {
		return nextDepatureTime;
	}
	
	public void setNumOfTransfers(int nt) {
		this.numOfTransfers = nt;
	}
	
	public void setNextDepartureTime(QueryTime ndt) {
		this.nextDepatureTime = ndt;
	}

	
	// if ovs is the upper bound
	public void updateUb(ObjectiveValues ovs) {
		if (this.arrivalTime.earlierThan(ovs.getArrivalTime())) {
			this.arrivalTime = ovs.getArrivalTime();
		}
		
		if (ovs.getNumOfTransfers() > this.numOfTransfers) {
			this.numOfTransfers = ovs.getNumOfTransfers();
		}
		
		if (ovs.getTotalWalkingDistance() > this.totalWalkingDistance) {
			this.totalWalkingDistance = ovs.getTotalWalkingDistance();
		}
	}		

	public static ObjectiveValues MAX_VALUE() {
		ObjectiveValues max = new ObjectiveValues(QueryTime.MAX_VALUE(), Integer.MAX_VALUE, Double.MAX_VALUE, QueryTime.MAX_VALUE());
		return max;
	}
	
	// if ovs is the lower bound
	public void updateLb(ObjectiveValues ovs) {
		if (ovs.getArrivalTime().earlierThan(this.arrivalTime)) {
			this.arrivalTime = ovs.getArrivalTime();
		}
		
		if (ovs.getNumOfTransfers() < this.numOfTransfers) {
			this.numOfTransfers = ovs.getNumOfTransfers();
		}
		
		if (ovs.getTotalWalkingDistance() < this.totalWalkingDistance) {
			this.totalWalkingDistance = ovs.getTotalWalkingDistance();
		}
		
	}
	
	// -1 if it dominates cmpOvs, 1 if it is dominated by cmpOvs, and 0 if both are nondominated
	public int compareTo (ObjectiveValues cmpOvs, Criteria criteria) {
		int better = 0;
		int worse = 0;
		
		if (criteria.getEarliestArrival()) {
			if (this.arrivalTime.earlierThan(cmpOvs.getArrivalTime())) {
				better ++;
			}
			else if (cmpOvs.getArrivalTime().earlierThan(this.arrivalTime)) {
				worse ++;
			}
		}
		
		if (criteria.getMinTransfer()) {
			if (this.numOfTransfers < cmpOvs.getNumOfTransfers()) {
				better ++;
			}
			else if (this.numOfTransfers > cmpOvs.getNumOfTransfers()) {
				worse ++;
			}
		}
		
		if (criteria.getLatestDepature()) {
			
		}
		
		if (criteria.getLeastWalkingDistance()) {
			
		}
		
		if (better > 0) {
			if (worse > 0) {
				return 0;
			}
			else { // worse = 0
				return -1;
			}
		}
		else { // better = 0
			if (worse > 0) {
				return 1;
			}
			else { // worse = 0
				return 0;
			}
		}
	}
}

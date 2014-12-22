package com.yimei.routing.core;

/***
 * The criteria to rank the journeys
 * @author e04499
 *
 */

public class Criteria {

	private boolean earliestArrival;
	private boolean minTransfer; // min number of transfers
	private boolean latestDeparture; // used for real-time update or something else
	private boolean leastWalkingDistance;
	
	// default, earliest arrival journey
	public Criteria () {
		this.earliestArrival = true;
		this.minTransfer = false;
		this.latestDeparture = false;
		this.leastWalkingDistance=  false;
	}
	
	public Criteria (boolean ea, boolean mt, boolean ld, boolean lwd) {
		this.earliestArrival = ea;
		this.minTransfer = mt;
		this.latestDeparture = ld;
		this.leastWalkingDistance=  lwd;
	}
	
	public boolean getEarliestArrival () {
		return earliestArrival;
	}
	
	public boolean getMinTransfer () {
		return minTransfer;
	}
	
	public boolean getLatestDepature () {
		return latestDeparture;
	}
	
	public boolean getLeastWalkingDistance () {
		return leastWalkingDistance;
	}
}

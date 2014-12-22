package com.yimei.graph;

/***
 * This is the temporal travel time from one station to another. 
 * @author e04499
 *
 */

public class TemporalTravelTime {
	public static int constantTimePoint = -Integer.MAX_VALUE;
	
	private int timePoint; // in seconds
	private int value;
	private ArcWeightInfo info;
	
	public TemporalTravelTime(int tp, int val, ArcWeightInfo info) {
		this.timePoint = tp;
		this.value = val;
		this.info = info;
	}
	
	public int getTimePoint() {
		return timePoint;
	}
	
	public int getValue() {
		return value;
	}
	
	public ArcWeightInfo getInfo() {
		return info;
	}
}

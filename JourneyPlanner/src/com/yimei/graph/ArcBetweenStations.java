package com.yimei.graph;

/***
 * This is the arc between stations, with the weights (e.g., traveling time) as time-dependent functions instead of fixed values
 * @author e04499
 *
 */

public class ArcBetweenStations extends Arc
{
	TravelTimeFunc travelTimeFunc;
	
	/* constructor */
	
	public ArcBetweenStations(int id, Vertex v1, Vertex v2) {
		super(id, v1, v2);
		travelTimeFunc = new TravelTimeFunc();
	}
	
	public ArcBetweenStations(int id, Vertex v1, Vertex v2, TravelTimeFunc ttFunc) {
		super(id, v1, v2);
		travelTimeFunc = ttFunc;
	}
	
	/* methods */
	
	public TravelTimeFunc getTravelTimeFunc() {
		return travelTimeFunc;
	}
	
	public int travelTimeFromNow(String dayLabel, int now) {
		return travelTimeFunc.travelTimeFromNow(dayLabel, now);
	}
}

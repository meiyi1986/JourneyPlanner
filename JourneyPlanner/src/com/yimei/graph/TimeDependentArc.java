package com.yimei.graph;

/***
 * This is an arc with time-dependent travel time function
 * @author e04499
 *
 */

public class TimeDependentArc extends Arc
{
	TravelTimeFunc travelTimeFunc;
	
	/* constructor */
	
	public TimeDependentArc(int id, Vertex v1, Vertex v2) {
		super(id, v1, v2);
		travelTimeFunc = new TravelTimeFunc();
	}
	
	public TimeDependentArc(int id, Vertex v1, Vertex v2, TravelTimeFunc ttFunc) {
		super(id, v1, v2);
		travelTimeFunc = ttFunc;
	}
	
	/* methods */
	
	public TravelTimeFunc getTravelTimeFunc() {
		return travelTimeFunc;
	}
	
	public int travelTimeFromNow(String day, int now) {
		return travelTimeFunc.travelTimeFromNow(day, now);
	}
}

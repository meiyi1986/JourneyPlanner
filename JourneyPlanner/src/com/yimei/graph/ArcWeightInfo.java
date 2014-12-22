package com.yimei.graph;

/***
 * This is the information of a weight of an arc, e.g., the bus no. or name, route description
 * @author e04499
 *
 */

public class ArcWeightInfo {
	public String routeDesc; // route description
	
	/* constructor */
	public ArcWeightInfo(String rd) {
		this.routeDesc = rd;
	}
}

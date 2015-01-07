package com.yimei.routing.raptor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;
import com.yimei.routing.core.Location;
import com.yimei.routing.core.TransferForSearch;

/***
 * A stop for search contains a set of routes serving it, along with the set of stops that are reachable with foot-path
 * @author e04499
 *
 */

public class RaptorStop {

	String id; // the stop id
	Location loc;
	List<String> sameNameIds; // the stop ids with the same name (especially for train stations)
	Map<String, List<Integer>> routeIdx; // the routes serving the stop
	
	/* constructor */
	
	public RaptorStop(String id) {
		this.id = id;
		this.sameNameIds = new ArrayList<String>();
		this.routeIdx = Maps.newHashMap();
	}
	
	public RaptorStop(String id, double lat, double lon) {
		this.id = id;
		this.loc = new Location(lat, lon, null);
		this.sameNameIds = new ArrayList<String>();
		this.routeIdx = Maps.newHashMap();
	}
	
	public RaptorStop(String id, Map<String, List<Integer>> routeIdx) {
		this.id = id;
		this.sameNameIds = new ArrayList<String>();
		this.routeIdx = routeIdx;
	}
	
	/* methods */
	
	public String getId() {
		return id;
	}
	
	public Location getLocation() {
		return loc;
	}
	
	public List<String> getSameNameIds() {
		return sameNameIds;
	}
	
	public Collection<String> getRoutes() {
		return routeIdx.keySet();
	}
	
	public List<Integer> getIdxOfRoute(String routeId) {
		return routeIdx.get(routeId);
	}
	
	public int getFirstIdxOfRoute(String routeId) {
		return routeIdx.get(routeId).get(0);
	}
	
	public int getLastIdxOfRoute(String routeId) {
		return routeIdx.get(routeId).get(routeIdx.get(routeId).size()-1);
	}
	
	public void addRoute(String routeId, List<Integer> idx) {
		routeIdx.put(routeId, idx);
	}
	
	public void addRouteIdx(String routeId, int idx) {
		if (routeIdx.containsKey(routeId)) {
			routeIdx.get(routeId).add(new Integer(idx));
		}
		else
		{
			List<Integer> list = new ArrayList<Integer>();
			list.add(new Integer(idx));
			routeIdx.put(routeId, list);
		}
	}
}

package com.yimei.routing.core;

import java.util.ArrayList;
import java.util.List;
import org.mapdb.Fun.Tuple2;

/***
 * This is the reverse profile, storing the profile from each stop to the target stop for every possible arrival
 * @author e04499
 *
 */

public class ProfileToTarget {

	private String toStopId; // the target stop
	List<Tuple2<Integer, Integer>> profileList; // (departure time, arrival time) pairs, ordered by decreasing arrival time
	
	public ProfileToTarget(String toStopId) {
		this.toStopId = toStopId;
		this.profileList = new ArrayList<Tuple2<Integer, Integer>> ();
	}
	
	public ProfileToTarget(String toStopId, List<Tuple2<Integer, Integer>> profileList) {
		this.toStopId = toStopId;
		this.profileList = profileList;
	}
	
	public String getToStopId() {
		return toStopId;
	}
	
	public List<Tuple2<Integer, Integer>> getProfileList() {
		return profileList;
	}
	
	public Tuple2<Integer, Integer> getProfile(int index) {
		return profileList.get(index);
	}
	
	public int getProfileDepartureTime(int index) {
		return profileList.get(index).a;
	}
	
	public int getProfileArrivalTime(int index) {
		return profileList.get(index).b;
	}
	
	public void addToProfile(int dt, int at) {
		Tuple2<Integer, Integer> tuple2 = new Tuple2<Integer, Integer>(dt, at);
		profileList.add(tuple2);
	}
}

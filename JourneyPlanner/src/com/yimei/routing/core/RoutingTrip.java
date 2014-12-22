package com.yimei.routing.core;

import java.util.ArrayList;
import java.util.List;

public class RoutingTrip {
	
	private String id;
	private List<String> workingDays; // the working days of the route
	
	public RoutingTrip(String id) {
		this.id = id;
		workingDays = new ArrayList<String>();
	}
	
	public RoutingTrip(String id, List<String> wd) {
		this.id = id;
		this.workingDays = wd;
	}
	
	public String getId() {
		return id;
	}
	
	public List<String> getWorkingDays() {
		return workingDays;
	}
	
	public boolean workingOn(String day) {
		return workingDays.contains(day);
	}
	
	public void setWorkingDays(List<String> wd) {
		this.workingDays = wd;
	}
	
	public void printMe() {
		System.out.println("Trip " + id + " works on ");
		for (String wd : workingDays) {
			System.out.print(wd + " ");
		}
		System.out.println("");
	}
}

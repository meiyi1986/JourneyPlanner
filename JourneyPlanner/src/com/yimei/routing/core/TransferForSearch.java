package com.yimei.routing.core;

public class TransferForSearch {

	private String fromStopId;
	private String toStopId;
	private double distance;
	private int transferTime; // in seconds
	
	public TransferForSearch(String fromStopId, String toStopId, double distance, int transferTime) {
		this.fromStopId = fromStopId;
		this.toStopId = toStopId;
		this.distance = distance;
		this.transferTime = transferTime;
	}
	
	public String getFromStopId() {
		return fromStopId;
	}
	
	public String getToStopId() {
		return toStopId;
	}
	
	public double getDistance() {
		return distance;
	}
	
	public int getTransferTime() {
		return transferTime;
	}
}

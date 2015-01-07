package com.yimei.routing.core;

import java.util.ArrayList;
import java.util.List;

import com.yimei.routing.query.QueryTime;

/**
 * The label bag, including the set of nondominated labels, and the lower bound of each objective value
 * @author e04499
 *
 */

public class LabelBag {

	private List<Label> labelSet;
	QueryTime arrivalTimeLb;
	int numOfTripsLb;
	
	public LabelBag() {
		labelSet = new ArrayList<Label>();
		arrivalTimeLb = QueryTime.MAX_VALUE();
		numOfTripsLb = Integer.MAX_VALUE;
	}
	
	public List<Label> getLabelSet() {
		return labelSet;
	}
	
	public Label getLabel(int index) {
		return labelSet.get(index);
	}
	
	public QueryTime getArrivalTimeLb() {
		return arrivalTimeLb;
	}
	
	public int getNumOfTripLb() {
		return numOfTripsLb;
	}
	
	public void addLabel(Label label) {
		labelSet.add(label);
	}
	
	public void addLabel(int index, Label label) {
		labelSet.add(index, label);
	}
	
	public void removeLabel(int index) {
		labelSet.remove(index);
	}
	
	public void removeLabel(Label label) {
		labelSet.remove(label);
	}
	
	public boolean isEmpty() {
		return labelSet.isEmpty();
	}
	
	public boolean dominatedBy(Label label) {
		if (label.getArrivalTime().earlierThan(arrivalTimeLb)) {
			if (label.getNumOfTrips() <= numOfTripsLb){
				return true;
			}
		}
		else if (label.getArrivalTime().equals(arrivalTimeLb)) {
			if (label.getNumOfTrips() < numOfTripsLb) {
				return true;
			}
		}
		
		return false;
	}
	
	public void updateLb(Label newLabel) {
		if (newLabel.getArrivalTime().earlierThan(arrivalTimeLb)) {
			arrivalTimeLb = newLabel.getArrivalTime();
		}
		
		if (newLabel.getNumOfTrips() < numOfTripsLb) {
			numOfTripsLb = newLabel.getNumOfTrips();
		}
	}
	
	
	
	
	public boolean updateWithLabel(Label newLabel) {
		
		updateLb(newLabel);

		int dominatedBy = 0;
		boolean insert = false;
		for (int i = labelSet.size()-1; i > -1; i --) {

			if (newLabel.compareTo(labelSet.get(i)) == -1) {
				labelSet.remove(i);
				insert = true;
			}
			else if (dominatedBy == 0) {
				if (newLabel.compareTo(labelSet.get(i)) == 1) {
					dominatedBy ++;
				}
			}
		}
		
//		System.out.println("label set:");
//		for (MoLabel label : labelSet) {
//			label.printMe();
//		}
//		System.out.println("new label:");
//		newLabel.printMe();
//		System.out.println("updated set:");
//		for (MoLabel label : labelSet) {
//			label.printMe();
//		}
//		new java.util.Scanner(System.in).nextLine();
		
		boolean inserted = false;
		
		if (dominatedBy == 0 || insert) {
			labelSet.add(newLabel);
			inserted = true;
		}
		
		
		return inserted;
		
		
	}
	
	
	public void printLabelSet() {
		for (Label label : labelSet) {
			label.printMe();
		}
	}
}
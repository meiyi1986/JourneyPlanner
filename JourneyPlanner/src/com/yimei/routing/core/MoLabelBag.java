package com.yimei.routing.core;

import java.util.ArrayList;
import java.util.List;

/**
 * The label bag, including the set of nondominated labels, and the lower bound of each objective value
 * @author e04499
 *
 */

public class MoLabelBag {

	private List<MoLabel> labelSet;
	private ObjectiveValues ovsLb; // lower bound
	
	public MoLabelBag() {
		labelSet = new ArrayList<MoLabel>();
		ovsLb = null;
	}
	
	public List<MoLabel> getLabelSet() {
		return labelSet;
	}
	
	public MoLabel getLabel(int index) {
		return labelSet.get(index);
	}
	
	public ObjectiveValues getOvsLb() {
		return ovsLb;
	}
	
	public void addLabel(MoLabel mol) {
		labelSet.add(mol);
	}
	
	public void addLabel(int index, MoLabel mol) {
		labelSet.add(index, mol);
	}
	
	public void removeLabel(int index) {
		labelSet.remove(index);
	}
	
	public void removeLabel(MoLabel mol) {
		labelSet.remove(mol);
	}
	
	public boolean isEmpty() {
		return labelSet.isEmpty();
	}
	
	public void updateLb(MoLabel newLabel) {
		if (ovsLb == null) {
			ovsLb = newLabel.getObjectiveValues();
			return;
		}
		
		ovsLb.updateLb(newLabel.getObjectiveValues());
	}
	
	
	public boolean updateWithLabel(MoLabel newLabel, Criteria criteria) {

		int dominatedBy = 0;
		boolean insert = false;
		for (int i = labelSet.size()-1; i > -1; i --) {
			MoLabel mol = labelSet.get(i);
			if (newLabel.compareTo(mol, criteria) == -1) {
				labelSet.remove(i);
				insert = true;
			}
			else if (dominatedBy == 0) {
				if (newLabel.compareTo(mol, criteria) == 1) {
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
		for (MoLabel label : labelSet) {
			label.printMe();
		}
	}
}
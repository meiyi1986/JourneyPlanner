package com.yimei.routing.csa;

import java.util.ArrayList;
import java.util.List;

import com.yimei.routing.core.TransferForSearch;

public class CsaStop {

	private String id; // stop id
	private List<Integer> connectionIndex; // the connection indices departing from this stop
	
	public CsaStop(String id) {
		this.id = id;
		this.connectionIndex = new ArrayList<Integer>();
	}
	
	public String getId() {
		return id;
	}
	
	public List<Integer> getConnectionIds() {
		return connectionIndex;
	}
	
	public void addConnection(int c) {
		connectionIndex.add(new Integer(c));
	}
}

package au.com.jakebarnes.JourneyPlanner;


import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.yimei.sql.ptvdbJDBC;
import com.yimei.test.API;
import com.yimei.util.Utc;

import java.sql.*;

/***
 * The Pattern First Search algorithm, from the following reference:
 * R. Huang, "A Schedule-based Pathfinding Algorithm for Transit Networks Using Pattern First Search"
 * @author e04499 - Yi Mei
 *
 */

public class PFS {
	
	public static String Search(String from, String to, String utc) throws Exception {
		long startTime = System.currentTimeMillis();
		
		String journeys;
		// open a connection to the ptvdb sql database
		Connection conn = null;
		
		try {
			conn = ptvdbJDBC.connectDatabase();
			journeys = sqlSearch(from, to, utc, conn);
			
			
		} finally {
			if (conn != null) conn.close();
		}
		
		long endTime   = System.currentTimeMillis();
		long totalTime = endTime - startTime;
		System.out.println(totalTime);
		
		return journeys;
	}
	
	public static String sqlSearch(String from, String to, String utc, Connection conn) throws Exception {
		// get the JSON object of the origin and destination
		JSONObject jsonFrom = new JSONObject(from);
		JSONObject jsonTo = new JSONObject(to);
		
		// get the mode and stop ids of from and to
		int fromModeId = API.getModeId(jsonFrom.getString("transport_type"));
		int fromStopId = jsonFrom.getInt("stop_id");
		int toModeId = API.getModeId(jsonTo.getString("transport_type"));
		int toStopId = jsonTo.getInt("stop_id");
		
		// get all the key lines and stops between the origin and destination based on sql
		ArrayList<Integer> keyLineIds = new ArrayList<Integer>();
		ArrayList<Integer> keyStopIds = new ArrayList<Integer>();
		
		
		
		JSONArray journeys = new JSONArray();
		JSONArray journey = new JSONArray();
		
//		System.out.println(fromStopId + " " + toStopId);
		
		// initialize an empty network
		ArrayList<MPFSNode> MPFSNetwork = new ArrayList<MPFSNode>();
		// initialize the empty list of the closed patterns
		ArrayList<Integer> closedPatterns = new ArrayList<Integer>();
		
		// initialize the transfer nodes for from and to and add them to the network
		MPFSNode fromNode = new MPFSNode();
		fromNode.initialize(fromModeId, fromStopId, jsonFrom.getString("location_name"), MPFSNetwork.size(), utc, true);
		MPFSNetwork.add(fromNode);
		MPFSNode toNode = new MPFSNode();
		toNode.initialize(toModeId, toStopId, jsonTo.getString("location_name"), MPFSNetwork.size(), utc, false);
		MPFSNetwork.add(toNode);
		
		System.out.println(fromNode.stopId + " " + fromNode.networkIdx);
		System.out.println(toNode.stopId + " " + toNode.networkIdx);
		
		// create the open set (the network indices), which is initialized to consist of the from node
		ArrayList<Integer> openSet = new ArrayList<Integer>();
		openSet.add(fromNode.networkIdx);
		
		while (openSet.size() > 0) {
			// select the node with the earliest arrival time in the open set
			int k = bestNodeIndex(openSet, MPFSNetwork, toNode.getNetworkIdx());
			if (k == -1) {
				k = 0; // select the first node arbitrarily, since all nodes are the same bad
			}
			int currIdx = openSet.get(k); // the index of the current node in the network
			
			// the destination is reached, stop expanding the network
			if (currIdx == toNode.networkIdx) {
				journey = generateJourney(currIdx, fromNode.networkIdx, MPFSNetwork);
				break;
			}
			
			MPFSNode currNode = MPFSNetwork.get(currIdx);
			// update the current node with the new arrival time
			currNode.updatePatterns();
			
			// add the patterns of the current node into closed list
			for (int i = 0; i < currNode.getPatterns().length(); i++) {
				int tmpPatternId = currNode.getPattern(i).getJSONObject("platform").getJSONObject("direction").getInt("linedir_id");
				if (!closedPatterns.contains(tmpPatternId)) {
					closedPatterns.add(new Integer(tmpPatternId));
				}
			}
			
			System.out.println("current stop: " + currNode.stopId + " " + currNode.stopName + " " + currNode.getObject().arrivalTime);
//			System.out.print("closed patterns: ");
//			for (int i = 0; i < closedPatterns.size(); i++) {
//				System.out.print(closedPatterns.get(i) + " ");
//			}
//			System.out.print("\n");
			
			// find all the patterns of currIdx satisfying certain conditions
			for (int i = 0; i < currNode.getPatterns().length(); i++) {
				JSONObject tmpPattern = currNode.getPattern(i);
				
				int tmpPatternId = tmpPattern.getJSONObject("platform").getJSONObject("direction").getInt("linedir_id");
				
				if (tmpPatternId == currNode.getObject().patternId) { // the same pattern id, skip
					continue;
				}
				
				long freeTime = Utc.compare(tmpPattern.getString("time_timetable_utc"), currNode.getObject().arrivalTime);
//				System.out.println(freeTime + " " + currNode.getObject().walkingTime);
				if (freeTime < 1000 * currNode.getObject().walkingTime) { // not enough transfer time, skip
					continue;
				}
				
				if (stopInPattern(currNode.getObject().getPrevStopId(), tmpPattern)) { // the previous stop is in this pattern, skip
					continue;
				}
				
				// get all the stops in this pattern
				JSONArray tmpPatternStops = findAllPatternStops(tmpPattern);
				
//				if (stopInArray(currNode.getObject().prevStopId, tmpPatternStops)) { // the previous stop is in this pattern, skip
//					continue;
//				}
				
//				// check if each stop in this pattern is a transfer
//				ArrayList<Boolean> iTList = isTransferInPattern(tmpPatternStops);
				
//				for (int j = 0; j < tmpPatternStops.length(); j++) {
//					System.out.print(tmpPatternStops.getJSONObject(j).getJSONObject("platform").getJSONObject("stop").getString("location_name"));
//					System.out.print(" " + iTList.get(j) + "\n");
//				}
				
				// get the position of the current stop in this pattern
				int currIdxInPattern = indexOfStopInArray(currNode.stopId, tmpPatternStops);
				
				// find all the subsequent transfer nodes starting from the current stop in this pattern
				for (int j = currIdxInPattern+1; j < tmpPatternStops.length(); j++) {
					JSONObject tmpPatternStop = tmpPatternStops.getJSONObject(j).getJSONObject("platform").getJSONObject("stop");
					
//					System.out.println("tmp stop: " + tmpPatternStop.getString("location_name"));
					
					// check if the stop is a transfer node or the destination
					if (stopIsIntraTransfer(tmpPatternStop, conn) || tmpPatternStop.getInt("stop_id") == toNode.getStopId()) {
//						System.out.println("tmp stop: " + tmpPatternStop.getString("location_name"));
						// find the index of this stop in the network
						k = indexOfStopInNetwork(tmpPatternStop.getInt("stop_id"), MPFSNetwork);
						if (k == -1) { // a new stop
							// create and initialize the node
							int tmpModeId = API.getModeId(tmpPatternStop.getString("transport_type"));
							MPFSNode tmpNode = new MPFSNode();
							tmpNode.initialize(tmpModeId, tmpPatternStop.getInt("stop_id"), tmpPatternStop.getString("location_name"), MPFSNetwork.size(), utc, false);
							
//							System.out.println("finish initialization");
							
							MPFSNetwork.add(tmpNode);
							// update the object of the node
							updateMPFSNodeObject(tmpNode.networkIdx, currNode.networkIdx, MPFSNetwork, tmpPatternStops.getJSONObject(j), tmpPatternStops.getJSONObject(currIdxInPattern));
							
							System.out.println("arrival time of " + tmpNode.stopName + " updated to " + tmpNode.getObject().arrivalTime);
							
							// add this node to the open set
							openSet.add(tmpNode.networkIdx);
						}
						else { // already in the network
							// update the object of the node
							boolean updated = updateMPFSNodeObject(k, currNode.networkIdx, MPFSNetwork, tmpPatternStops.getJSONObject(j), tmpPatternStops.getJSONObject(currIdxInPattern));
							
							if (updated) {
								System.out.println("arrival time of " + MPFSNetwork.get(k).stopName + " updated to " + MPFSNetwork.get(k).getObject().arrivalTime);
								if (!openSet.contains(MPFSNetwork.get(k).networkIdx)) {
									openSet.add(MPFSNetwork.get(k).networkIdx);
								}
							}
							
//							System.out.println("2 Open set:");
//							for (int n = 0; n < openSet.size(); n++) {
//								System.out.print(MPFSNetwork.get(openSet.get(n)).stopName + " ");
//							}
//							System.out.print("\n");
						}
					}
				}				
			}
			
			openSet.remove(new Integer(currIdx));
			
			System.out.println("Open set:");
			for (int n = 0; n < openSet.size(); n++) {
				System.out.print(MPFSNetwork.get(openSet.get(n)).stopName + " ");
			}
			System.out.print("\n");
		}
		
		System.out.println("finish");		
		
		journeys.put(journey);
					
		return journeys.toString();
	}
	
	// search for the key lines and stops
	public static void keyLineSearch(ArrayList<Integer> keyLineIds, int fromStopId, int toStopId, Connection conn) throws SQLException {
		Statement stmt = conn.createStatement();
		String sql;
		
		
	}
		
	// find all the patterns given the modeId, stopId and utc
	public static JSONArray findAllPatterns(int modeId, int stopId, String utc) throws Exception {
		// find the next departures of the current stop
		
		String jsonStrBND = API.doPTVBroadNextDepartures(modeId, stopId, 30);
		
//		System.out.println(jsonStrBND);
		
		JSONObject jsonBND = new JSONObject(jsonStrBND);
		JSONArray jsonDepartures = jsonBND.getJSONArray("values");
		
		// only keep the earliest run of each line and direction
		ArrayList<Integer> usedLinedirIds = new ArrayList<Integer>();
		JSONArray jsonEarliestDepartures = new JSONArray();
		
		for (int i = 0; i < jsonDepartures.length(); i++) {
			JSONObject currJsonDep = jsonDepartures.getJSONObject(i);
			JSONObject currJsonPlatform = currJsonDep.getJSONObject("platform");
			int linedirId = currJsonPlatform.getJSONObject("direction").getInt("linedir_id");
			String currUtc = currJsonDep.getString("time_timetable_utc");
			long diff = Utc.compare(currUtc, utc);
//			System.out.println(linedirId + " " + diff);
			if (diff < 0) { // currUtc is earlier
				continue;
			}
			
			if (usedLinedirIds.contains(linedirId)) { // this line and direction already used, skip
				continue;
			}
			
			jsonEarliestDepartures.put(currJsonDep);
			usedLinedirIds.add(linedirId);
		}
		
//		System.out.println(jsonEarliestDepartures.toString());
		
		return jsonEarliestDepartures;
	}
	
	// check if a stop is an intra-transfer: the stop is the API stop JSONObject
	public static boolean stopIsIntraTransfer(JSONObject stop, Connection conn) throws Exception {
		boolean iit = false;
		
		Statement stmt = conn.createStatement();
		String sql = "SELECT * FROM ptv_stops " + 
				"WHERE ID = " +
				stop.getInt("stop_id");
		ResultSet rs = stmt.executeQuery(sql);
		
		while (rs.next()) {
			if (rs.getString("IntraTransfer").equals("Y")) {
				iit = true;
				break;
			}
		}
		
		stmt.close();
		return iit;
	}
	
	// check if each stop in the pattern is a transfer stop or not
	public static ArrayList<Boolean> isTransferInPattern(JSONArray patternStops) throws Exception {
		// get the number of patterns for each stop in this pattern
		ArrayList<Integer> patternNum = new ArrayList<Integer>();
		for (int i = 0; i < patternStops.length(); i++) {
			int tmpModeId = API.getModeId(patternStops.getJSONObject(i).getJSONObject("platform").getJSONObject("stop").getString("transport_type"));
			int tmpStopId = patternStops.getJSONObject(i).getJSONObject("platform").getJSONObject("stop").getInt("stop_id");
			String tmpUtc = patternStops.getJSONObject(i).getString("time_timetable_utc");
			JSONArray tmpAllPatterns = findAllPatterns(tmpModeId, tmpStopId, tmpUtc);
			
			int tmpNum = tmpAllPatterns.length();
			patternNum.add(new Integer(tmpNum));
		}
		
		ArrayList<Boolean> iTList = new ArrayList<Boolean>();
		if (patternNum.get(0) > patternNum.get(1)) {
			iTList.add(true);
		}
		else {
			iTList.add(false);
		}
		
		for (int i = 1; i < patternNum.size()-1; i++) {
			if (patternNum.get(i) > patternNum.get(i-1) || patternNum.get(i) > patternNum.get(i+1)) {
				iTList.add(true);
			}
			else {
				iTList.add(false);
			}
		}
		
		if (patternNum.get(patternNum.size()-1) > patternNum.get(patternNum.size()-2)) {
			iTList.add(true);
		}
		else {
			iTList.add(false);
		}
		
//		System.out.println(patternStops.length() + " " + iTList.size());
		
		return iTList;
	}
	
	// find all the stops of a pattern in order of its run
	public static JSONArray findAllPatternStops(JSONObject pattern) throws Exception {
		// find all the stops in this pattern (by run mode and id)
		int runModeId = API.getModeId(pattern.getJSONObject("run").getString("transport_type"));
		int runId = pattern.getJSONObject("run").getInt("run_id");
		int stopId = pattern.getJSONObject("platform").getJSONObject("stop").getInt("stop_id");
		String utc = pattern.getString("time_timetable_utc");
		
		String jsonStr = API.doPTVRunStops(runModeId, runId, stopId, utc);
		
		JSONObject jsonRS = new JSONObject(jsonStr);
		JSONArray jsonPatternStops = jsonRS.getJSONArray("values");
		
		return jsonPatternStops;
	}
	
	// find the index of the best node in the set (earliest arrival time)
	public static int bestNodeIndex(ArrayList<Integer> set, ArrayList<MPFSNode> network, int toNetworkIdx) {
		String ert = "NULL";
		int k = -1;
		for (int i = 0; i < set.size(); i++) {
//			if (set.get(i) == toNetworkIdx) { // reach the destination
//				return i;
//			}
//			
			String tmpRt = network.get(set.get(i)).getObject().arrivalTime;
			long diff = Utc.compare(tmpRt, ert);
//			System.out.println("i = " + i + ", curr utc = " + ert + " tmp utc = " + tmpRt + ", diff = " + diff);
			if (diff < 0) { // tmpRt is earlier
				ert = tmpRt;
				k = i;
			}
		}
		
//		System.out.println(k);
		
		return k;
	}
	
	// check if a pattern (departure JSONObject) has a stop id based on sql database
	public static boolean stopInPattern(int stopId, JSONObject jsonPattern) throws JSONException, SQLException, ClassNotFoundException {
		int lineId = jsonPattern.getJSONObject("platform").getJSONObject("direction").getJSONObject("line").getInt("line_id");
		
		// connect to ptvdb database
		Connection conn = null;
		Statement stmt = null;
		boolean sip = false;
		try {
			conn = ptvdbJDBC.connectDatabase();
			sip = ptvdbJDBC.stopInLine(stopId, lineId, conn);
		} finally {
			if (stmt != null) stmt.close();
			if (conn != null) conn.close();
		}
		
		return sip;
	}
	
	// check if a pattern has a stop id
	public static boolean stopInArray(int stopId, JSONArray stopArray) throws JSONException {
		for (int i = 0; i < stopArray.length(); i++) {
			int tmpStopId = stopArray.getJSONObject(i).getJSONObject("platform").getJSONObject("stop").getInt("stop_id");
			if (tmpStopId == stopId) {
				return true;
			}
		}
		
		return false;
	}
	
	// get the index of a stop in an array, -1 if the stop is not in the array
	public static int indexOfStopInArray(int stopId, JSONArray stopArray) throws JSONException {
		int k = -1;
		for (int i = 0; i < stopArray.length(); i++) {
			int tmpStopId = stopArray.getJSONObject(i).getJSONObject("platform").getJSONObject("stop").getInt("stop_id");
			if (tmpStopId == stopId) {
				k = i;
				break;
			}
		}
		
		return k;
	}
	
	// check if the stop is in the network
	public static boolean stopInNetwork(int stopId, ArrayList<MPFSNode> network) {
		for (int i = 0; i < network.size(); i++) {
			if (network.get(i).stopId == stopId) {
				return true;
			}
		}
		
		return false;
	}
	
	// get the index of the stop in the network, -1 if the stop is not in the network
	public static int indexOfStopInNetwork(int stopId, ArrayList<MPFSNode> network) {
		int k = -1;
		for (int i = 0; i < network.size(); i++) {
			if (network.get(i).stopId == stopId) {
				k = i;
				break;
			}
		}
		
		return k;
	}
	
	// update the object when given a new pattern stop series (preIdx is the index of the previous stop, and currIdx is the index of this stop)
	public static boolean updateMPFSNodeObject(int currNetworkIdx, int prevNetworkIdx, ArrayList<MPFSNode> network, JSONObject currPatternStop, JSONObject prevPatternStop) throws JSONException {
		String oldArrivalTime = network.get(currNetworkIdx).getObject().arrivalTime;
		String newArrivalTime = currPatternStop.getString("time_timetable_utc");
		
		long diff = Utc.compare(newArrivalTime, oldArrivalTime);
		if (diff < 0) { // the new arrival time is earlier, replace it
			int newPID = currPatternStop.getJSONObject("platform").getJSONObject("direction").getInt("linedir_id");
			int newPSID = prevPatternStop.getJSONObject("platform").getJSONObject("stop").getInt("stop_id");
			String newPDT = prevPatternStop.getString("time_timetable_utc");
			
			network.get(currNetworkIdx).getObject().setArrivalTime(newArrivalTime);
			network.get(currNetworkIdx).getObject().setPatternId(newPID);
			network.get(currNetworkIdx).getObject().setPrevStopId(newPSID);
			network.get(currNetworkIdx).getObject().setPreNetworkIdx(prevNetworkIdx);
			network.get(prevNetworkIdx).getObject().setcurrDeparture(prevPatternStop);
			network.get(prevNetworkIdx).getObject().setPrevDepTime(newPDT);
			network.get(currNetworkIdx).getObject().setcurrDeparture(currPatternStop);
			
			return true;
		}
		
		return false;
	}
	
	// generate a journey based on the current network and the idx of the destination
	public static JSONArray generateJourney(int toIdx, int fromIdx, ArrayList<MPFSNode> MPFSNetwork) throws JSONException {
		JSONArray jsonJourney = new JSONArray();
		
		jsonJourney.put(MPFSNetwork.get(toIdx).getObject().currDeparture.getJSONObject("platform").getJSONObject("stop"));
		int currIdx = toIdx;
		while (currIdx != fromIdx) {
			int prevIdx = MPFSNetwork.get(currIdx).getObject().prevNetworkIdx;
//			jsonJourney.put(0, MPFSNetwork.get(prevIdx).getObject().currDeparture);
			JSONArray tmpJourney = new JSONArray();
			tmpJourney.put(MPFSNetwork.get(prevIdx).getObject().currDeparture);
			for (int i = 0; i < jsonJourney.length(); i++) {
				tmpJourney.put(jsonJourney.getJSONObject(i));
			}
			jsonJourney = tmpJourney;
			
//			System.out.println(jsonJourney.toString());
			
			currIdx = prevIdx;
		}
		
		return jsonJourney;
	}
	
	public static class MPFSNode {
		int modeId; // the mode id of this transfer node
		int stopId; // the stop id of the node
		String stopName; // the stop name
		int networkIdx; // the index of this node in the search network
		private JSONArray patterns; // the available patterns (the earliest departures of each unique line and direction)
		MPFSNodeObject object;
		
		public MPFSNode () {
			
		}
		
		public MPFSNode (MPFSNode node) {
			this.modeId = node.modeId;
			this.stopId = node.stopId;
			this.stopName = node.stopName;
			this.networkIdx = node.networkIdx;
			this.patterns = node.patterns;
			this.object = node.object;
		}
		
		public MPFSNode (int modeId, int stopId, String stopName, int networkIdx, JSONArray patterns, MPFSNodeObject object) {
			this.modeId = modeId;
			this.stopId = stopId;
			this.stopName = stopName;
			this.networkIdx = networkIdx;
			this.patterns = patterns;
			this.object = object;
		}
		
		// initialize a node
		public void initialize(int modeId, int stopId, String stopName, int networkIdx, String utc, boolean origin) throws Exception {
//			System.out.println("initialize " + stopName);
			
			JSONArray patterns = new JSONArray();
//			JSONArray patterns = findAllPatterns(modeId, stopId, utc);
			
//			System.out.println("number of patterns = " + patterns.length());
			
			this.modeId = modeId;
			this.stopId = stopId;
			this.stopName = stopName;
			this.networkIdx = networkIdx;
			this.patterns = patterns;

			if (origin) { // initialize the origin
				MPFSNodeObject object = new MPFSNodeObject(utc, 0, -1, -1, -1, new JSONObject(), "NULL");
				this.object = object;
			}
			else { // initialize other nodes
				MPFSNodeObject object = new MPFSNodeObject("NULL", 120, -1, -1, -1, new JSONObject(), "NULL");
				this.object = object;
			}			
		}
		
		public int getModeId() {
			return modeId;
		}
		
		public int getStopId() {
			return stopId;
		}
		
		public String getStopName() {
			return stopName;
		}
		
		public int getNetworkIdx() {
			return networkIdx;
		}
		
		public JSONArray getPatterns() {
			return patterns;
		}
		
		public JSONObject getPattern(int idx) throws JSONException {
			JSONObject obj = patterns.getJSONObject(idx);
			return obj;
		}
		
		public MPFSNodeObject getObject() {
			return object;
		}
		
		public void setModeId(int newModeId) {
			this.modeId = newModeId;
		}
		
		public void setStopId(int newStopId) {
			this.stopId = newStopId;
		}
		
		public void setStopName(String newStopName) {
			this.stopName = newStopName;
		}
		
		public void setNetworkIdx(int newNetworkId) {
			this.networkIdx = newNetworkId;
		}
		
		public void getPatterns(JSONArray newPatterns) {
			this.patterns = newPatterns;
		}
		
		public void setPattern(JSONObject newPattern, int idx) throws JSONException {
			this.patterns.put(idx, newPattern);
		}
		
		public void setObject(MPFSNodeObject newObj) {
			this.object = newObj;
		}
		
		// update patterns based on the new arrival time
		public void updatePatterns() throws Exception {
			String jsonStrBND = API.doPTVBroadNextDepartures(modeId, stopId, 20);
			
//			System.out.println(jsonStrBND);
			
			JSONObject jsonBND = new JSONObject(jsonStrBND);
			JSONArray jsonDepartures = jsonBND.getJSONArray("values");
			
			// only keep the earliest run of each line and direction
			ArrayList<Integer> usedLinedirIds = new ArrayList<Integer>();
			JSONArray jsonEarliestDepartures = new JSONArray();
			
			for (int i = 0; i < jsonDepartures.length(); i++) {
				JSONObject currJsonDep = jsonDepartures.getJSONObject(i);
				JSONObject currJsonPlatform = currJsonDep.getJSONObject("platform");
				int linedirId = currJsonPlatform.getJSONObject("direction").getInt("linedir_id");
				String currUtc = currJsonDep.getString("time_timetable_utc");
				long diff = Utc.compare(currUtc, object.arrivalTime);
//				System.out.println(linedirId + " " + diff);
				if (diff < 1000*object.walkingTime) { // currUtc is earlier
					continue;
				}
				
				if (usedLinedirIds.contains(linedirId)) { // this line and direction already used, skip
					continue;
				}
				
				jsonEarliestDepartures.put(currJsonDep);
				usedLinedirIds.add(linedirId);
			}
			
//			System.out.println(jsonEarliestDepartures.toString());
			this.patterns = jsonEarliestDepartures;
		}
		
		// check if this node is an intra-transfer node
		public boolean isIntraTransfer() throws JSONException, ClassNotFoundException, SQLException {
			// connect to ptvdb database
			Connection conn = null;
			Statement stmt = null;
			boolean iit = false;
			try {
				conn = ptvdbJDBC.connectDatabase();
				stmt = conn.createStatement();
				String sql = "SELECT * FROM ptv_stops " + 
						"WHERE ID = " +
						stopId;
				ResultSet rs = stmt.executeQuery(sql);
				
				rs.next();
				if (rs.getString("IntraTransfer").equals("Y")) {
					iit = true;
				}
			} finally {
				if (stmt != null) stmt.close();
				if (conn != null) conn.close();
			}
			
			return iit;
		}
		
		// get the unique line ids going through this node
		public ArrayList<Integer> uniqueLineIds() throws JSONException {
			ArrayList<Integer> lineIds = new ArrayList<Integer>();
			
//			System.out.println("number of patterns = " + patterns.length());
			
			for (int i = 0; i < patterns.length(); i++) {
				int tmpLineId = patterns.getJSONObject(i).getJSONObject("platform").getJSONObject("direction").getJSONObject("line").getInt("line_id");
				
				if (lineIds.contains(tmpLineId)) {
					continue;
				}
				
				lineIds.add(tmpLineId);
			}
			
			return lineIds;
		}
		
		// the index of this node in a given network, -1 if it is not in the network
		public int indexInNetwork(ArrayList<MPFSNode> network) {
			int k = -1;
			for (int i = 0; i < network.size(); i++) {
				if (network.get(i).stopId == this.stopId) {
					k = i;
					break;
				}
			}
			
			return k;
		}
	}
	
	public static class MPFSNodeObject {
		private String arrivalTime; // the arrival time
		private int walkingTime; // the walking time for a transfer (in seconds)
		private int patternId; // the id of the current line and direction
		private int prevStopId; // the previous transfer stop id
		private int prevNetworkIdx; // the previous stop id in the network
		private JSONObject currDeparture; // the previous departure
		private String prevDepTime; // the departure of the previous transfer stop
		
		public String getArrivalTime() {
			return arrivalTime;
		}
		
		public int getWalkingTime() {
			return walkingTime;
		}
		
		public int getPatternId() {
			return patternId;
		}
		
		public int getPrevStopId() {
			return prevStopId;
		}
		
		public int getPreNetworkIdx() {
			return prevNetworkIdx;
		}
		
		public JSONObject getcurrDeparture() {
			return currDeparture;
		}
		
		public String getPrevDepTime() {
			return prevDepTime;
		}
		
		public void setArrivalTime(String newAT) {
			this.arrivalTime = newAT;
		}
		
		public void setWalkingTime(int newWT) {
			this.walkingTime = newWT;
		}
		
		public void setPatternId(int newPID) {
			this.patternId = newPID;
		}
		
		public void setPrevStopId(int newPSID) {
			this.prevStopId = newPSID;
		}
		
		public void setPreNetworkIdx(int newPNIDX) {
			this.prevNetworkIdx = newPNIDX;
		}
		
		public void setcurrDeparture(JSONObject newCD) {
			this.currDeparture = newCD;
		}
		
		public void setPrevDepTime(String newPDT) {
			this.prevDepTime = newPDT;
		}
		
		public MPFSNodeObject(String arrivalTime, int walkingTime, int patternId, int prevStopId, int prevNetworkIdx, JSONObject currDeparture, String prevDepTime) {
			this.arrivalTime = arrivalTime;
			this.walkingTime = walkingTime;
			this.patternId = patternId;
			this.prevStopId = prevStopId;
			this.prevNetworkIdx = prevNetworkIdx;
			this.currDeparture = currDeparture;
			this.prevDepTime = prevDepTime;
		}
		
		public MPFSNodeObject(MPFSNodeObject obj) {
			this.arrivalTime = obj.arrivalTime;
			this.walkingTime = obj.walkingTime;
			this.patternId = obj.patternId;
			this.prevStopId = obj.prevStopId;
			this.prevNetworkIdx = obj.prevNetworkIdx;
			this.currDeparture = obj.currDeparture;
			this.prevDepTime = obj.prevDepTime;
		}
		
		
		
		
		
		
		// the objects for the Key Line Stop (KLS) Search
		
	}
}

package au.com.jakebarnes.JourneyPlanner;

import java.util.ArrayList;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.ui.ModelMap;

import com.yimei.test.API;
import com.yimei.util.Utc;

/***
 * The Multiple Pattern First Search algorithm, extended from the PFS in the following reference:
 * R. Huang, "A Schedule-based Pathfinding Algorithm for Transit Networks Using Pattern First Search"
 * @author e04499 - Yi Mei
 *
 */

public class MPFS {
	
	public static String Search(String from, String to, String utc, ModelMap model) throws Exception {
		// get the JSON object of the origin and destination
		JSONObject jsonFrom = new JSONObject(from);
		JSONObject jsonTo = new JSONObject(to);
		
		// get the mode and stop ids of from and to
		int fromModeId = API.getModeId(jsonFrom.getString("transport_type"));
		int fromStopId = jsonFrom.getInt("stop_id");
		int toModeId = API.getModeId(jsonTo.getString("transport_type"));
		int toStopId = jsonTo.getInt("stop_id");
		
//		System.out.println(fromStopId + " " + toStopId);
		
		// initialize an empty network
		ArrayList<MPFSNode> MPFSNetwork = new ArrayList<MPFSNode>();
		// initialize the transfer node for from and to
		MPFSNode fromNode = new MPFSNode();
		fromNode.initialize(fromModeId, fromStopId, utc, true, model);
		MPFSNode toNode = new MPFSNode();
		toNode.initialize(toModeId, toStopId, utc, false, model);
		
		System.out.println(fromNode.id + " " + fromNode.patterns.toString());
		System.out.println(toNode.id + " " + toNode.patterns.toString());
		
		if (fromStopId < toStopId) {
			MPFSNetwork.ensureCapacity(toStopId+1);
		}
		else {
			MPFSNetwork.ensureCapacity(fromStopId);
			System.out.println(MPFSNetwork.size());
		}
		
		MPFSNetwork.set(fromStopId, fromNode);
		MPFSNetwork.set(toStopId, toNode);
		
		// create the open set, which is initialized to consist of the from node
		ArrayList<Integer> openSet = new ArrayList<Integer>();
		openSet.add(fromStopId);
		
		while (openSet.size() > 0) {
			// select the scan node in the open set, which is from the nondominated nodes in the network in terms of the objects
			ArrayList<Integer> nondominatedIds = findNondominatedIds(openSet, MPFSNetwork);
			// the destination is reached
//			if (nondominatedIds.indexOf(toStopId) > 0) {
//				
//			}
//			// select the first as the scan node
//			int currId = nondominatedIds.get(0);
			
		}
		
		return ("1");
	}
	
	// find all the patterns given the modeId, stopId and utc
	public static JSONArray findAllPatterns(int modeId, int stopId, String utc, ModelMap model) throws Exception {
		// find the next departures of the current stop
		
		String jsonStrBND = API.doPTVBroadNextDepartures(modeId, stopId, 20, model);
		
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
			if (diff < 0) { // currUtc is earlier
				continue;
			}
			
			if (usedLinedirIds.contains(linedirId)) { // this line and direction already used, skip
				continue;
			}
			
			jsonEarliestDepartures.put(currJsonDep);
			usedLinedirIds.add(linedirId);
		}
		
		return jsonEarliestDepartures;
	}
	
	public static ArrayList<Integer> findNondominatedIds(ArrayList<Integer> openSet, ArrayList<MPFSNode> MPFSNetwork) {
		ArrayList<Integer> nondominatedIds = new ArrayList<Integer>();
		
		nondominatedIds.add(openSet.get(0));
		for (int i = 1; i < openSet.size(); i++) {
			MPFSNode n1 = MPFSNetwork.get(openSet.get(i));
			
			ArrayList<Integer> dominatedIds = new ArrayList<Integer>();
			boolean isDominated = false;
			
			for (int j = 0; j < nondominatedIds.size(); j++) {
				MPFSNode n2 = MPFSNetwork.get(nondominatedIds.get(j));
				
				int domRel = n1.domination(n2);
				if (domRel > 0) {
					isDominated = true;
					break;
				}
				else if (domRel < 0) {
					dominatedIds.add(nondominatedIds.get(j));
				}
			}
				
			if (isDominated) {
				break;
			}
			
			// remove all the objects that are dominated by newObj
			nondominatedIds.removeAll(dominatedIds);
			
			// add newObj into the existing objects
			nondominatedIds.add(openSet.get(i));		
		}
		
		return nondominatedIds;
	}
	
	public static class MPFSNode {
		int modeId; // the mode id of this transfer node
		int id; // the stop id of the node
		private JSONArray patterns; // the available patterns (the earliest departures of each unique line and direction
		private ArrayList<MPFSNodeObject> objects;
		
		public MPFSNode () {
			this.modeId = -1;
			this.id = -1;
			this.patterns = new JSONArray();
			this.objects = new ArrayList<MPFSNodeObject>();
		}
		
		public MPFSNode (int modeId, int id, JSONArray patterns, ArrayList<MPFSNodeObject> objects) {
			this.modeId = modeId;
			this.id = id;
			this.patterns = patterns;
			this.objects = objects;
		}
		
		// initialize a node
		public void initialize(int modeId, int id, String utc, boolean origin, ModelMap model) throws Exception {
			JSONArray patterns = findAllPatterns(modeId, id, utc, model);
			
			ArrayList<MPFSNodeObject> objects = new ArrayList<MPFSNodeObject>();
			if (origin) { // initialize the origin
				MPFSNodeObject obj = new MPFSNodeObject(utc, 0, -1, -1, -1, "NULL", 0, 0);
				objects.add(obj);
			}
			else { // initialize other nodes
				MPFSNodeObject obj = new MPFSNodeObject("NULL", 2, -1, -1, -1, "NULL", Integer.MAX_VALUE, Integer.MAX_VALUE);
				objects.add(obj);
			}
			
			this.modeId = modeId;
			this.id = id;
			this.patterns = patterns;
			this.objects = objects;
		}
		
		public JSONArray getPatterns() {
			return patterns;
		}
		
		public JSONObject getPattern(int idx) throws JSONException {
			JSONObject obj = patterns.getJSONObject(idx);
			return obj;
		}
		
		public ArrayList<MPFSNodeObject> getObjects() {
			return objects;
		}
		
		public MPFSNodeObject getObject(int idx) {
			return objects.get(idx);
		}
		
		public void updateObjects(MPFSNodeObject newObj) { // update the Pareto-optimal objects in terms of arrivalTime, prevChangeNum and waitingTime
			ArrayList<MPFSNodeObject> dominatedObjs = new ArrayList<MPFSNodeObject>(); // the ids of the existing objects that are dominated by newObj
			boolean isDominated = false; // whether newObj is dominated by the existing objects
			
			for (int i = 0; i < objects.size(); i++) {
				int domRel = newObj.domination(objects.get(i)); // domRel < 0: newObj dominates objects[i]; domRel > 0: newObj is dominated by objects[i]
				if (domRel > 0) { // newObj is dominated by objects[i]
					isDominated = true;
					break;
				}
				else if (domRel < 0) { // newObj dominates objects[i]
					dominatedObjs.add(objects.get(i));
				}
			}
			
			if (isDominated) { // newObj is dominated, no change
				return;
			}
			
			// remove all the objects that are dominated by newObj
			objects.removeAll(dominatedObjs);
			
			// add newObj into the existing objects
			objects.add(newObj);
		}
		
		public int domination(MPFSNode n2) {
			int betterNum = 0;
			int worseNum = 0;
			
			for (int i = 0; i < this.objects.size(); i++) {
				for (int j = 0; j < n2.objects.size(); j++) {
					if (this.objects.get(i).domination(n2.objects.get(j)) > 0) {
						worseNum ++;
						break;
					}
				}
			}
			
			for (int j = 0; j < n2.objects.size(); j++) {
				for (int i = 0; i < this.objects.size(); i++) {
					if (this.objects.get(i).domination(n2.objects.get(j)) < 0) {
						betterNum ++;
						break;
					}
				}
			}
			
			if (betterNum == this.objects.size()) {
				return -1;
			}
			else if (worseNum == n2.objects.size()) {
				return 1;
			}
			else {
				return 0;
			}
		}
		
		public void printObjectiveList() { // print out the list of the objectives of the objects
			for (int i = 0; i < objects.size(); i++) {
				System.out.print(objects.get(i).arrivalTime + " " + objects.get(i).prevChangeNum + " " + objects.get(i).waitingTime + "\n");
			}
		}
	}
	
	public static class MPFSNodeObject {
		private String arrivalTime; // the arrival time
		private int walkingTime; // the walking time for a transfer (in minutes)
		private int patternId; // the id of the current line and direction
		private int prevStopId; // the previous transfer stop id
		private int prevRunId; // the run id of the previous transfer stop
		private String prevDepTime; // the departure of the previous transfer stop
		private int prevChangeNum; // the number of changes so far at the previous transfer stop
		private int waitingTime; // the waiting time at the previous transfer stop
		
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
		
		public int getPrevRunId() {
			return prevRunId;
		}
		
		public String getPrevDepTime() {
			return prevDepTime;
		}
		
		public int getPrevChangeNum() {
			return prevChangeNum;
		}
		
		public int getWaitingTime() {
			return waitingTime;
		}
		
		public MPFSNodeObject(String arrivalTime, int walkingTime, int patternId, int prevStopId, int prevRunId, String prevDepTime, int prevChangeNum, int waitingTime) {
			this.arrivalTime = arrivalTime;
			this.walkingTime = walkingTime;
			this.patternId = patternId;
			this.prevStopId = prevStopId;
			this.prevRunId = prevRunId;
			this.prevDepTime = prevDepTime;
			this.prevChangeNum = prevChangeNum;
			this.waitingTime = waitingTime;
		}
		
		public int domination(MPFSNodeObject o2) {
			int betterNum = 0;
			int worseNum = 0;
			
			if (Utc.compare(this.arrivalTime, o2.arrivalTime) < 0) { // it is earlier
				betterNum ++;
			}
			else if (Utc.compare(this.arrivalTime, o2.arrivalTime) > 0) { // equal
				worseNum ++;
			}
			
			if (this.prevChangeNum < o2.prevChangeNum) {
				betterNum ++;
			}
			else if (this.prevChangeNum > o2.prevChangeNum) {
				worseNum ++;
			}
			
			if (this.waitingTime < o2.waitingTime) {
				betterNum ++;
			}
			else if (this.waitingTime > o2.waitingTime) {
				worseNum ++;
			}
			
			if (betterNum > 0) {
				if (worseNum == 0) {
					return -1; // it dominates o2
				}
				else {
					return 0; // nondominated
				}
			}
			else {
				if (worseNum == 0) {
					return 0; // nondominated
				}
				else {
					return 1; // it is dominated by o2
				}
			}
		}
	}
}

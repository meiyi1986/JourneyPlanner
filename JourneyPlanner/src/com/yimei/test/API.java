package com.yimei.test;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.yimei.json.CrunchifyJSON;

import au.com.jakebarnes.JourneyPlanner.PFS;
import au.com.jakebarnes.JourneyPlanner.PTVAPI;

@Controller
@RequestMapping("/api")
public class API
{
	// use proxy or not
	private static final boolean useProxy = true;
	// constants
	private static final int RESULTS_LIMIT = 2; // the number of paths shown
	private static final int PTVDevId = 1000254; // Yi's PTV developer ID
	private static final String PTVSecKey = "4334b7be-28fe-11e4-8bed-0263a9d0b8a0"; // Yi's PTV security key
	
	/**** 
	 * get the complete URLs for each function
	 */
	
	private static String getURLHealthCheck(String timestamp) {
		final String uri;
		uri = "/v2/healthcheck?timestamp=" + timestamp;
		String url = PTVAPI.generateCompleteURLWithSignature(PTVSecKey, uri, PTVDevId);
		return url;
	}
	
	private static String getURLStopsNearby(double lat, double lon) {
		final String uri;
		uri = "/v2/nearme/latitude/" + lat + "/longitude/" + lon;
		String url = PTVAPI.generateCompleteURLWithSignature(PTVSecKey, uri, PTVDevId);
		return url;
	}
	
	private static String getURLSearch(String search) {
		String searchMod = encodeURIComponent(search);
//		System.out.println(searchMod);
		final String uri;
		uri = "/v2/search/" + searchMod;
		String url = PTVAPI.generateCompleteURLWithSignature(PTVSecKey, uri, PTVDevId);
		return url;
	}
	
	private static String getURLPOIsByMap(String poiStr, double lat1, double lon1, double lat2, double lon2, int griddepth, int limit) {
		final String uri;
		uri = "/v2/poi/" + poiStr + "/lat1/" + lat1 + "/long1/" + lon1 + "/lat2/" + lat2 + "/long2/" + lon2 
				+ "/griddepth/" + griddepth + "/limit/" + limit;
		String url = PTVAPI.generateCompleteURLWithSignature(PTVSecKey, uri, PTVDevId);
		return url;
	}
	
	private static String getURLBroadNextDepart(int mode, int stopId, int limit) {
		final String uri;
		uri = "/v2/mode/" + mode + "/stop/" + stopId + "/departures/by-destination/limit/" + limit;
		String url = PTVAPI.generateCompleteURLWithSignature(PTVSecKey, uri, PTVDevId);
		return url;
	}
	
	private static String getURLRunStops(int mode, int runId, int stopId, String utc) {
		final String uri;
		uri = "/v2/mode/" + mode + "/run/" + runId + "/stop/" + stopId + "/stopping-pattern?for_utc=" + utc;
		String url = PTVAPI.generateCompleteURLWithSignature(PTVSecKey, uri, PTVDevId);
		return url;
	}
	
	private static String getURLLineStops(int mode, int lineId) {
		final String uri;
		uri = "/v2/mode/" + mode + "/line/" + lineId + "/stops-for-line";
		String url = PTVAPI.generateCompleteURLWithSignature(PTVSecKey, uri, PTVDevId);
		return url;
	}
	
	// this is for URI encoding
	public static String encodeURIComponent(String s) {
	    String result;

	    try {
	        result = URLEncoder.encode(s, "UTF-8")
	                .replaceAll("\\+", "%20")
	                .replaceAll("\\%21", "!")
	                .replaceAll("\\%27", "'")
	                .replaceAll("\\%28", "(")
	                .replaceAll("\\%29", ")")
	                .replaceAll("\\%7E", "~");
	    } catch (UnsupportedEncodingException e) {
	        result = s;
	    }
	    
	    // if result ends with space, then remove the last space
	    if (result.length() >= 3) {
		    if (result.substring(result.length()-3, result.length()).equals("%20")) {
		    	result = result.substring(0, result.length()-3);
		    }
	    }

	    return result;
	}
  
	/******* Yi Mei: PTV timetable API - 19/08/2014 ************/
	// do the JSON API request and return the corresponding JSON data
	public static String doPTVHealthCheck(String timestamp) throws Exception {
		String url = getURLHealthCheck(timestamp);
		String jsonString = getJSONFromURL(url);
		
		return jsonString;
	}
	
	public static String doPTVStopsNearby(double lat, double lon) throws Exception {
		String url = getURLStopsNearby(lat, lon);
		String jsonString = getJSONFromURL(url);
		
		return jsonString;
	}
	
	public static String doPTVPOIsByMap(String poiStr, double lat1, double lon1, double lat2, double lon2, int griddepth, int limit) throws Exception {
		String url = getURLPOIsByMap(poiStr, lat1, lon1, lat2, lon2, griddepth, limit);
//		String jsonString = CrunchifyJSON.callURL(url);
		String jsonString = getJSONFromURL(url);
		
//		System.out.println(jsonString);
		
		return jsonString;
	}
	
	public static String doPTVSearch(String search) throws Exception {
		String url = getURLSearch(search);
		String jsonString = getJSONFromURL(url);
		
//		System.out.println(jsonString);
		
		return jsonString;
	}
	
	public static String doPTVBroadNextDepartures(int mode, int stopId, int limit) throws Exception {
		String url = getURLBroadNextDepart(mode, stopId, limit);
//		String jsonString = CrunchifyJSON.callURL(url);
		String jsonString = getJSONFromURL(url);
		
//		System.out.println(jsonString);
		
		return jsonString;
	}
	
	public static String doPTVRunStops(int mode, int runId, int stopId, String utc) throws Exception {
		String url = getURLRunStops(mode, runId, stopId, utc);
//		String jsonString = CrunchifyJSON.callURL(url);
		String jsonString = getJSONFromURL(url);
		
//		System.out.println(jsonString);
		
		return jsonString;
	}
	
	public static String doPTVLineStops(int mode, int lineId) throws Exception {
		String url = getURLLineStops(mode, lineId);
//		String jsonString = CrunchifyJSON.callURL(url);
		String jsonString = getJSONFromURL(url);
		
//		System.out.println(jsonString);
		
		return jsonString;
	}
  
	// do the JSON API request and return the corresponding JSON data
	@RequestMapping(value = "/PTVHealthCheck", method = RequestMethod.GET)
	@ResponseBody
	public static String doPTVHealthCheck(@RequestParam String timestamp, ModelMap model) throws Exception {
		String url = getURLHealthCheck(timestamp);
//		System.out.println(url);
//		String jsonString = CrunchifyJSON.callURL(url);
		String jsonString = getJSONFromURL(url);
		
		return jsonString;
	}
  
	@RequestMapping(value = "/PTVSearch", method = RequestMethod.GET)
	@ResponseBody
	public static String doPTVSearch(@RequestParam String search, ModelMap model) throws Exception {
		String url = getURLSearch(search);
//		System.out.println(url);
//		String jsonString = CrunchifyJSON.callURL(url);
		String jsonString = getJSONFromURL(url);
		
//		System.out.println(jsonString);
		
		return jsonString;
	}
	
	@RequestMapping(value = "/PTVPOIsByMap", method = RequestMethod.GET)
	@ResponseBody
	public static String doPTVPOIsByMap(String poiStr, double lat1, double lon1, double lat2, double lon2, int griddepth, int limit, 
			ModelMap model) throws Exception {
		String url = getURLPOIsByMap(poiStr, lat1, lon1, lat2, lon2, griddepth, limit);
//		String jsonString = CrunchifyJSON.callURL(url);
		String jsonString = getJSONFromURL(url);
		
//		System.out.println(jsonString);
		
		return jsonString;
	}
	
	@RequestMapping(value = "/PTVBroadNextDepartures", method = RequestMethod.GET)
	@ResponseBody
	public static String doPTVBroadNextDepartures(int mode, int stopId, int limit, ModelMap model) throws Exception {
		String url = getURLBroadNextDepart(mode, stopId, limit);
//		String jsonString = CrunchifyJSON.callURL(url);
		String jsonString = getJSONFromURL(url);
		
//		System.out.println(jsonString);
		
		return jsonString;
	}
	
	@RequestMapping(value = "/PTVRunStops", method = RequestMethod.GET)
	@ResponseBody
	public static String doPTVRunStops(int mode, int runId, int stopId, String utc, ModelMap model) throws Exception {
		String url = getURLRunStops(mode, runId, stopId, utc);
//		String jsonString = CrunchifyJSON.callURL(url);
		String jsonString = getJSONFromURL(url);
		
//		System.out.println(jsonString);
		
		return jsonString;
	}
	
	@RequestMapping(value = "/PTVLineStops", method = RequestMethod.GET)
	@ResponseBody
	public static String doPTVLineStops(int mode, int lineId, ModelMap model) throws Exception {
		String url = getURLLineStops(mode, lineId);
//		String jsonString = CrunchifyJSON.callURL(url);
		String jsonString = getJSONFromURL(url);
		
//		System.out.println(jsonString);
		
		return jsonString;
	}

	/******************************************************************/
	
	// data
	private Map<Integer, List<Integer>> links;
	
	public API() {
		
	}
	
	@RequestMapping(value = "/search", method = RequestMethod.GET)
	@ResponseBody
	public static String search(@RequestParam String from, @RequestParam String to, @RequestParam String utc,
		@RequestParam(defaultValue = "0") int offset, ModelMap model) throws Exception 
	{
		
		String journeys = PFS.Search(from, to, utc);
		
		System.out.println(journeys.toString());
		
		return journeys;
		
		
		
//		ArrayList<Integer> closedset = new ArrayList<Integer>(); // the set of stop ids already evaluated
//		JSONArray openset = new JSONArray(); // the set of tentative departures to be evaluated
//		JSONArray currJourney = new JSONArray(); // the current journey is initialized to be empty
//		
//		while (openset.length() > 0) {
//			// select the stop with the best score (e.g., earliest arrival time)
//			JSONObject currentDep = openset.getJSONObject(currIdx);
//			JSONObject currentStop = currentDep.getJSONObject("platform").getJSONObject("stop");
//			
//			// get the mode id and id of current
//			int currModeId = getModeId(currentStop.getString("transport_type"));
//			int currStopId = currentStop.getInt("stop_id");
//			
//			if (currStopId == jsonTo.getInt("stop_id")) {
//				JSONObject nextStop = new JSONObject(current.toString());
//				currJourney.put(nextStop);
//				return currJourney.toString();
//			}
//			
//			openset.remove(currIdx); // remove current from openset
//			closedset.add(currStopId); // add current to closedset
//			
//			/**
//			 *  the neighbors of current is the next transit stop or the destination 
//			 *  */
//			
//			// find the next departures of the current stop
//			String jsonStrBND = doPTVBroadNextDepartures(currModeId, currStopId, 20, model);
//			
////			System.out.println(jsonStrBND);
//			
//			JSONObject jsonBND = new JSONObject(jsonStrBND);
//			JSONArray jsonDepartures = jsonBND.getJSONArray("values");
//			
//			// only keep the earliest run of each line and direction
//			ArrayList<Integer> usedLinedirIds = new ArrayList<Integer>();
//			JSONArray jsonEarliesDepartures = new JSONArray();
//			for (int i = 0; i < jsonDepartures.length(); i++) {
//				JSONObject currJsonDep = jsonDepartures.getJSONObject(i);
//				JSONObject currJsonPlatform = currJsonDep.getJSONObject("platform");
//				int linedirId = currJsonPlatform.getJSONObject("direction").getInt("linedir_id");
//				
//				if (usedLinedirIds.contains(linedirId)) { // this line and direction already used, skip
//					continue;
//				}
//				
//				jsonEarliesDepartures.put(currJsonDep);
//				usedLinedirIds.add(linedirId);
//			}
//			
////			System.out.println(jsonEarliesDepartures.toString());
//			
//			// select a run
//			for (int i = 0; i < jsonEarliesDepartures.length(); i++) {			
//				JSONObject selJsonDep = jsonEarliesDepartures.getJSONObject(i);
//				
//				// add the linedir id of this run into closedLineIds, if it is not in it
//				int tmpLineIds = selJsonDep.getJSONObject("platform").getJSONObject("direction").getJSONObject("line").getInt("line_id");
//				if (!closedLineIds.contains(tmpLineIds)) {
//					closedLineIds.add(tmpLineIds);
//				}
//				
//				// check if there is next stop in this run (there are transit stops or destination ahead)
//				JSONObject nextStop = new JSONObject();
//				
//				// get all the stops of this run
//				JSONObject selJsonRun = selJsonDep.getJSONObject("run");
//				int runModeId = getModeId(selJsonRun.getString("transport_type"));
//				int runId = selJsonRun.getInt("run_id");
//				int stopId = selJsonDep.getJSONObject("platform").getJSONObject("stop").getInt("stop_id");
//				String currUtc = selJsonDep.getString("time_timetable_utc");
//				String jsonStrRS = doPTVRunStops(runModeId, runId, stopId, currUtc, model);
//				
////				System.out.println(jsonStrRS);
////				System.out.println(currUtc + ", " + selJsonDep.getJSONObject("platform").getJSONObject("stop").getString("location_name"));
//				
//				JSONObject jsonRS = new JSONObject(jsonStrRS);
//				JSONArray jsonRunDepartures = jsonRS.getJSONArray("values");
//				
//				// get the position of the current stop
//				int stopPos = 0;
//				while (stopPos < jsonRunDepartures.length()) {
//					JSONObject tmpDeparture = jsonRunDepartures.getJSONObject(stopPos);
//					JSONObject tmpJsonStop = tmpDeparture.getJSONObject("platform").getJSONObject("stop");
//					
//					if (tmpJsonStop.getInt("stop_id") == stopId)
//						break;
//					
//					stopPos ++;
//				}
//				
////				System.out.println("Next stops in this run");
//				
//				// find the next stop starting from the current stop in this run (transit or destination)
//				stopPos ++;
//				while (stopPos < jsonRunDepartures.length()) {
//					JSONObject tmpDeparture = jsonRunDepartures.getJSONObject(stopPos);
//					JSONObject tmpJsonStop = tmpDeparture.getJSONObject("platform").getJSONObject("stop");
//					
////					System.out.println(tmpJsonStop.getString("location_name"));
//					
//					// if the stop is the destination
//					if (tmpJsonStop.getInt("stop_id") == jsonTo.getInt("stop_id")) {
//						nextStop = tmpJsonStop;
//						break;
//					}
//					
////					System.out.println(tmpJsonStop.getString("location_name") + ": " + isTransit(tmpJsonStop, removedLineIds, model));
//					
//					// if this stop is a transit stop 
//					if (!removedStopIds.contains(tmpJsonStop.getInt("stop_id")) && isTransit(tmpJsonStop, removedLineIds, model)) {
//						nextStop = tmpJsonStop;
//						break;
//					}
//					
//					stopPos ++;
//				}	
//				
//				if (nextStop.length() == 0) { // this run is dead-end
//					continue;
//				}
//				
////				System.out.println("next stop: " + nextStop);
//				
//				// append the selected departure into the current journey
//				currJourney.put(selJsonDep);
//				
//				// add the next stop to the removed stop id list
//				if (!removedStopIds.contains(nextStop.getInt("stop_id"))) {
//					removedStopIds.add(nextStop.getInt("stop_id"));
//				}
//				
//				if (nextStop.getInt("stop_id") == jsonTo.getInt("stop_id")) { // next stop is the destination
//					currJourney.put(nextStop);
//					final JSONArray closedJourney = new JSONArray(currJourney.toString());
//					journeys.put(closedJourney);
//					currJourney.remove(currJourney.length()-1);
//				}
//				else {
//					// continue to search at the next stop
//					JourneyDFS(journeys, currJourney, nextStop, jsonFrom, jsonTo, removedStopIds, removedLineIds, model);
//				}
//				
//				// remove the selected departure
//				currJourney.remove(currJourney.length()-1);
//				
////				if (journeys.length() == RESULTS_LIMIT) {
////					System.out.println("curr stop = " + currStop.getString("location_name"));
////					System.out.println("all journeys : " + journeys);
////					return;
////				}
//			}
//			
//			System.out.println(currIdx);
//		}
//		
//		System.out.println(openset.toString());
//		
//		
//		JSONArray journeys = new JSONArray();
//		
//		JourneyDFS(journeys, currJourney, currStop, jsonFrom, jsonTo, removedStopIds, removedLineIds, model);
//		
//		System.out.println("finish searching!");
////		System.out.println(journeys);
//		
//		return journeys.toString();
	}
	
//	private int bestScoreIdx(JSONArray set, String score) throws JSONException {
//		int bestIdx = -1;
//		if (score == "EarliestArrival") { // earliest arrival time
//			String earliestArrivalTime = "NULL";
//			for (int i = 0; i < set.length(); i++) {
//				String tmpArrivalTime = set.getJSONObject(i).getString("arrivalTime");
//				long diff = Utc.compare(tmpArrivalTime, earliestArrivalTime);
//				
//				if (diff < 0) { // tmpArrivalTime is earlier than earliestArrivalTime
//					bestIdx = i;
//					earliestArrivalTime = tmpArrivalTime;
//				}
//				else if (diff == 0 && bestIdx == -1) { // the first one
//					bestIdx = i;
//				}
//			}
//		}
//		
//		return bestIdx;
//	}
	
	public static String getJSONFromURL(String url) {
		String jsonStr;
		if (useProxy) {
			jsonStr = CrunchifyJSON.callURLProxy(url);
		}
		else {
			jsonStr = CrunchifyJSON.callURL(url);
		}
		
		return jsonStr;
	}
	
	public static int getModeId(String modeStr) {
		int modeId = -1;
		
		switch (modeStr) {
		case "train":
			modeId = 0;
			break;
		case "tram":
			modeId = 1;
			break;
		case "bus":
			modeId = 2;
			break;
		case "VLine":
			modeId = 3;
			break;
		case "NightRider":
			modeId = 4;
			break;
		case "Outlet":
			modeId = 100;
			break;
		}
		
		return modeId;
	}
}

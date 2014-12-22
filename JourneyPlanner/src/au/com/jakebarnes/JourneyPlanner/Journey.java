package au.com.jakebarnes.JourneyPlanner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Journey {
	JSONArray departures;
	
	public JSONArray getDepartures() {
		return departures;
	}
	
	public Journey() {
		this.departures = new JSONArray();
	}
	
	public Journey(JSONArray departures) {
		this.departures = departures;
	}
	
	public void addDeparture(JSONObject departure) {
		departures.put(departure);
	}
	
	public void removeDeparture(int pos) {
		departures.remove(pos);
	}
	
	public boolean isempty() {
		if (departures.length() == 0) {
			return true;
		}
		else {
			return false;
		}
	}
	
	public int length() {
		return departures.length();
	}
	
	public void stopSeq() throws JSONException {
		System.out.println("Journey stop sequence:");
		for (int i = 0; i < departures.length(); i++) {
			System.out.println(departures.getJSONObject(i).getJSONObject("platform").getJSONObject("stop").getString("location_name"));
		}
	}
}

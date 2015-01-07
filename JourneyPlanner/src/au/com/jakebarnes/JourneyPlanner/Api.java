package au.com.jakebarnes.JourneyPlanner;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.conveyal.gtfs.GTFSFeed;
import com.conveyal.gtfs.model.Stop;
import com.yimei.routing.core.Location;
import com.yimei.routing.journey.Journey;
import com.yimei.routing.query.LocationQuery;
import com.yimei.routing.query.Query;
import com.yimei.routing.query.QueryTime;
import com.yimei.routing.raptor.Raptor;
import com.yimei.routing.raptor.RaptorModel;
import com.yimei.sql.GtfsJDBC;
import com.yimei.sql.JDBC;
import com.google.gson.Gson;

@Controller
@RequestMapping("/api")
public class Api
{
	final String database = "Adelaide";

	@RequestMapping(value = "/loadStops", method = RequestMethod.GET)
	public @ResponseBody Map<String, Stop> loadStops(ModelMap model) throws Exception {
		
		String jsonFile = "data/JSON/" + database + "/GTFSStops.json";
		
		Map<String, Stop> stops = GTFSFeed.stopFromJSON(jsonFile);
		return stops;
	}
	
	@RequestMapping(value = "/searhStopByName", method = RequestMethod.GET)
	@ResponseBody
	public String searhStopByName(@RequestParam String subName, ModelMap model) throws ClassNotFoundException, SQLException {
		
//		System.out.println("search starts");
		
		// connect the database
		Connection conn = JDBC.connectDatabase(database);
		
		List<Stop> stops = GtfsJDBC.searhStopsByName(subName, conn);
		
//		System.out.println("search ends, " + stops.size() + " stops found.");
		
		// transfer to json
		Gson gson = new Gson();
		String json = gson.toJson(stops);
		
//		System.out.println(json);
		
		return json;
	}
	
	@RequestMapping(value = "/searhStopById", method = RequestMethod.GET)
	@ResponseBody
	public String searhStopById(@RequestParam String id, ModelMap model) throws ClassNotFoundException, SQLException {
		
		System.out.println("search starts, id = " + id);
		
		// connect the database
		Connection conn = GtfsJDBC.connectDatabase(database);
		
		Stop stop = GtfsJDBC.searchStopById(id, conn);
		
//		for (Map.Entry<String, String> entry : stopinfo.entrySet()) {
//			System.out.println(entry.getKey() + ", " + entry.getValue());
//		}
		
		System.out.println("search ends.");
		
		// transfer to json
		Gson gson = new Gson();
		String json = gson.toJson(stop);
		
		System.out.println(json);
		
		return json;
	}
	
	@RequestMapping(value = "/journeyBetweenStops", method = RequestMethod.GET)
	@ResponseBody
	public String journeyBetweenStops(@RequestParam String fromStopId, @RequestParam String toStopId, @RequestParam String utc, 
			ModelMap model) throws Exception {
		// transform utc to QueryTime
		QueryTime qt = QueryTime.fromUtc(utc);
		
		// create the query
		Query query = new Query(fromStopId, toStopId, qt);
		
		// load the database
		String jsonDir = "data/JSON/" + database;
		File catalinaBase = new File( System.getProperty( "catalina.base" ) ).getAbsoluteFile();
		File modelFile = new File( catalinaBase, jsonDir + "/RaptorModel.json" );
		
		RaptorModel raptorModel = RaptorModel.fromJSON(modelFile);
		
		Raptor raptor = new Raptor(raptorModel);
		Journey journey = raptor.earliestArrivalJourney(query);
		journey.getShowInfo(database, raptorModel);
		
		journey.showMe();

//		CsaModel csaModel = CsaModel.fromJSON(jsonDir + "/CsaModel.json");
//		Csa csa = new Csa(csaModel);		
//		Journey journey = csa.searchEarliestArrivalJourney(query);
		
		// transfer to json
		Gson gson = new Gson();
		String json = gson.toJson(journey);
		
//		System.out.println(json);
		
		return json;
	}
	
	@RequestMapping(value = "/journeyBetweenLocations", method = RequestMethod.GET)
	@ResponseBody
	public String journeyBetweenLocations(@RequestParam String fromLat, @RequestParam String fromLon, @RequestParam String fromAddress,
			@RequestParam String toLat, @RequestParam String toLon, @RequestParam String toAddress, @RequestParam String utc, 
			ModelMap model) throws Exception {
		// transform utc to QueryTime
		QueryTime qt = QueryTime.fromUtc(utc);
		
		double fromLatDouble = Double.parseDouble(fromLat);
		double fromLonDouble = Double.parseDouble(fromLon);
		double toLatDouble = Double.parseDouble(toLat);
		double toLonDouble = Double.parseDouble(toLon);
		
		// create the location query
		Location fromLoc = new Location(fromLatDouble, fromLonDouble, fromAddress);
		Location toLoc = new Location(toLatDouble, toLonDouble, toAddress);
		LocationQuery locQuery = new LocationQuery(fromLoc, toLoc, qt);
		
		// load the database
		String jsonDir = "data/JSON/" + database;
		File catalinaBase = new File( System.getProperty( "catalina.base" ) ).getAbsoluteFile();
		File modelFile = new File( catalinaBase, jsonDir + "/RaptorModel.json" );
		
		RaptorModel raptorModel = RaptorModel.fromJSON(modelFile);
		
		Raptor raptor = new Raptor(raptorModel);
		Journey journey = raptor.earliestArrivalJourney(locQuery);
		journey.getShowInfo(locQuery, database, raptorModel);
		
		journey.showMe();

//		CsaModel csaModel = CsaModel.fromJSON(jsonDir + "/CsaModel.json");
//		Csa csa = new Csa(csaModel);		
//		Journey journey = csa.searchEarliestArrivalJourney(query);
		
		// transfer to json
		Gson gson = new Gson();
		String json = gson.toJson(journey);
		
//		System.out.println(json);
		
		return json;
	}
	
	public static void main(String[] args) throws Exception {
		String fromStopId = "101578";
		String toStopId = "3017";
		String utc = "2014-12-03T05:33:12.000Z";
		
		Api api = new Api();
		String journey = api.journeyBetweenStops(fromStopId, toStopId, utc, new ModelMap());
		
		
	}
  
}


package au.com.jakebarnes.JourneyPlanner;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.mapdb.Fun.Tuple2;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.conveyal.gtfs.GTFSFeed;
import com.conveyal.gtfs.model.Stop;
import com.yimei.routing.core.FootPath;
import com.yimei.routing.core.Journey;
import com.yimei.routing.core.Query;
import com.yimei.routing.core.QueryTime;
import com.yimei.routing.csa.Csa;
import com.yimei.routing.csa.CsaModel;
import com.yimei.routing.raptor.Raptor;
import com.yimei.routing.raptor.RaptorModel;
import com.yimei.sql.GtfsJDBC;
import com.yimei.sql.JDBC;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

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
	
	@RequestMapping(value = "/search", method = RequestMethod.GET)
	@ResponseBody
	public String search(@RequestParam String fromStopId, @RequestParam String toStopId, @RequestParam String utc, 
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
	
	public static void main(String[] args) throws Exception {
		String fromStopId = "101578";
		String toStopId = "3017";
		String utc = "2014-12-03T05:33:12.000Z";
		
		Api api = new Api();
		String journey = api.search(fromStopId, toStopId, utc, new ModelMap());
		
		
	}
  
}


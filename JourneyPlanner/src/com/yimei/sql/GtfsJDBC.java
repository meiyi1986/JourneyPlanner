package com.yimei.sql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestParam;

import com.conveyal.gtfs.GTFSFeed;
import com.conveyal.gtfs.model.Route;
import com.conveyal.gtfs.model.Stop;
import com.conveyal.gtfs.model.Trip;
import com.google.gson.Gson;

public class GtfsJDBC extends JDBC {

	private String database;
	
	public GtfsJDBC(String database) {
		this.database = database;
	}
	
	public String getLabel() {
		return database;
	}
	
	
	
	public void createStopTable(GTFSFeed feed, Connection conn) throws SQLException {
		Statement stmt = conn.createStatement();
		String sql;
		
		// drop the table if it exists
		sql = "DROP TABLE IF EXISTS gtfs_stops";
		stmt.executeUpdate(sql);
		
		// create a new table
		sql = "CREATE TABLE IF NOT EXISTS gtfs_stops " +
		        "(ID VARCHAR(255) not NULL, " +
		        "Code VARCHAR(255), " + 
		        "Name VARCHAR(255), " +
		        "Description VARCHAR(255), " +
		        "Lat DECIMAL(18,6), " +
		        "Lon DECIMAL(18,6), " +
		        "ZoneID VARCHAR(255), " +
		        "Url VARCHAR(255), " + 
		        "LocationType Integer, " +
		        "ParentStation VARCHAR(255), " +
		        "TimeZone VARCHAR(255), " +
		        "WheelChairBoarding VARCHAR(255), " +
		        "PRIMARY KEY (ID))";
		stmt.executeUpdate(sql);
		
		for (Stop stop : feed.stops.values()) {
			sql = "INSERT INTO gtfs_stops " +
					"VALUES (\"" +
					stop.stop_id + "\", \"" +
					stop.stop_code + "\", \"" +
					stop.stop_name + "\", \"" +
					stop.stop_desc + "\", " +
					stop.stop_lat + ", " + stop.stop_lon + ", \"" +
					stop.zone_id + "\", \"" +
					stop.stop_url + "\", " +
					stop.location_type + ", \"" +
					stop.parent_station + "\", \"" + 
					stop.stop_timezone + "\", \"" + 
					stop.wheelchair_boarding + "\"" +
					") ON DUPLICATE KEY UPDATE id=id";
				
			System.out.println(sql);
			
			stmt.executeUpdate(sql);
		}
		
		stmt.close();
	}
	
	public static Stop toStop(ResultSet rs) throws SQLException {
		String id = rs.getString("ID");
		String code = rs.getString("Code");
		String name = rs.getString("Name");
		String desc = rs.getString("Description");
		double lat = rs.getDouble("Lat");
		double lon = rs.getDouble("Lon");
		String zid = rs.getString("ZoneID");
		String url = rs.getString("Url");
		int lt = rs.getInt("LocationType");
		String ps = rs.getString("ParentStation");
		String tz = rs.getString("TimeZone");
		String wb = rs.getString("WheelChairBoarding");
		
		Stop stop = new Stop(id, code, name, desc, lat, lon, zid, url, lt, ps, tz, wb);
		return stop;
	}
	
	public void createRouteTable(GTFSFeed feed, Connection conn) throws SQLException {
		Statement stmt = conn.createStatement();
		String sql;
		
		// drop the table if it exists
		sql = "DROP TABLE IF EXISTS gtfs_routes";
		stmt.executeUpdate(sql);
		
		// create a new table
		sql = "CREATE TABLE IF NOT EXISTS gtfs_routes " +
		        "(ID VARCHAR(255) not NULL, " +
		        "AgencyID VARCHAR(255), " + 
		        "ShortName VARCHAR(255), " +
		        "LongName VARCHAR(255), " +
		        "Description VARCHAR(255), " +
		        "Type Integer, " +
		        "Url VARCHAR(255), " + 
		        "Color VARCHAR(255), " +
		        "TextColor VARCHAR(255), " +
		        "PRIMARY KEY (ID))";
		stmt.executeUpdate(sql);
		
		for (Route route : feed.routes.values()) {
			sql = "INSERT INTO gtfs_routes " +
					"VALUES (\"" +
					route.route_id + "\", \"" +
					route.agency_id + "\", \"" +
					route.route_short_name + "\", \"" +
					route.route_long_name + "\", \"" +
					route.route_desc + "\", " +
					route.route_type + ", \"" +
					route.route_url + "\", \"" +
					route.route_color + "\", \"" + 
					route.route_text_color + "\"" +
					") ON DUPLICATE KEY UPDATE id=id";
				
			System.out.println(sql);
			
			stmt.executeUpdate(sql);
		}
		
		stmt.close();
	}
	
	
	public void createTripTable(GTFSFeed feed, Connection conn) throws SQLException {
		Statement stmt = conn.createStatement();
		String sql;
		
		// drop the table if it exists
		sql = "DROP TABLE IF EXISTS gtfs_trips";
		stmt.executeUpdate(sql);
		
		// create a new table
		sql = "CREATE TABLE IF NOT EXISTS gtfs_trips " +
		        "(ID VARCHAR(255) not NULL, " +
		        "RouteID VARCHAR(255), " + 
		        "ServiceID VARCHAR(255), " +
		        "HeadSign VARCHAR(255), " +
		        "ShortName VARCHAR(255), " +
		        "DirectionID Integer, " +
		        "BlockID VARCHAR(255), " +
		        "ShapeID VARCHAR(255), " + 
		        "BikeAllowed Integer, " +
		        "WheelchairAccessible Integer, " +
		        "PRIMARY KEY (ID))";
		stmt.executeUpdate(sql);
		
		for (Trip trip : feed.trips.values()) {
			sql = "INSERT INTO gtfs_trips " +
					"VALUES (\"" +
					trip.trip_id + "\", \"" +
					trip.route_id + "\", \"" +
					trip.service_id + "\", \"" +
					trip.trip_headsign + "\", \"" +
					trip.trip_short_name + "\", " +
					trip.direction_id + ", \"" +
					trip.block_id + "\", \"" +
					trip.shape_id + "\", " +
					trip.bikes_allowed + ", " +
					trip.wheelchair_accessible +
					") ON DUPLICATE KEY UPDATE id=id";
				
			System.out.println(sql);
			
			stmt.executeUpdate(sql);
		}
		
		stmt.close();
	}
	
	
	public static List<Stop> searhStopsByName(String subName, Connection conn) throws ClassNotFoundException, SQLException {
		List<Stop> stops = new ArrayList<Stop>();
		
		// find all the items whose name contains the sub-name
		Statement stmt = conn.createStatement();
		String sql;
		sql = "SELECT * FROM gtfs_stops WHERE Name LIKE \"%" + subName + "%\"";
		
		ResultSet rs = stmt.executeQuery(sql);
		int count = 0;
		while (rs.next()) {
			Stop stop = GtfsJDBC.toStop(rs);
			stops.add(stop);
			
			count ++;
			if (count > 10)
				break;
		}
		
		rs.close();
		stmt.close();
		
		return stops;
	}
	
	public static Stop searchStopById(String id, Connection conn) throws SQLException {
		
		Stop stop = new Stop();
		
		// find the item with the id
		Statement stmt = conn.createStatement();
		String sql;
		sql = "SELECT * FROM gtfs_stops WHERE ID = \"" + id + "\"";
		
		ResultSet rs = stmt.executeQuery(sql);
		
		while (rs.next()) {
			stop = GtfsJDBC.toStop(rs);
		}
		
		rs.close();
		stmt.close();
		
		return stop;
	}
	
	public static String routeNameOfTrip(String tripId, Connection conn) throws SQLException {
		
		String routeName = null;
		
		if (tripId.equals("walking")) {
			routeName = "walking";
			return routeName;
		}
		
		Statement stmt = conn.createStatement();
		String sql;
		sql = "SELECT gtfs_routes.ShortName, gtfs_trips.ID FROM gtfs_routes INNER JOIN gtfs_trips ON gtfs_routes.ID = gtfs_trips.RouteID"
				+ " WHERE gtfs_trips.ID = " + tripId;
		
		ResultSet rs = stmt.executeQuery(sql);
		
		while (rs.next()) {
			routeName = rs.getString("ShortName");
		}
		
		rs.close();
		stmt.close();
		
		return routeName;
	}
	
	public static void main(String[] args) throws ClassNotFoundException, SQLException{
		
		String database = "Adelaide";
		
		GtfsJDBC gj = new GtfsJDBC(database);
		Connection conn = JDBC.connectDatabase(database);
		
		String INPUT = "data/GTFS/" + database + ".zip";
		
        GTFSFeed feed = GTFSFeed.fromFile(INPUT);
        
        gj.createStopTable(feed, conn);
        gj.createRouteTable(feed, conn);
        gj.createTripTable(feed, conn);
        
        
	}
}

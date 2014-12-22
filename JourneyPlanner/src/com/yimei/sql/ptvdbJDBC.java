package com.yimei.sql;

// Import required packages
import java.sql.*;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.yimei.test.API;
import com.yimei.util.GeoCalculator;

public class ptvdbJDBC {
	// JDBC driver name and database URL
	static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";  
	static final String DB_URL = "jdbc:mysql://localhost/";
	
	// Database credentials
	static final String USER = "root";
	static final String PASS = "123456";
	
	// PTV database properties (obtained from PTV API)
	static final int PTVmaxLineID = 8362;
	static final int PTVmaxStopID = 31852;
	
	public static Connection connectDatabase() throws ClassNotFoundException, SQLException {
		Connection conn = null;
		Statement stmt = null;
		// Register JDBC driver
		Class.forName("com.mysql.jdbc.Driver");
		
		// Open a connection
//		System.out.println("Connecting to database...");
		conn = DriverManager.getConnection(DB_URL, USER, PASS);
		stmt = conn.createStatement();
		String sql;
		
		// Select this database
		sql = "USE ptvdb";
		stmt.executeUpdate(sql);
//		System.out.println("Database ptvdb selected...");
		
		return conn;
	}
	
	public static void main(String[] args) {
		Connection conn = null;
		Statement stmt = null;
		try{
			// Register JDBC driver
			Class.forName("com.mysql.jdbc.Driver");
			
			// Open a connection
			System.out.println("Connecting to database...");
			conn = DriverManager.getConnection(DB_URL, USER, PASS);
			
			stmt = conn.createStatement();
			String sql;
			
//			// Drop the database ptvdb if it exists
//			System.out.println("Clearing database ptvdb...");
//			
//			sql = "DROP DATABASE IF EXISTS ptvdb";
//			stmt.executeUpdate(sql);
//			System.out.println("Database ptvdb cleared successfully...");
//			
//			// Create the database ptvdb if it does not exist
//			System.out.println("Creating database ptvdb...");
//			
//			sql = "CREATE DATABASE ptvdb";
//			stmt.executeUpdate(sql);
//			System.out.println("Database ptvdb created successfully...");
			
			// Select this database
			sql = "USE ptvdb";
			stmt.executeUpdate(sql);
			System.out.println("Database ptvdb selected...");

			// initialize the tables ptv_stops and ptv_linestops
//			createTableStopsAndLinestops(conn);
			
			// generate the table ptv_runstops
//			createTableRunstops(conn);
			
			// update the intra transfer status of all the stops
//			updateAllIntraTransfer(conn);
			
			// add the artificial lines with walking distance
//			addWalkingLines(conn);
			
			// generate the table ptv_lineneighbors
//			createLineNeighbors(conn);
			
			// create the table for the lines involved between any pair of two nodes
			createTablePairLines(conn);
			
			
		}catch(SQLException se){
			//Handle errors for JDBC
			se.printStackTrace();
		}catch(Exception e){
			//Handle errors for Class.forName
			e.printStackTrace();
		}finally{
			//finally block used to close resources
			try{
				if(stmt!=null)
					stmt.close();
			}catch(SQLException se2){
			}// nothing we can do
			try{
				if(conn!=null)
					conn.close();
			}catch(SQLException se){
				se.printStackTrace();
			}//end finally try
		}//end try
		System.out.println("Goodbye!");
	}//end main
	
	// create the tables ptv_stops and ptv_linestops
	public static void createTableStopsAndLinestops(Connection conn) throws Exception {
		Statement stmt = conn.createStatement();
		String sql;
		
		// Create a table for stops
		System.out.println("Creating table for stops in given database...");
		
		sql = "CREATE TABLE IF NOT EXISTS ptv_stops " +
        "(ID INTEGER not NULL, " +
        "Name VARCHAR(255), " + 
        "ModeID INTEGER, " +
        "Suburb VARCHAR(255), " +
        "Lat DECIMAL(18,6), " +
        "Lon DECIMAL(18,6), " +
        "Distance DECIMAL(18,6), " +
        "IntraTransfer VARCHAR(1), " + // whether this stop is an intra-transfer (without walking)
        "InterTransfer VARCHAR(1), " + // whether this stop is an inter-transfer (with walking to other modes)
        "PRIMARY KEY (ID))";

		stmt.executeUpdate(sql);
		System.out.println("Created table for stops in given database...");
		
		// Create a table for linestops
		System.out.println("Creating table for linestops in given database...");
		
		sql = "CREATE TABLE IF NOT EXISTS ptv_linestops " +
        "(ID INTEGER not NULL, " +
        "LineID INTEGER not NULL, " + 
        "ModeID INTEGER, " +
        "StopID INTEGER, " +
        "Distance DECIMAL(18,6), " + // the distance to the first node of the line (used for walking lines)
        "PRIMARY KEY (ID))";

		stmt.executeUpdate(sql);
		System.out.println("Created table for linestops in given database...");
					
		for (int i = 0; i < PTVmaxLineID+1; i++) { // the line id
			System.out.println("Scanning line id " + i + "/" + PTVmaxLineID + ", process = " + 100*i/PTVmaxLineID + "%");
			
			for (int j = 0; j < 5; j++) {
				String strAllStops = API.doPTVLineStops(j, i);
				
				if (strAllStops.length() > 2) { // it is not empty
					// add to the tables
					JSONArray jsonAllStops = new JSONArray(strAllStops);
					System.out.println("LineID = " + i);
					addAllStops(jsonAllStops, j, conn);
					addLine(jsonAllStops, i, j, conn);
//					System.out.println(j + " " + strAllStops);
					break;
				}
			}
		}
		
		stmt.close();
		
		System.out.println("Initialized tables ptv_stops (" + count("ptv_stops", conn) + "rows), ptv_linestops (" + count("ptv_linestops", conn) + "rows)");
	}
	
	// add all the line stops into the table ptv_stops
	public static void addAllStops(JSONArray jsonAllStops, int modeId, Connection conn) throws JSONException, SQLException{
		Statement stmt = conn.createStatement();
		
		for (int i = 0; i < jsonAllStops.length(); i++) {
			String sql = "INSERT INTO ptv_stops " +
					"VALUES (" +
					jsonAllStops.getJSONObject(i).getInt("stop_id") + ", " +
					"\"" + jsonAllStops.getJSONObject(i).getString("location_name") + "\", " +
					modeId + ", " +
					"\"" + jsonAllStops.getJSONObject(i).getString("suburb") + "\", " +
					jsonAllStops.getJSONObject(i).getDouble("lat") + ", " +
					jsonAllStops.getJSONObject(i).getDouble("lon") + ", " +
					jsonAllStops.getJSONObject(i).getDouble("distance") + ", " +
					"\"N\"" + ", " + // IntraTransfer
					"\"N\"" +  // InterTransfer
					") ON DUPLICATE KEY UPDATE id=id";
			
//			System.out.println(sql);
			
			stmt.executeUpdate(sql);
		}
		
		stmt.close();
	}
	
	// add the line into the table ptv_linestops
	public static void addLine(JSONArray jsonAllStops, int lineId, int modeId, Connection conn) throws JSONException, SQLException {
		Statement stmt = conn.createStatement();
		
		// count the rows of the current table
		String sql = "SELECT COUNT(*) FROM ptv_linestops";
		ResultSet rs = stmt.executeQuery(sql);
		
		int currID = 0;
		while(rs.next()){
	         //Retrieve by column name
	         currID  = rs.getInt("COUNT(*)");
        }
		
//		System.out.println(currID);
		
		for (int i = 0; i < jsonAllStops.length(); i++) {
			currID ++;
			sql = "INSERT INTO ptv_linestops " +
					"VALUES (" +
					currID + ", " +
					lineId + ", " +
					modeId + ", " +
					jsonAllStops.getJSONObject(i).getInt("stop_id") + ", " +
					jsonAllStops.getJSONObject(i).getDouble("distance") +
					")";
			
//			System.out.println(sql);
			
			stmt.executeUpdate(sql);
		}
		
		rs.close();
		stmt.close();
	}
	
	// create the table ptv_runs based on ptv_stops
	public static void createTableRunstops(Connection conn) throws Exception {
		Statement stmt = conn.createStatement();
		String sql;
		ResultSet rs = null;
		
//		// Drop the table if exists
//		System.out.println("Clearing table for runstops in given database...");
//		
//		sql = "DROP TABLE IF EXISTS ptv_runstops";
//		
//		stmt.executeUpdate(sql);
//		System.out.println("Cleared table for runstops in given database...");
		
		// Create the table
		System.out.println("Creating table for runstops in given database...");
		
		sql = "CREATE TABLE IF NOT EXISTS ptv_runstops " +
        "(ID INTEGER not NULL, " + // the runstop id
        "RunID INTEGER, " + // the run id
        "ModeID INTEGER, " + // the mode id
        "LineID INTEGER, " + // the line id it belongs to
        "StopID INTEGER, " + // the stop id along the run
        "StopOrder INTEGER, " + // the stop order in the run
        "Timetable VARCHAR(255), " + // the timetable of the run in this stop
        "PRIMARY KEY (ID))";

		stmt.executeUpdate(sql);
		System.out.println("Created table for runstops in given database...");
		
		int currLineID = 8306;
		
		// clear the runs in the currLineID
		sql = "DELETE FROM ptv_runStops " +
                "WHERE LineID >= " + currLineID;
		stmt.executeUpdate(sql);
		
		for (int l = currLineID; l < PTVmaxLineID+1; l++) {
			int lineId = l;
			
			System.out.println("Processing line id " + lineId);
			
			ArrayList<Integer> scannedRunIds = new ArrayList<Integer>(); // the run ids that have already been scanned
//			ArrayList<Integer> scannedLinedirIds = new ArrayList<Integer>(); // the linedir ids that have already been scanned

			// scann the stops in this line
			sql = "SELECT * FROM ptv_linestops WHERE LineID = " + l; // + " AND ID > 11";
			rs = stmt.executeQuery(sql);
			while (rs.next()) {
				int modeId = rs.getInt("ModeID");
				int stopId = rs.getInt("StopID");
				
				// find all the departures going through this stop
				String strAllDep = API.doPTVBroadNextDepartures(modeId, stopId, 0);
//				System.out.println(strAllDep);
				
				JSONObject jsonObj = new JSONObject(strAllDep);
				JSONArray jsonAllDep = jsonObj.getJSONArray("values");
				
				if (jsonAllDep.length() > 0) {
					System.out.println("There are " + jsonAllDep.length() + " runs at stop " + jsonAllDep.getJSONObject(0).getJSONObject("platform").getJSONObject("stop").getString("location_name"));
				}
				
				for (int i = 0; i < jsonAllDep.length(); i++) {
					int runId = jsonAllDep.getJSONObject(i).getJSONObject("run").getInt("run_id");
					
					if (jsonAllDep.getJSONObject(i).getJSONObject("platform").getJSONObject("direction").getJSONObject("line").getInt("line_id") != lineId) {
						continue;
					}
					
//					int linedirId = jsonAllDep.getJSONObject(i).getJSONObject("platform").getJSONObject("direction").getInt("linedir_id");
//					if (scannedLinedirIds.contains(linedirId)) { // this linedir is scanned, skip
//						continue;
//					}
					
					if (scannedRunIds.contains(runId)) { // this run is scanned, skip
						continue;
					}
					
					System.out.println(i + "/" + jsonAllDep.length() + ", adding run id " + runId);
					
					// get the run sequence
					String utc = jsonAllDep.getJSONObject(i).getString("time_timetable_utc");
					String strRunStops = API.doPTVRunStops(modeId, runId, stopId, utc);
					addRunStops(strRunStops, runId, modeId, lineId, conn);
					
					scannedRunIds.add(new Integer(runId));
//					scannedLinedirIds.add(new Integer(linedirId));
				}
			}
		}
		
		rs.close();
		stmt.close();
		
		System.out.println("Initialized tables ptv_runstops (" + count("ptv_runstops", conn) + "rows)");
	}
	
	public static void addRunStops(String strRunStops, int runId, int modeId, int lineId, Connection conn) throws JSONException, SQLException {
		JSONObject obj = new JSONObject(strRunStops);
		JSONArray jsonRunStops = obj.getJSONArray("values");
		
		Statement stmt = conn.createStatement();
		String sql;
		
		// get the maximal ID of the current ptv_runstops table
		int currID = columnMaxInt("ptv_runstops", "ID", conn);
		// add the stops one by one
		for (int i = 0; i < jsonRunStops.length(); i++) {
			currID ++;
			sql = "INSERT INTO ptv_runstops " +
					"VALUES (" +
					currID + ", " +
					runId + ", " +
					modeId + ", " +
					lineId + ", " +
					jsonRunStops.getJSONObject(i).getJSONObject("platform").getJSONObject("stop").getInt("stop_id") + ", " +
					i + ", " + // stop order
					"'" + jsonRunStops.getJSONObject(i).getString("time_timetable_utc") + "'" + // timetable
					")";
			stmt.executeUpdate(sql);
			
//			System.out.println("Order of stop " + jsonRunStops.getJSONObject(i).getJSONObject("platform").getJSONObject("stop").getString("location_name") +
//					" is set to " + i);
		}
		
		stmt.close();
	}
	
	// update the IntraTransfer status of all the stops
	public static void updateAllIntraTransfer(Connection conn) throws Exception {
		Statement stmt = conn.createStatement();
		String sql;
		
		int currID = 25434;
		sql = "SELECT * FROM ptv_stops WHERE ID >=" + currID;
		ResultSet rs = stmt.executeQuery(sql);
		
		int maxStopID = columnMaxInt("ptv_stops", "ID", conn);
		
		while (rs.next()) {
			System.out.println("Process: " + rs.getInt("ID") + "/" + maxStopID);
			
			int stopId = rs.getInt("ID");
			updateStopIntraTransfer(stopId, conn);
		}
		
		rs.close();
		stmt.close();
	}
	
	// update the IntraTransfer status of a stop
	public static void updateStopIntraTransfer(int stopId, Connection conn) throws Exception {
		System.out.println("Scanning stop " + stopName(stopId, conn) + "...");
		
		// reset IntraTransfer = 'N'
		setIntraTransfer(stopId, 'N', conn);
		
		// the scanned neighbors
		ArrayList<Integer> scannedNeighbors = new ArrayList<Integer>();
		
		Statement stmt = conn.createStatement();
		String sql;
		
		// select the IDs of the occurrences of this stop
		sql = "SELECT * FROM ptv_runstops " +
				"WHERE StopID = " +
				stopId;
		ResultSet rs = stmt.executeQuery(sql);
		
		while (rs.next()) {
			int id = rs.getInt("ID");
			int runId = rs.getInt("RunID");
			int runModeId = rs.getInt("ModeID");
			ResultSet neighbors = runNeighbors(id, runId, conn);
			while (neighbors.next()) {
				int neighborStopId = neighbors.getInt("StopID");
				
				if (scannedNeighbors.contains(neighborStopId)) { // the neighbor is scanned, skip
					continue;
				}
				
//				System.out.println("scanning neighboring stop " + stopName(neighborStopId, conn));
				
				int currBelongToModeLineSize = belongToModeLineSize(stopId, runModeId, conn);
				int tmpBelongToModeLineSize = belongToModeLineSize(neighborStopId, runModeId, conn);
				
				if (tmpBelongToModeLineSize < currBelongToModeLineSize) { // the current stop is a intra-transfer!
					System.out.println("Stop " + stopName(stopId, conn) + " is a tranfer!");
					setIntraTransfer(stopId, 'Y', conn);
					return;
				}
				
				// add this neighbor into the scanned list
				scannedNeighbors.add(new Integer(neighborStopId));
			}
		}
		
		rs.close();
		stmt.close();
	}
	
	// get the neighbors of a stop (id) in a run
	public static ResultSet runNeighbors(int id, int runId, Connection conn) throws SQLException {
		Statement stmt = conn.createStatement();
		String sql;
		
		int predId = id-1;
		int succId = id+1;
		
		sql = "SELECT * FROM ptv_runstops " + 
				"WHERE (ID = " +
				predId + 
				" OR ID = " + 
				succId + 
				") AND runID = " +
				runId;
//		System.out.println(sql);
		ResultSet rs = stmt.executeQuery(sql);
		
		rs.close();
		stmt.close();
		
		return rs;
		
	}
	
	// get the number of rows in the table
	public static int count(String tableName, Connection conn) throws SQLException {
		Statement stmt = conn.createStatement();
		
		String sql = "SELECT COUNT(*) FROM " + tableName;
		ResultSet rs = stmt.executeQuery(sql);
		
		int count = 0;
		while(rs.next()) {
	         //Retrieve by column name
			count  = rs.getInt("COUNT(*)");
		}
		
		rs.close();
		stmt.close();
		
		return count;
	}
	
	// the size (number of stops) of the line
	public static int LineSize(int lineId, Connection conn) throws SQLException {
		Statement stmt = conn.createStatement();
		
		String sql = "SELECT COUNT(*) FROM ptv_linestops WHERE LineID = " + lineId;
		ResultSet rs = stmt.executeQuery(sql);
		
		int count = 0;
		while(rs.next()) {
	         //Retrieve by column name
			count  = rs.getInt("COUNT(*)");
		}
		
		rs.close();
		stmt.close();
		
		return count;
	}
	
	// the number of lines the stop belongs to
	public static int belongToLineSize(int stopId, Connection conn) throws SQLException {
		Statement stmt = conn.createStatement();
		
		String sql = "SELECT COUNT(*) FROM ptv_linestops WHERE StopID = " + stopId;
		ResultSet rs = stmt.executeQuery(sql);
		
		int count = 0;
		while(rs.next()) {
	         //Retrieve by column name
			count  = rs.getInt("COUNT(*)");
		}
		
		rs.close();
		stmt.close();
		
		return count;
	}
	
	// the number of lines with the given mode the stop belongs to
	public static int belongToModeLineSize(int stopId, int modeId, Connection conn) throws SQLException {
		Statement stmt = conn.createStatement();
		
		String sql = "SELECT COUNT(*) FROM ptv_linestops WHERE StopID = " + 
				stopId + 
				" AND ModeID = " +
				modeId;
		ResultSet rs = stmt.executeQuery(sql);
		
		int count = 0;
		while(rs.next()) {
	         //Retrieve by column name
			count  = rs.getInt("COUNT(*)");
		}
		
		rs.close();
		stmt.close();
		
		return count;
	}
	
	public static void addWalkingLines(Connection conn) throws Exception {
		// get the current maximal line ID
		int currLineID = columnMaxInt("ptv_linestops", "LineID", conn);
		
		Statement stmt = conn.createStatement();
		String sql;
		Statement stmt1 = null;
		ResultSet rs;
		
		int startStopId = 1003;
		
		// find the first ID of the walking line of the starting stop
		int startID = -1;
		sql = "SELECT * FROM ptv_linestops WHERE ModeID = 5 AND StopID = " +
				startStopId +
				" AND Distance = 0";
		rs = stmt.executeQuery(sql);
		while (rs.next()) {
			startID = rs.getInt("ID");
		}
//		System.out.println(startID);
		if (startID > 0) {
			sql = "DELETE FROM ptv_linestops WHERE ID >= " + startID;
			stmt.executeUpdate(sql);
		}
		
		sql = "SELECT * FROM ptv_stops WHERE ID >= " + startStopId;
		rs = stmt.executeQuery(sql);
		while (rs.next()) {
			double lat1 = rs.getDouble("Lat");
			double lon1 = rs.getDouble("Lon");
			int modeId1 = rs.getInt("ModeID");
			String name1 = rs.getString("Name");
			
			System.out.println("Curr stop ID = " + rs.getInt("ID"));
//			System.out.println("Examine stop " + name1);
			
			// reset InterTransfer = 'N'
			setInterTransfer(rs.getInt("ID"), 'N', conn);
			
			// check if there are more than one mode at this stop
			stmt1 = conn.createStatement();
			sql = "SELECT COUNT(DISTINCT ModeID) AS DistCount FROM ptv_linestops " +
					"WHERE StopID = " +
					rs.getInt("ID");
			ResultSet rs1 = stmt1.executeQuery(sql);
			int modeNum = 0;
			while (rs1.next()) {
				modeNum = rs1.getInt("DistCount");
			}
			
			if (modeNum > 1) { // there are multiple modes at this stop, it is an inter-transfer
				setInterTransfer(rs.getInt("ID"), 'Y', conn);
			}
			
			JSONArray walkingStops = new JSONArray();
			
			String strNearStops = API.doPTVStopsNearby(lat1, lon1);
			JSONArray jsonNearStops = new JSONArray(strNearStops);
			
//			System.out.println(jsonNearStops);
			
//			System.out.println(selfStop.toString());
			
			// add the self stop into the walking stops
			JSONObject selfStop = jsonNearStops.getJSONObject(0).getJSONObject("result");
			selfStop.put("distance", 0);
			walkingStops.put(selfStop);
			
//			System.out.println(walkingStops);
			
			// only maintain the nearby stops within the predefined walking distance
			double walkingDistance = 0.2;
			for (int i = 1; i < jsonNearStops.length(); i++) {
				if (!jsonNearStops.getJSONObject(i).getString("type").equals("stop")) { // is not stop, skip
					continue;
				}
				
				// recalculate the distance
				double lat2 = jsonNearStops.getJSONObject(i).getJSONObject("result").getDouble("lat");
				double lon2 = jsonNearStops.getJSONObject(i).getJSONObject("result").getDouble("lon");
				int modeId2 = API.getModeId(jsonNearStops.getJSONObject(i).getJSONObject("result").getString("transport_type"));
				String name2 = jsonNearStops.getJSONObject(i).getJSONObject("result").getString("location_name");
				
				if (modeId2 == modeId1) { // the same mode, skip
					continue;
				}
				
				double distance = GeoCalculator.distFrom(lat1, lon1, lat2, lon2);
				
				if (distance > walkingDistance) { // too far away
					break;
				}
				
				JSONObject tmpStop = jsonNearStops.getJSONObject(i).getJSONObject("result");
				tmpStop.put("distance", distance);
				
				walkingStops.put(tmpStop);
			}
			
			if (walkingStops.length() > 1) {
//				System.out.println(walkingStops.toString());
				currLineID ++;
				// set this stop to be an inter-transfer
				setInterTransfer(rs.getInt("ID"), 'Y', conn);
				// add this walking line into ptv_linestops
				addLine(walkingStops, currLineID, 5, conn); // 5: walking mode
			}
		}
		
		rs.close();
		stmt1.close();
		stmt.close();
	}
	
	// create the table of neighbors of the lines
	public static void createLineNeighbors(Connection conn) throws SQLException {
		Statement stmt = conn.createStatement();
		String sql;
		
		// Create the table
		System.out.println("Creating table for lineneighbors in given database...");
		
		sql = "CREATE TABLE IF NOT EXISTS ptv_lineneighbors " +
        "(ID INTEGER not NULL, " + // the line neighbor id
        "LineID INTEGER, " + // the current line id
        "NeighborLineID INTEGER, " + // the neighboring line id
        "NeighborModeID INTEGER, " + // the neighboring line mode
        "PRIMARY KEY (ID))";

		stmt.executeUpdate(sql);
		System.out.println("Created table for lineneighbors in given database...");
		
		int startLineId = 0;
		
		// clear the runs in the currLineID
		sql = "DELETE FROM ptv_lineneighbors " +
                "WHERE LineID >= " + startLineId;
		stmt.executeUpdate(sql);
		
		int currId = count("ptv_lineneighbors", conn);
		
		// select all the unique line ids
		sql = "SELECT DISTINCT LineID FROM ptv_linestops";
		ResultSet rs = stmt.executeQuery(sql);
		while (rs.next()) {
			int lineId = rs.getInt("LineID");
			
			ArrayList<Integer> neighborLineIds = allNeighborLineIds(lineId, conn);
			System.out.println("Line " + lineId + ", neighbor size = " + neighborLineIds.size());
			// add all the neighbors into the table
			for (int i = 0; i < neighborLineIds.size(); i++) {
				currId ++;
				int neiLineId = neighborLineIds.get(i);
				int neiModeId = lineModeId(neiLineId, conn);
				addLineNeighbor(currId, lineId, neiLineId, neiModeId, conn);
			}
		}
		
		rs.close();
		stmt.close();
	}
	
	// find all the neighboring line ids for a given line id
	public static ArrayList<Integer> allNeighborLineIds(int lineId, Connection conn) throws SQLException {
		Statement stmt = conn.createStatement();
		String sql;
		
		ArrayList<Integer> neighborLineIds = new ArrayList<Integer>();
		
		// find all the stops that are transfer nodes
//		sql = "SELECT ptv_stops.ID, ptv_linestops.LineID, ptv_stops.IntraTransfer, " +
//				"ptv_stops.InterTransfer, ptv_linestops.StopID " +
//				"FROM ptv_linestops, ptv_stops " +
//				"WHERE ptv_stops.ID = ptv_linestops.StopID " +
//				"AND ptv_linestops.LineID = " +
//				lineId;
		sql = "SELECT * FROM ptv_linestops WHERE LineID = " + lineId;
		ResultSet rs = stmt.executeQuery(sql);
		
		while(rs.next()) {
			int stopId = rs.getInt("StopID");
			ArrayList<Integer> stopLineIds = allStopLineIds(stopId, conn);
//			System.out.println("Stop " + stopId + " has lines num = " + stopLineIds.size());
			
			for (int i = 0; i < stopLineIds.size(); i++) {
				if (stopLineIds.get(i) == lineId) { // this is the line itself
					continue;
				}
				
				if (neighborLineIds.contains(stopLineIds.get(i))) { // the line is already in the list
					continue;
				}
				
				neighborLineIds.add(new Integer(stopLineIds.get(i)));
			}
		}
		
		rs.close();
		stmt.close();
		
		return neighborLineIds;
	}
	
	// find all lines of a stop
	public static ArrayList<Integer> allStopLineIds(int stopId, Connection conn) throws SQLException {
		Statement stmt = conn.createStatement();
		String sql;
		
		ArrayList<Integer> stopLineIds = new ArrayList<Integer>();
		
		// all the non-walking lines
		sql = "SELECT DISTINCT LineID FROM ptv_linestops WHERE StopID = " +
				stopId +
				" AND ModeID != 5";
		ResultSet rs = stmt.executeQuery(sql);
		while (rs.next()) {
			int lineId = rs.getInt("LineID");
			stopLineIds.add(new Integer(lineId));
		}
		
		// the walking line of the stop
		sql = "SELECT * FROM ptv_linestops WHERE StopID = " +
				stopId +
				" AND ModeID = 5 AND Distance = 0";
		rs = stmt.executeQuery(sql);
		while (rs.next()) {
			int lineId = rs.getInt("LineID");
			stopLineIds.add(new Integer(lineId));
		}
		
		rs.close();
		stmt.close();
		
		return stopLineIds;
	}
	
	// add a line and neighbor pair into the table ptv_lineneighbors
	public static void addLineNeighbor(int id, int lineId, int neighborLineId, int neighborModeId, Connection conn) throws SQLException {
		Statement stmt = conn.createStatement();
		String sql;
		
		sql = "INSERT INTO ptv_lineneighbors " +
				"VALUES (" +
				id + ", " +
				lineId + ", " +
				neighborLineId + ", " +
				neighborModeId +
				")";
		
//		System.out.println(sql);
		
		stmt.executeUpdate(sql);
		
		stmt.close();
	}
	
	// create the table ptv_pairlines
	public static void createTablePairLines(Connection conn) throws Exception {
		Statement stmt = conn.createStatement();
		String sql;
		
		System.out.println("Creating table for pairlines in given database...");
		
		sql = "CREATE TABLE IF NOT EXISTS ptv_pairlines " +
        "(ID INTEGER not NULL, " + // the pair ID
        "SourceID INTEGER not NULL, " + // the source ID
        "DestinationID INTEGER not NULL, " + // the destination ID
        "LineID INTEGER not NULL, " + // the involved line ID
        "PRIMARY KEY (ID))";

		stmt.executeUpdate(sql);
		System.out.println("Created table for pairlines in given database...");
		
		// add pair lines into the table
		
		
		stmt.close();
	}
	
	// get the maximal value of the integer column of a table
	public static int columnMaxInt(String table, String column, Connection conn) throws SQLException {
		Statement stmt = conn.createStatement();
		
		String sql = "SELECT MAX(" + column + ") AS Max FROM " + table;
		ResultSet rs = stmt.executeQuery(sql);
		
		int max = -Integer.MAX_VALUE;
		while(rs.next()) {
	        max = rs.getInt("Max");
		}
		
		rs.close();
		stmt.close();
		
		return max;
	}
	
	// set the interTransfer of a stop to stat
	public static void setIntraTransfer(int stopId, char stat, Connection conn) throws SQLException {
		Statement stmt = conn.createStatement();
		
		String sql = "UPDATE ptv_stops " +
                "SET IntraTransfer = '" + stat + "' WHERE ID = " +
				stopId;
		
		stmt.executeUpdate(sql);
		
		stmt.close();
	}
	
	// set the interTransfer of a stop to stat
	public static void setInterTransfer(int stopId, char stat, Connection conn) throws SQLException {
		Statement stmt = conn.createStatement();
		
		String sql = "UPDATE ptv_stops " +
                "SET InterTransfer = '" + stat + "' WHERE ID = " +
				stopId;
		
		stmt.executeUpdate(sql);
		
		stmt.close();
	}
	
	// if a stop is in a line
	public static boolean stopInLine(int stopId, int lineId, Connection conn) throws SQLException {
		Statement stmt = conn.createStatement();
		
		String sql = "SELECT COUNT(*) FROM ptv_linestops " +
				"WHERE StopID = " + stopId +
				" AND LineID = " + lineId;
		
		ResultSet rs = stmt.executeQuery(sql);
		int selectedSize = 0;
		while (rs.next()) {
			selectedSize = rs.getInt("COUNT(*)");
		}
		
		stmt.close();
		
		if (selectedSize == 0) {
			return false;
		}
		else {
			return true;
		}
	}
	
	// get the name of the stop based on its stopId
	public static String stopName(int stopId, Connection conn) throws SQLException {
		Statement stmt = conn.createStatement();
		String sql;
		
		sql = "SELECT ID, Name FROM ptv_stops " +
				"WHERE ID = " +
				stopId;
		ResultSet rs = stmt.executeQuery(sql);
		
		String name = "";
		while (rs.next()) {
			name = rs.getString("Name");
		}
		
		rs.close();
		stmt.close();
		
		return name;
	}
	
	// get the line mode of a line id
	public static int lineModeId(int lineId, Connection conn) throws SQLException {
		Statement stmt = conn.createStatement();
		String sql;
		
		sql = "SELECT * FROM ptv_linestops " +
				"WHERE LineID = " +
				lineId;
		ResultSet rs = stmt.executeQuery(sql);
		
		int modeId = -1;
		while (rs.next()) {
			modeId = rs.getInt("ModeID");
		}
		
		rs.close();
		stmt.close();
		
		return modeId;
	}

}//end JDBCExample
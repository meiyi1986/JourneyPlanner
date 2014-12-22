package com.conveyal.gtfs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.zip.ZipFile;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Fun;
import org.mapdb.Fun.Tuple2;
import org.mapdb.Serializer;

import com.conveyal.gtfs.error.GTFSError;
import com.conveyal.gtfs.error.GeneralError;
import com.conveyal.gtfs.model.*;
import com.conveyal.gtfs.validator.DefaultValidator;
import com.conveyal.gtfs.validator.GTFSValidator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.yimei.routing.csa.CsaModel;

import static org.opentripplanner.common.LoggingUtil.human;

/**
 * All entities must be from a single feed namespace.
 * Composed of several GTFSTables.
 */
public class GTFSFeed {

    private static final Logger LOG = LoggerFactory.getLogger(GTFSFeed.class);

    DB db = DBMaker.newTempFileDB()
            .transactionDisable()
            .asyncWriteEnable()
            .compressionEnable()
            .make(); // TODO db.close();
    
    public String label; // this is the label of the feed, indicating where it is from (drawn from the file name)

    String feedId = null;

    /* Some of these should be multimaps since they don't have an obvious unique key. */
    public final Map<String, Agency>        agency         = Maps.newHashMap();
    public final Map<String, Calendar>      calendars      = Maps.newHashMap();
    public final Map<String, CalendarDate>  calendarDates  = Maps.newHashMap();
    public final Map<String, FareAttribute> fareAttributes = Maps.newHashMap();
    public final Map<String, FareRule>      fareRules      = Maps.newHashMap();
    public final Map<String, FeedInfo>      feedInfo       = Maps.newHashMap();
    public final Map<String, Frequency>     frequencies    = Maps.newHashMap();
    public final Map<String, Route>         routes         = Maps.newHashMap();
    public final Map<String, Shape>         shapes         = db.getHashMap("shapes"); // Shapes table is often one of the two big ones
    public final Map<String, Stop>          stops          = Maps.newHashMap();
    public final Map<String, Transfer>      transfers      = Maps.newHashMap();
    public final Map<String, Trip>          trips          = Maps.newHashMap();

    // Map from 2-tuples of (trip_id, stop_sequence) to stoptimes.
    public final ConcurrentNavigableMap<Tuple2, StopTime> stop_times = db.getTreeMap("stop_times");

    /* A place to accumulate errors while the feed is loaded. The objective is to tolerate as many errors as possible and keep on going. */
    public transient List<GTFSError> errors = Lists.newArrayList();

    /* Set the feed_id from feed_info. Call only after feed_info has been loaded. */
    private void setFeedId() {
        if (feedInfo.size() == 0) {
            feedId = null;
            LOG.info("feed_info missing, feed ID is undefined."); // TODO log an error, ideally feeds should include a feedID
            return;
        } else if (feedInfo.size() > 1) {
            errors.add(new GeneralError("feed_info", 2, null, "More than one entry in this table. Using only the first one."));
        }
        feedId = feedInfo.values().iterator().next().feed_id;
        LOG.info("Feed ID is '{}'.", feedId);
    }

    private void loadFromFile(ZipFile zip) throws Exception {
        // maybe pass in GtfsFeed, which contains the three params to loadTable as fields.
        new FeedInfo.Factory().loadTable(zip, errors, feedInfo);
        setFeedId();
        new Agency.Factory().loadTable(zip, errors, agency);
        new Calendar.Factory().loadTable(zip, errors, calendars);
        new CalendarDate.Factory().loadTable(zip, errors, calendarDates);
        new FareAttribute.Factory().loadTable(zip, errors, fareAttributes);
        new FareRule.Factory().loadTable(zip, errors, fareRules);
        new Frequency.Factory().loadTable(zip, errors, frequencies);
        new Route.Factory().loadTable(zip, errors, routes);
        new Shape.Factory().loadTable(zip, errors, shapes);
        new Stop.Factory().loadTable(zip, errors, stops);
        new Transfer.Factory().loadTable(zip, errors, transfers);
        new Trip.Factory().loadTable(zip, errors, trips);
        new StopTime.Factory().loadTable(zip, errors, stop_times);

        LOG.info("{} errors", errors.size());
        for (GTFSError error : errors) {
            LOG.info("{}", error);
        }
    }

    public void validate (GTFSValidator validator) {
        validator.validate(this, false);
    }

    public static GTFSFeed fromFile(String file) {
        GTFSFeed feed = new GTFSFeed();
        
        // get the label
        feed.getLabelFromFile(file);
        
        ZipFile zip;
        try {
            zip = new ZipFile(file);
            feed.loadFromFile(zip);
            zip.close();
            feed.validate(new DefaultValidator());
            return feed;
        } catch (Exception e) {
            LOG.error("Error loading GTFS: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    // Bin all trips by the sequence of stops they visit.
    public void findPatterns() {
        // A map from a list of stop IDs (the pattern) to a list of trip IDs which fit that pattern.
        Map<List<String>, List<String>> tripsForPattern = Maps.newHashMap();
        int n = 0;
        for (String trip_id : trips.keySet()) {
            if (++n % 100000 == 0) {
                LOG.info("trip {}", human(n));
            }
            Map<Fun.Tuple2, StopTime> tripStopTimes =
                stop_times.subMap(
                    Fun.t2(trip_id, null),
                    Fun.t2(trip_id, Fun.HI)
                );
            List<String> stops = Lists.newArrayList();
            // In-order traversal of StopTimes within this trip. The 2-tuple keys determine ordering.
            for (StopTime stopTime : tripStopTimes.values()) {
                stops.add(stopTime.stop_id);
            }
            // Fetch or create the tripId list for this stop pattern, then add the current trip to that list.
            List<String> trips = tripsForPattern.get(stops);
            if (trips == null) {
                trips = Lists.newArrayList();
                tripsForPattern.put(stops, trips);
            }
            trips.add(trip_id);
        }
        LOG.info("Total patterns: {}", tripsForPattern.keySet().size());
    }

    // TODO augment with unrolled calendar, patterns, etc. before validation
    
    // Yi Mei
    // get label from file name
    public String getLabelFromFile(String file) {
    	label = file.substring(file.lastIndexOf('/') + 1);
    	label = label.substring(0, label.indexOf('.'));
    	
    	return label;
    }
    
    /*** to JSON ***/
    
    public void stopToJSON(String dirStr) {
		// set directory
		File directory = new File(dirStr);
		if (!directory.exists()) {
			try{
				directory.mkdir();
		    } catch(SecurityException se){
			   //handle it
			}        
		}
		
		Gson gson = new Gson();
		String json = gson.toJson(this.stops);
//	      System.out.println(json);
			
		try {
			//write converted json data to a file named "file.json"
			FileWriter writer = new FileWriter(dirStr + "/GTFSStops.json");
			writer.write(json);
			writer.close();
		} catch (IOException e) {
	  		e.printStackTrace();
	  	}
	}
    
    public static Map<String, Stop> stopFromJSON(String jsonFile) {
    	Map<String, Stop> stops = Maps.newHashMap();
		
		Gson gson = new Gson();
		try {
	 
			BufferedReader br = new BufferedReader(
				new FileReader(jsonFile));
	 
			//convert the json string back to object
			Type typeOfQueryList = new TypeToken<Map<String, Stop>>(){}.getType();
			stops = gson.fromJson(br, typeOfQueryList);
	 
//			System.out.println(model);
	 
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return stops;
	}
    
    public void routeToJSON(String dirStr) {
		// set directory
		File directory = new File(dirStr);
		if (!directory.exists()) {
			try{
				directory.mkdir();
		    } catch(SecurityException se){
			   //handle it
			}        
		}
		
		Gson gson = new Gson();
		String json = gson.toJson(this.routes);
//	      System.out.println(json);
			
		try {
			//write converted json data to a file named "file.json"
			FileWriter writer = new FileWriter(dirStr + "/GTFSRoutes.json");
			writer.write(json);
			writer.close();
		} catch (IOException e) {
	  		e.printStackTrace();
	  	}
	}
    
    public static Map<String, Route> routeFromJSON(String jsonFile) {
    	Map<String, Route> routes = Maps.newHashMap();
		
		Gson gson = new Gson();
		try {
	 
			BufferedReader br = new BufferedReader(
				new FileReader(jsonFile));
	 
			//convert the json string back to object
			Type typeOfQueryList = new TypeToken<Map<String, Route>>(){}.getType();
			routes = gson.fromJson(br, typeOfQueryList);
	 
//			System.out.println(model);
	 
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return routes;
	}
}

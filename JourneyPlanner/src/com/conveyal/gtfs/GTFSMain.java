package com.conveyal.gtfs;

import org.mapdb.DB;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.conveyal.gtfs.model.Agency;
import com.conveyal.gtfs.model.Shape;
import com.conveyal.gtfs.model.Trip;

public class GTFSMain {

    private static final Logger LOG = LoggerFactory.getLogger(GTFSMain.class);

    static final String INPUT = "data/GTFS/Adelaide.zip";
    //static final String INPUT = "/var/otp/graphs/nl/gtfs-nl.zip";
    //static final String INPUT = "/var/otp/graphs/trimet/gtfs.zip";
    
    public static void main (String[] args) {
        GTFSFeed feed = GTFSFeed.fromFile(INPUT);
        
        feed.stopToJSON("data/JSON/Adelaide");
        feed.routeToJSON("data/JSON/Adelaide");
        feed.transfersToJSON("data/JSON/Adelaide");  
//        feed.findPatterns();
//        feed.db.close();
//        
//        for(Map.Entry<String, Trip> entry: feed.trips.entrySet()) {
//            System.out.println(entry.getKey() + " : " + entry.getValue().service_id);
//        }
    }

}

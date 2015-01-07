Journey Planner V.1.0.0

This is the journey planner project aiming to give reasonable earliest arrival journey given any source and target stops. This is based on the paper:

"Round-based Public Transit Routing"

This is example of Adelaide, Australia. It has several major modules:

1. GTFS to RAPTOR Model module: com.yimei.routing.raptor.RaptorModel
2. RAPTOR journey planning module: com.yimei.routing.raptor.Raptor
3. GTFS to MySQL module: com.yimei.sql.GtfsJDBC

------------------------
Before starting:

1. Run RaptorModel locally to obtain the model data;
2. Run the GtfsJDBC to obtain the SQL database (MySQL server has to being running on your computer);
3. Go have fun!

------------------------
Local test:
Run the main() function in com.yimei.routing.raptor.Raptor. You may want to change the source, target and departure time in the query.

Web test using Eclipse EE:
1. Install Tomcat Server by following the steps from http://tomcat.apache.org/;
2. Start the Tomcat Server in Eclipse EE;
3. Visit localhost:8080/JourneyPlanner for testing.

Stop-Stop Journey Planning:
Type and choose the stops from the drop-down list in both the "from" and "to" textbox. The time and date is set to the current time and date by default. You can also choose specific time and date for journey planning. Then click "Search".

Location-Location Journey Planner
Click on the map to set the source location. Then click again to set the target location. The click "Search".

------------------------
For any further question, please contact yi.mei@rmit.edu.au.

By Yi Mei @RMIT University, Dec 2014.
==============

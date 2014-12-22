Journey Planner V.1.0.0

This is the journey planner project aiming to give reasonable earliest arrival journey given any source and target stops. This is based on the paper:

"Round-based Public Transit Routing"

This is example of Adelaide, Australia. It has several major modules:

1. GTFS to RAPTOR Model module: com.yimei.routing.raptor.RaptorModel
2. RAPTOR journey planning module: com.yimei.routing.raptor.Raptor
3. GTFS to MySQL module: com.yimei.sql.GtfsJDBC

Local test:
Run the main() function in com.yimei.routing.raptor.Raptor. You may want to change the source, target and departure time in the query.

You can also deploy the whole Maven project on server such as TomCat.

For any further question, please contact yi.mei@rmit.edu.au.

By Yi Mei @RMIT University, Dec 2014.
==============

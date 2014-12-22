package com.yimei.graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.mapdb.Fun.Tuple2;

import com.conveyal.gtfs.GTFSFeed;
import com.google.common.collect.Maps;

/***
 * This is the class for time-dependent travel time (in seconds) of an edge, i.e., from one station to another
 * @author e04499
 *
 */

public class TravelTimeFunc {
	
	private Map<String, DailyTravelTimeFunc> func;
	
	/* constructor */
	
	public TravelTimeFunc(){
		func = Maps.newHashMap();
	}
	
	/* methods */
	
	public void addTravelTime(String day, TemporalTravelTime tt) {
		if (!func.containsKey(day)) {
			DailyTravelTimeFunc dttf = new DailyTravelTimeFunc();
			func.put(day, dttf);
		}
		func.get(day).addTravelTime(tt);
	}
	
	public int travelTimeFromNow(String day, int now) {
		int ttfn = 0;
		String today = day;
		int tmpNow = now;
		DailyTravelTimeFunc todayFunc = func.get(today);
		TemporalTravelTime todayNextKeyTT = todayFunc.nextKeyTravelTime(tmpNow);
		while (todayNextKeyTT == null) {
			ttfn += 24 * 60 * 60 - tmpNow;
			tmpNow = 0;
			today = nextDay(today); // go to next day
			todayFunc = func.get(today);
			todayNextKeyTT = todayFunc.nextKeyTravelTime(tmpNow);
		} 
		
		if (todayNextKeyTT.getTimePoint() == TemporalTravelTime.constantTimePoint) {
			return ttfn + todayNextKeyTT.getValue();
		}
		
		ttfn += todayNextKeyTT.getTimePoint() + todayNextKeyTT.getValue() - tmpNow;
		return ttfn;
	}
	
	public String nextDay(String day) { // get the next day of the week
		String nextDay;
		switch (day) {
        case "Monday":  nextDay = "Tuesday";
                 break;
        case "Tuesday":  nextDay = "Wednesday";
                 break;
        case "Wednesday":  nextDay = "Thusday";
                 break;
        case "Thusday":  nextDay = "Friday";
                 break;
        case "Friday":  nextDay = "Saturday";
                 break;
        case "Saturday":  nextDay = "Sunday";
                 break;
        case "Sunday":  nextDay = "Monday";
                 break;
        default: nextDay = "Invalid day";
                 break;
		}
		
		return nextDay;
	}
	
	public static TravelTimeFunc createConstantFunc(int value, String funcDesc) {
		ArcWeightInfo info = new ArcWeightInfo(funcDesc);
		TemporalTravelTime constantTT = new TemporalTravelTime(TemporalTravelTime.constantTimePoint, value, info);
		
		DailyTravelTimeFunc dttf = new DailyTravelTimeFunc();
		dttf.addTravelTime(constantTT);
		
		TravelTimeFunc ttf = new TravelTimeFunc();
		ttf.func = Maps.newHashMap();
		ttf.func.put("Monday", dttf);
		ttf.func.put("Tuesday", dttf);
		ttf.func.put("Wednesday", dttf);
		ttf.func.put("Thusday", dttf);
		ttf.func.put("Friday", dttf);
		ttf.func.put("Saturday", dttf);
		ttf.func.put("Sunday", dttf);
		
		return ttf;
	}
}

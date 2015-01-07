package com.yimei.routing.query;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;

import com.conveyal.gtfs.model.Calendar;
import com.yimei.modelbuilder.ModelBuilder;
import com.yimei.util.TimeTransformer;

/*** 
 * time + day + overnights : time is no more than 24*3600
 * @author e04499
 *
 */

public class QueryTime implements Comparable<QueryTime> {

	public int time;
	public String day;
	public int overnights;
	
	public QueryTime(int time, String day, int overnights) {
		this.time = time;
		this.day = day;
		this.overnights = overnights;
	}
	
	public QueryTime(QueryTime normTime) {
		this.time = normTime.time;
		this.day = normTime.day;
		this.overnights = normTime.overnights;
	}
	
	public void within24Hours() {
		
		while (time < 0) {
			time += ModelBuilder.secondsPerDay;
			day = Calendar.prevWeekDay(day);
			overnights --;
		}

		while (time > ModelBuilder.secondsPerDay) {
			time -= ModelBuilder.secondsPerDay;
			day = Calendar.nextWeekDay(day);
			overnights ++;
		}
	}
	
	public int adjustedTime(int overnights) { // adjusted time relative to the current overnights
		return this.time + (this.overnights - overnights) * ModelBuilder.secondsPerDay;
	}
	
	public boolean earlierThan(QueryTime cmpQueryTime) {
		return (this.time - cmpQueryTime.time < (cmpQueryTime.overnights-this.overnights) * ModelBuilder.secondsPerDay);
	}
	
	public boolean laterThan(QueryTime cmpQueryTime) {
		return (this.time - cmpQueryTime.time > (cmpQueryTime.overnights-this.overnights) * ModelBuilder.secondsPerDay);
	}
	
	public boolean equals(QueryTime cmpQueryTime) {
		return (this.time - cmpQueryTime.time == (cmpQueryTime.overnights-this.overnights) * ModelBuilder.secondsPerDay);
	}
	
	public int differenceFrom(QueryTime cmpQueryTime) {
		return this.time - cmpQueryTime.time + (this.overnights - cmpQueryTime.overnights) * ModelBuilder.secondsPerDay;
	}
	
	public static QueryTime MAX_VALUE() {
		QueryTime max = new QueryTime(Integer.MAX_VALUE, null, 0);
		return max;
	}
	
	public static QueryTime MAX_VALUE(String day) {
		QueryTime max = new QueryTime(Integer.MAX_VALUE, day, 0);
		return max;
	}
	
	public static QueryTime MIN_VALUE(String day) {
		QueryTime max = new QueryTime(-Integer.MAX_VALUE, day, 0);
		return max;
	}
	
	public void moveForward(int delta) {
		time += delta;
		
		this.within24Hours();
	}
	
	public void moveBackward(int delta) {
		time -= delta;
		
		this.within24Hours();
	}
	
	public int compareTo(QueryTime cmpObj) {
		if (differenceFrom(cmpObj) < 0) {
			return -1;
		}
		else if (differenceFrom(cmpObj) > 0) {
			return 1;
		}
		return 0;
	}
	
	public static QueryTime fromUtc(String utc) {
		List<String> weekDays = new ArrayList<String>();
		weekDays.add("Sunday");
		weekDays.add("Monday");
		weekDays.add("Tuesday");
		weekDays.add("Wednesday");
		weekDays.add("Thursday");
		weekDays.add("Friday");
		weekDays.add("Saturday");
		
		DateTime dt = new DateTime(utc);
		QueryTime qt = new QueryTime(dt.getSecondOfDay(), weekDays.get(dt.getDayOfWeek()), 0);
		
//		System.out.println(utc + ", " + qt.time + ", " + qt.day);
		
		return qt;
	}
	
	public void printIt() {
		System.out.println(TimeTransformer.IntegerToString(time) + ", " + day);
	}
	
	
	
	public static void main(String[] args) {
		QueryTime qt = new QueryTime(50000, "Tuesday", 0);
		qt.printIt();
		qt.moveForward(40000);
		qt.printIt();
		qt.moveBackward(40000);
		qt.printIt();
	}
}

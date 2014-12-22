package com.yimei.util;

import org.joda.time.DateTime;

// "NULL" is later than any specified date
public class Utc {
	public static long compare(String utc1, String utc2) {
		if (utc1.equals("NULL")) {
			if (utc2.equals("NULL")) {
				return 0; // the same
			}
			else {
				return 1; // a positive value indicates utc1 > utc2
			}
		}
		else if (utc2.equals("NULL")) {
			return -1; // a negative value indicates utc1 < utc2
		}
		else {
			DateTime dt1 = new DateTime(utc1);
			DateTime dt2 = new DateTime(utc2);
			long diff = dt1.getMillis()-dt2.getMillis();
			
			return diff;
		}
	}
	
//	public static void main(String[] args) {
//		DateTime dt = new DateTime();
//		int seconds = 3600 * dt.getHourOfDay() + 60 * dt.getMinuteOfHour() + dt.getSecondOfMinute();
//		System.out.println(dt.getSecondOfDay() + ", " + seconds + ", " + dt.getHourOfDay() + "." + dt.getMinuteOfHour());
//	}
}

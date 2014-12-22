package com.yimei.util;

/***
 * Transform time between string "HH:MM:SS" and integer
 * @author Administrator
 *
 */

public class TimeTransformer {
	
	public static int StringToInteger(String timeString) {
		String[] fields = timeString.split(":");
        if (fields.length != 3) {
            System.out.println("Wrong time string format!");
            System.exit(0);
        }

        int hours = Integer.parseInt(fields[0]);
        int minutes = Integer.parseInt(fields[1]);
        int seconds = Integer.parseInt(fields[2]);
        int val = (hours * 60 * 60) + minutes * 60 + seconds;
        
        return val;
	}

	public static String IntegerToString(int val) {
		int hours = val/3600;
		int minutes = (val-3600*hours)/60;
		int seconds = val-3600*hours-60*minutes;
		
		String hourStr = Integer.toString(hours);
		if (hours < 10)
			hourStr = "0" + hourStr;
		
		String minsStr = Integer.toString(minutes);
		if (minutes < 10)
			minsStr = "0" + minsStr;
		
		String secsStr = Integer.toString(seconds);
		if (seconds < 10)
			secsStr = "0" + secsStr;
		
		String str = hourStr + ":" + minsStr + ":" + secsStr;
		return str;
	}
}

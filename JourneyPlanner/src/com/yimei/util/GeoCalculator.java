package com.yimei.util;

import org.json.JSONArray;

import com.yimei.test.API;

public class GeoCalculator {
	/**
	 * <p>This routine calculates the distance between two points (given the
	 * latitude/longitude of those points). It is being used to calculate
	 * the distance between two locations.</p>
	 * 
	 * <p>Definitions: South latitudes are negative, east longitudes are positive</p>
	 * 
	 * <p>Passed to function:
	 * <ul>
	 *      <li>lat1, lon1 = Latitude and Longitude of point 1 (in decimal degrees)</li>
	 *      <li>lat2, lon2 = Latitude and Longitude of point 2 (in decimal degrees)</li>
	 *      <li>unit = the unit you desire for results
	 *          <ul>
	 *              <li>where: 'M' is statute miles</li>
	 *              <li>'K' is kilometers (default) </li>
	 *              <li>'N' is nautical miles</li>
	 *          </ul>
	 *      </li>
	 * </ul>
	 * Worldwide cities and other features databases with latitude longitude
	 * are available at http://www.geodatasource.com</p>
	 * 
	 * <p>For enquiries, please contact sales@geodatasource.com</p>
	 * <p>Official Web site: http://www.geodatasource.com</p>
	 * <p>GeoDataSource.com (C) All Rights Reserved 2013</p>
	 * 
	 * 
	 * @param lat1 - latitude point 1
	 * @param lon1 - longitude point 1
	 * @param lat2 - latitude point 2
	 * @param lon2 - longitude point 2
	 * @param unit - unit of measure (M, K, N)
	 * @return the distance between the two points
	 * @throws Exception 
	 */
	
	public static void main(String[] args) throws Exception {
		double lat1 = Math.random();
		double lon1 = Math.random();
		double lat2 = Math.random();
		double lon2 = Math.random();
		
		double d1 = distance(lat1, lon1, lat2, lon2, 'K');
		double d2 = distFrom(lat1, lon1, lat2, lon2);
		System.out.println("d1 = " + d1 + ", d2 = " + d2);

	}
	
	public static double distFrom(double lat1, double lng1, double lat2, double lng2) {
	    double earthRadius = 6371; //kilometers
	    double dLat = Math.toRadians(lat2-lat1);
	    double dLng = Math.toRadians(lng2-lng1);
	    double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
	               Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
	               Math.sin(dLng/2) * Math.sin(dLng/2);
	    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
	    double dist = earthRadius * c;

	    return dist;
	}
	
	public static final double distance(double lat1, double lon1, double lat2, double lon2, char unit)
	{
	    double theta = lon1 - lon2;
	    double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
	    dist = Math.acos(dist);
	    dist = rad2deg(dist);
	    dist = dist * 60 * 1.1515;
	     
	    if (unit == 'K') {
	        dist = dist * 1.609344;
	    }
	    else if (unit == 'N') {
	        dist = dist * 0.8684;
	    }
	     
	    return (dist);
	}
	 
	/**
	 * <p>This function converts decimal degrees to radians.</p>
	 * 
	 * @param deg - the decimal to convert to radians
	 * @return the decimal converted to radians
	 */
	private static final double deg2rad(double deg)
	{
	    return (deg * Math.PI / 180.0);
	}
	 
	/**
	 * <p>This function converts radians to decimal degrees.</p>
	 * 
	 * @param rad - the radian to convert
	 * @return the radian converted to decimal degrees
	 */
	private static final double rad2deg(double rad)
	{
	    return (rad * 180 / Math.PI);
	}
	

}

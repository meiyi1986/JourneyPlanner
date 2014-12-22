package au.com.jakebarnes.JourneyPlanner;

import java.util.ArrayList;

public class Path
{
	private ArrayList<Integer> stops;

	public ArrayList<Integer> getStops()
	{
		return stops;
	}

	public void addStop(int id)
	{
		stops.add(id);
	}

	public Path()
	{
		stops = new ArrayList<Integer>();
	}
}
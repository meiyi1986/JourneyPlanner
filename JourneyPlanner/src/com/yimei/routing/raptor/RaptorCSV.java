package com.yimei.routing.raptor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.opencsv.CSVReader;
import com.yimei.routing.core.Label;
import com.yimei.util.TimeTransformer;

public class RaptorCSV {

	String filename;
	List<JSONObject> JsonObjList = null;
	String from_stop_id;
	String[] keys;

	public RaptorCSV(String fname) {
		filename = fname;
	}

	public void setFilename(String fname) {
		filename = fname;
	}

	public void setfromStop(String from_stop_id) {
		this.from_stop_id = from_stop_id;
	}

	public void csvToJSON() throws JSONException, IOException {
		CSVReader reader = new CSVReader(new FileReader(this.filename));
		this.JsonObjList = Collections
				.synchronizedList(new ArrayList<JSONObject>());

		String[] nextLine;
		keys = reader.readNext();

		while ((nextLine = reader.readNext()) != null) {
			// nextLine[] is an array of values from the line
			JSONObject obj = new JSONObject();
			for (int i = 0; i < keys.length; i++)
				obj.put(keys[i], nextLine[i]);

			JsonObjList.add(obj);

		}

		reader.close();
	}

	public void sort(String key) {
		Collections.sort(this.JsonObjList, new Comparator<JSONObject>() {

			@Override
			public int compare(JSONObject o1, JSONObject o2) {
				try {
					int weight = 0;
					if (o1.get(key).equals(null) || o2.get(key).equals(null))
						return 0;
					weight = o1.get(key).toString()
							.compareTo(o2.get(key).toString());
					return weight;
				} catch (JSONException e) {
					e.printStackTrace();
				}
				return 0;
			}
		});

	}
	
	public void sort(String[] keys) {
		Collections.sort(this.JsonObjList, new Comparator<JSONObject>() {

			@Override
			public int compare(JSONObject o1, JSONObject o2) {
				try {
					int weight = 0;
					for(int i=0;i<keys.length -1;i++)
					{
					if (o1.get(keys[i]).equals(null) || o2.get(keys[i]).equals(null))
						continue;
					weight = o1.get(keys[i]).toString()
							.compareTo(o2.get(keys[i]).toString());
					if (weight == 0 && i != keys.length -1)
						continue;
					return weight;
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
				return 0;
			}
		});

	}

	public int getIndexOf(String key, String value) throws JSONException {
		Iterator<JSONObject> it = JsonObjList.iterator();
		int idx = 0;
		while (it.hasNext()) {
			JSONObject obj = it.next();
			if (obj.get(key).toString().toLowerCase()
					.compareTo(value.toLowerCase()) == 0)
				return idx;
			idx++;
		}
		return -1;
	}

	public JSONObject idx(int index) throws JSONException {
		Iterator<JSONObject> it = JsonObjList.iterator();
		int idx = 0;
		while (it.hasNext()) {
			JSONObject obj = it.next();
			if (idx == index)
				return obj;
			idx++;
		}
		return null;
	}

	public void addTripTime(FileOutputStream fos) throws JSONException,
			IOException {
		Thread thrs[] = new Thread[32];
		File file = new File("data/JSON/Adelaide/RaptorModel.json");
		RaptorModel model = RaptorModel.fromJSON(file);
		Iterator<JSONObject> it = JsonObjList.iterator();
		int idx = 0;
		ModifiedRaptor raptor = new ModifiedRaptor(model);

		// write first line
		boolean hasTravelTime=false;
		boolean hasNumTrips=false;
		String tmp = "";
		for (int i = 0; i < keys.length; i++) {
			tmp = tmp.concat('"' + keys[i] + '"' + ",");
			if (keys[i].equals("travelTime"))
				hasTravelTime = true;
			if (keys[i].equals("numTrips"))
				hasNumTrips = true;
		}
		if (!hasTravelTime)
		    tmp = tmp.concat('"' + "travelTime" + '"' + ",");
		if (!hasNumTrips)
		    tmp = tmp.concat('"' + "numTrips" + '"' + "\n");

		fos.write(tmp.getBytes());

		
		while (it.hasNext()) {
			JSONObject obj = it.next();

			String from_stop_id = this.from_stop_id;
			String to_stop_id = obj.get("stop_id").toString();

			Runnable r1 = new Runnable() {
				public void run() {
					try {
						Map<String, Label> stopLabels = raptor.getStopLabels(
								from_stop_id, to_stop_id);
						if (stopLabels == null) {
							// System.out.println("to_stop_id:" +to_stop_id+
							// " is not in the model");
							return;
						}
						String travelTime = TimeTransformer
								.IntegerToString(raptor
										.getTravelTime(stopLabels));
						int numTrips = raptor.getNumofTrips(stopLabels);
						obj.put("travelTime", travelTime);
						obj.put("numTrips", Integer.toString(numTrips));

						String tmp = "";
						for (int i = 0; i < keys.length; i++)
							tmp = tmp.concat('"' + obj.get(keys[i]).toString()
									+ '"' + ",");
						
						tmp = tmp.concat('"' + obj.get("travelTime").toString()
								+ '"' + ",");
						tmp = tmp.concat('"' + obj.get("numTrips").toString()
								+ '"' + "\n");
						synchronized(fos){
						fos.write(tmp.getBytes());
						}
						System.out.println(tmp);

					} catch (JSONException | IOException iex) {
					}
				}
			};/*end of runnable*/
			thrs[idx % 32] = new Thread(r1);
			thrs[idx % 32].start();
			try {
				if ((idx + 1) % 32 == 0) {
					for (int i = 0; i < 32; i++)
						thrs[i].join();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			idx++;
		}
	}

	public void csvWrite(FileOutputStream fos) throws IOException,
			JSONException {
		Iterator<JSONObject> it = JsonObjList.iterator();
		// write first line
		String tmp = "";
		for (int i = 0; i < keys.length; i++) {
			if (i < keys.length - 1)
				tmp = tmp.concat('"' + keys[i] + '"' + ",");
			else
				tmp = tmp.concat('"' + keys[i] + '"' + "\n");
		}
		fos.write(tmp.getBytes());

		while (it.hasNext()) {
			JSONObject obj = it.next();
			String str = "";
			for (int i = 0; i < keys.length; i++) {
				if (i < keys.length - 1)
					str = str.concat('"' + obj.get(keys[i]).toString() + '"'
							+ ",");
				else
					str = str.concat('"' + obj.get(keys[i]).toString() + '"'
							+ "\n");
			}
			fos.write(str.getBytes());
			System.out.println(str);
		}
	}
}

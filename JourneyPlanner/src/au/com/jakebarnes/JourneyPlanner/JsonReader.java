package au.com.jakebarnes.JourneyPlanner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.nio.charset.Charset;

import org.json.JSONException;
import org.json.JSONObject;

public class JsonReader {
	
	private static String readAll(Reader rd) throws IOException {
		StringBuilder sb = new StringBuilder();
		int cp;
		while ((cp = rd.read()) != -1) {
			sb.append((char) cp);
		}
		return sb.toString();
	}

	public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
		InputStream is = new URL(url).openStream();
		try {
			BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
			String jsonText = readAll(rd);
			JSONObject json = new JSONObject(jsonText);
			return json;
		} finally {
			is.close();
		}
	}
	
	// read from RMIT proxy: aproxy.rmit.edu.au:8080
	public static JSONObject readJsonFromUrlProxy(String url) throws IOException, JSONException {
         System.setProperty("http.proxyHost", "aproxy.rmit.edu.au");
         System.setProperty("http.proxyPort", "8080");
         System.setProperty("https.proxyHost", "aproxy.rmit.edu.au");
         System.setProperty("https.proxyPort", "8080");
         Authenticator.setDefault(new DummyAuthenticator());
         
         JSONObject json = readJsonFromUrl(url);
         return json;
	}
	
	private static class DummyAuthenticator extends Authenticator {
		public PasswordAuthentication getPasswordAuthentication() {
			return new PasswordAuthentication(
					"e04499", "Godreigns4ever".toCharArray()
				);
		}
	}

	public static void main(String[] args) throws IOException, JSONException {
		JSONObject json = readJsonFromUrlProxy("https://graph.facebook.com/19292868552");
		System.out.println(json.toString());
		System.out.println(json.get("id"));
	}
}
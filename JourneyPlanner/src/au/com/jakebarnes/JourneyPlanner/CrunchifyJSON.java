package au.com.jakebarnes.JourneyPlanner;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;

import org.json.JSONArray;
import org.json.JSONException;
 
public class CrunchifyJSON {
 
    public static void main(String[] args) {
        String jsonString = callURL("http://cdn.crunchify.com/wp-content/uploads/code/jsonArray.txt");
        System.out.println("\n\njsonString: " + jsonString);
 
// Replace this try catch block for all below subsequent examples
        try {  
            JSONArray jsonArray = new JSONArray(jsonString);
            System.out.println("\n\njsonArray: " + jsonArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
 
    public static String callURL(String myURL) {
//        System.out.println("Requested URL:" + myURL);
        StringBuilder sb = new StringBuilder();
        URLConnection urlConn = null;
        InputStreamReader in = null;
        try {
            URL url = new URL(myURL);
            urlConn = url.openConnection();
            if (urlConn != null)
                urlConn.setReadTimeout(60 * 1000);
            if (urlConn != null && urlConn.getInputStream() != null) {
                in = new InputStreamReader(urlConn.getInputStream(),
                        Charset.defaultCharset());
                BufferedReader bufferedReader = new BufferedReader(in);
                if (bufferedReader != null) {
                    int cp;
                    while ((cp = bufferedReader.read()) != -1) {
                        sb.append((char) cp);
                    }
                    bufferedReader.close();
                }
            }
        in.close();
        } catch (Exception e) {
            throw new RuntimeException("Exception while calling URL:"+ myURL, e);
        }
        
//        System.out.println(sb.toString());
 
        return sb.toString();
    }
    
    public static String callURLProxy(String myURL) {
    	System.setProperty("http.proxyHost", "aproxy.rmit.edu.au");
        System.setProperty("http.proxyPort", "8080");
        System.setProperty("https.proxyHost", "aproxy.rmit.edu.au");
        System.setProperty("https.proxyPort", "8080");
        Authenticator.setDefault(new DummyAuthenticator());
        
//        System.out.println("proxy");
        
        String jsonString = callURL(myURL);
        return jsonString;
    }
 
    private static class DummyAuthenticator extends Authenticator {
		public PasswordAuthentication getPasswordAuthentication() {
			return new PasswordAuthentication(
					"e04499", "Godreigns4ever".toCharArray()
				);
		}
	}
}

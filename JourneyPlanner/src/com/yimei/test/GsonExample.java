package com.yimei.test;

import java.io.FileWriter;
import java.io.IOException;

import com.google.gson.Gson;
 
public class GsonExample {
    public static void main(String[] args) {
 
    	Employee e = new Employee();
        e.name = "Reyan Ali";
        e.address = "Phokka Kuan, Ambehta Peer";
        e.SSN = 11122333;
        e.number = 101;
        
    	Gson gson = new Gson();
     
    	// convert java object to JSON format,
    	// and returned as JSON formatted string
    	String json = gson.toJson(e);
     
    	try {
    		//write converted json data to a file named "file.json"
    		FileWriter writer = new FileWriter("file.json");
    		writer.write(json);
    		writer.close();
     
    	} catch (IOException e1) {
    		e1.printStackTrace();
    	}
     
    	System.out.println(json);
 
    }
}

package com.yimei.test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import com.google.gson.Gson;
 
public class FromGsonExample {
    public static void main(String[] args) {
 
	Gson gson = new Gson();
 
	try {
 
		BufferedReader br = new BufferedReader(
			new FileReader("file.json"));
 
		//convert the json string back to object
		Employee obj = gson.fromJson(br, Employee.class);
 
		System.out.println(obj.name + ", " + obj.address + ", " + obj.number);
 
	} catch (IOException e) {
		e.printStackTrace();
	}
 
    }
}

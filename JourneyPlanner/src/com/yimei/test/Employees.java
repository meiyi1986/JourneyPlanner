package com.yimei.test;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;

import org.mapdb.Fun;
import org.mapdb.Fun.Tuple2;

import com.google.common.collect.Maps;

public class Employees implements Serializable {
	private Map<Tuple2, Employee> list;
	
	public Employees() {
		list = Maps.newHashMap();
	}
	
	public void addEmployee(Employee e) {
		Tuple2 tt = new Fun.Tuple2(e.name, e.number);
		list.put(tt, e);
	}
	
	public Map<Tuple2, Employee> getList() {
		return list;
	}
	
	// serialization methods
	public void deserialize(String fileName) {
		Employees es = null;
	      try
	      {
	         FileInputStream fileIn = new FileInputStream(fileName);
	         ObjectInputStream in = new ObjectInputStream(fileIn);
	         es = (Employees) in.readObject();
	         in.close();
	         fileIn.close();
	      }catch(IOException i)
	      {
	         i.printStackTrace();
	      }catch(ClassNotFoundException c)
	      {
	         System.out.println("Employee class not found");
	         c.printStackTrace();
	      }
	      
	      this.list = es.getList();
    }

    public void serialize(String fileName){
    	try
        {
           FileOutputStream fileOut =
           new FileOutputStream(fileName);
           ObjectOutputStream out = new ObjectOutputStream(fileOut);
           out.writeObject(this);
           out.close();
           fileOut.close();
           System.out.printf("Serialized data is saved in " + fileName);
        }catch(IOException i)
        {
            i.printStackTrace();
        }
    }
}

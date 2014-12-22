package com.yimei.test;

import java.io.*;

import com.google.gson.Gson;

public class SerializeDemo2
{
   public static void main(String [] args)
   {
	   Employees es = new Employees();
	   
      Employee e = new Employee();
      e.name = "Reyan Ali";
      e.address = "Phokka Kuan, Ambehta Peer";
      e.SSN = 11122333;
      e.number = 101;
      es.addEmployee(e);
      
      Employee e2 = new Employee();
      e2.name = "Aeit";
      e2.address = "Aenteodtwd";
      e2.number = 202;
      es.addEmployee(e2);
      
      Gson gson = new Gson();
      String json = gson.toJson(es);
      
      System.out.println(json);
      
//      try
//      {
//         FileOutputStream fileOut =
//         new FileOutputStream("/employees.ser");
//         ObjectOutputStream out = new ObjectOutputStream(fileOut);
//         out.writeObject(es);
//         out.close();
//         fileOut.close();
//         System.out.printf("Serialized data is saved in /employees.ser");
//      }catch(IOException i)
//      {
//          i.printStackTrace();
//      }
   }
}
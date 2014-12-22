package com.yimei.test;

import java.io.*;
public class DeserializeDemo2
{
   public static void main(String [] args)
   {
//      Employees es = null;
//      try
//      {
//         FileInputStream fileIn = new FileInputStream("/employees.ser");
//         ObjectInputStream in = new ObjectInputStream(fileIn);
//         es = (Employees) in.readObject();
//         in.close();
//         fileIn.close();
//      }catch(IOException i)
//      {
//         i.printStackTrace();
//         return;
//      }catch(ClassNotFoundException c)
//      {
//         System.out.println("Employee class not found");
//         c.printStackTrace();
//         return;
//      }
      
	   Employees es = new Employees();
	   es.deserialize("/employees.ser");
	   
      int employeeNum = es.getList().size();
      
      for (int i = 0; i < employeeNum; i++) {
    	  Employee e = es.getList().get(i);
    	  System.out.println("Deserialized Employee..." + " " + i);
          System.out.println("Name: " + e.name);
          System.out.println("Address: " + e.address);
          System.out.println("SSN: " + e.SSN);
          System.out.println("Number: " + e.number);
      }
    }
}

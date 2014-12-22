package com.yimei.test;

public class Employee
{
   public String name;
   public String address;
   public int SSN;
   public int number;
   public void mailCheck()
   {
      System.out.println("Mailing a check to " + name
                           + " " + address);
   }
}
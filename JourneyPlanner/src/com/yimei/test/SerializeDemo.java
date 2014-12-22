package com.yimei.test;

import java.io.*;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import com.google.common.collect.Maps;
import com.google.gson.Gson;

public class SerializeDemo
{
   public static void main(String [] args)
   {
   
      Employee e = new Employee();
      e.name = "Reyan Ali";
      e.address = "Phokka Kuan, Ambehta Peer";
      e.SSN = 11122333;
      e.number = 101;
      Gson gson = new Gson();
      String json = gson.toJson(e);
      System.out.println(json);
      
      

   }
}
package com.yimei.test;

import java.util.*;

import com.google.common.collect.Maps;
import com.yimei.routing.core.Label;

public class CollectionsDemo {
   public static void main(String args[]) { 
      // create link list object 
      Map<String , Label> map = Maps.newHashMap();
      
      map.put("sg", new Label("24"));
      
      Label k = map.get("sg1");
      System.out.println(k.getStopId());
   }  
}

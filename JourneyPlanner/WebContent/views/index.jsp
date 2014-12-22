<%@ page language="java" contentType="text/html; charset=UTF-8"
  pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
  <head>
    <meta charset="utf-8" />
    <meta name="viewport" content="initial-scale=1.0, user-scalable=no" />
    <title>Journey Planner &ndash; Jake Barnes</title>
    <!-- jQuery -->
    <link rel="stylesheet" href="http://code.jquery.com/ui/1.10.3/themes/ui-lightness/jquery-ui.css">
    <script src="http://ajax.googleapis.com/ajax/libs/jquery/2.0.3/jquery.min.js"></script>
    <script src="http://code.jquery.com/ui/1.10.3/jquery-ui.js"></script>
    <!-- Google Maps API -->
    <script src="https://maps.googleapis.com/maps/api/js?key=AIzaSyDX4taVnGzTRopaLtDnFwCiXv7SSPMFWXs&amp;sensor=false&amp;region=AU"></script>
    <!-- our files -->
    <link rel="stylesheet" type="text/css" href="<c:url value="/static/css/reset.css" />" />
    <link rel="stylesheet" type="text/css" href="<c:url value="/static/css/template.css" />" />
    <script src="<c:url value="/static/js/CheckboxControl.js" />"></script>
    <script src="<c:url value="/static/js/main.js" />"></script>
  </head>
  <body>
    <div id="map-canvas"></div>
    <div id="side">
      <p id="status" class="ui-state-error ui-corner-all">Test error</p>
      <h2>Search</h2>
      <p><input type="text" id="from" placeholder="From" /></p>
      <p><input type="text" id="to" placeholder="To" /></p>
      <p><input type="date" id="date" /></p>
      <p><input type="time" id="time" /></p>
      <p><button id="search">Search</button></p>
      <p id="output"></p>
      <h2>Results</h2>
      <div id="results"></div>
    </div>
    
  </body>
</html>

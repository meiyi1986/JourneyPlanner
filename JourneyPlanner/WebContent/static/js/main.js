// rendering
var map;
var l_stops = [], l_links = [], l_paths = [];
var v_stops = true, v_links = true;
var icon = { url : "static/img/circle5.png", size : new google.maps.Size(5, 5),
  anchor : new google.maps.Point(3, 3) };
var arrow = { path : google.maps.SymbolPath.FORWARD_OPEN_ARROW, scale : 2 };
var linkColors = [ { strokeColor : '#40C0A0' }, { strokeColor : '#F04000' },
    { strokeColor : '#0040F0' }, { strokeColor : '#60B040' } ];
var resultColors = [ { strokeColor : '#FFA500', strokeOpacity : 0.7 },
    { strokeColor : '#0040F0', strokeOpacity : 0.3 },
    { strokeColor : '#F04000', strokeOpacity : 0.4 },
    { strokeColor : '#8040C0', strokeOpacity : 0.3 },
    { strokeColor : '#40C0A0', strokeOpacity : 0.6 } ];

// planning
var from = -1, to = -1; // the origin and destination stop ids
var utc; // the utc time

var autoCompleteResults = []; // the results of autocomplete

var journey; // the journey information
var journeyList; // the journey list

// stop and route information
var stopInfo = [], routeInfo = [];

// init
function initialize()
{
	var mapOptions = {
			// basic
			center : new google.maps.LatLng(-34.928809, 138.599934),
			zoom : 11,
			// controls
			panControl : false,
			streetViewControl : false,
			mapTypeControl : true,
			// controls options
			zoomControlOptions : { 
				style : google.maps.ZoomControlStyle.SMALL,
				position : google.maps.ControlPosition.LEFT_TOP 
			},
			mapTypeControlOptions : {
				style : google.maps.MapTypeControlStyle.HORIZONTAL_BAR,
				position : google.maps.ControlPosition.TOP_LEFT 
			}
		};

	// create map
	map = new google.maps.Map($('#map-canvas')[0], mapOptions);

	// layer controls
	cb1 = new CheckboxControl('Stops', true, function(v) {
		setLayer(l_stops, v_stops = v);
    });
	cb2 = new CheckboxControl('Links', true, function(v) {
		setLayer(l_links, v_links = v);
	});
  
	map.controls[google.maps.ControlPosition.LEFT_BOTTOM].push(cb2.getDiv());
	map.controls[google.maps.ControlPosition.LEFT_BOTTOM].push(cb1.getDiv());

	// side controls
	$('button').button();
	$('#search').click(search);
	$('#results').accordion({ 
		active : false, 
		collapsible : true, 
		heightStyle : "content" 
	});
	$('#from').autocomplete({ 
		delay : 0, 
		source : sql_stopname_search, 
		select : function(event, ui) {
			event.preventDefault();
			$('#from').val(ui.item.label);
			$('#from').addClass('valid');
			from = ui.item.value;
		}, 
		search : function(event, ui) {
			$('#from').removeClass('valid');
			from = -1;
		}, 
		change : function(event, ui) {
			if (ui.item = null) {
				$('#from').removeClass('valid');
				from = -1;
			}
		}, 
		focus : function(event, ui) {
			event.preventDefault();
		}
	});
	$('#to').autocomplete({ 
		delay : 0, 
		source : sql_stopname_search, 
		select : function(event, ui) {
			event.preventDefault();
			$('#to').val(ui.item.label);
			$('#to').addClass('valid');
			to = ui.item.value;
		}, 
		search : function(event, ui) {
			$('#to').removeClass('valid');
			to = -1;
		}, 
		change : function(event, ui) {
			if (ui.item = null) {
				$('#to').removeClass('valid');
				to = -1;
			}
		}, 
		focus : function(event, ui) {
			event.preventDefault();
		} 
	});
	

//	$.ajax({
//		url : "/api/loadStops",
//		dataType : 'json',
//		success : loadStopInfo,
//		timeout : 30000
//	})
//	.fail(function(a, b, c) {
//		api_status("Load stop information time out.", true);
//	});
}

$(document).ready(initialize);

function loadStopInfo(data) {
	$.each(data, function(key, item) {
		document.getElementById("output").innerHTML = item.stop_id + ", " + item.stop_name + "<br>";
		addStopInfo(item.stop_id, item.stop_name, item.stop_lat, item.stop_lon);
	});
}

function addStopInfo(id, name, lat, lon) {
	var marker = new google.maps.Marker({
		position : new google.maps.LatLng(lat, lon),
		map : map,
		title : name + ' (' + id + ')',
		icon : icon,
		zIndex : 5
	});
	marker.id = id;
	marker.name = name;
	
	google.maps.event.addListener(marker, 'click', function() {
		api_select(id, name);
	})
	stopInfo.push(marker);
}

// callbacks

function search()
{
//	document.getElementById("output").innerHTML = from + ", " + to;
	
	if (from == to) {
		alert("The origin and destination are the same!");
		return;
	}
	
	// get the date and time in ISO8601 UTC format
	var dateString = document.getElementById("date").value;
	var timeString = document.getElementById("time").value;
	
	if (dateString == ""){ // no date entered, get the current date
		dateString = new Date();
		var dd = dateString.getDate();
		var mm = dateString.getMonth()+1; //January is 0!
		var yyyy = dateString.getFullYear();

		if(dd<10) {
		    dd='0'+dd;
		}

		if(mm<10) {
		    mm='0'+mm;
		} 

		dateString = yyyy+'-'+mm+'-'+dd;
	}
	
	if (timeString == ""){ // no time entered, get the current time
	    var currDate = new Date();
		var hh = currDate.getHours();
		var mm = currDate.getMinutes();
		var ss = currDate.getSeconds();
		timeString = hh + ":" + mm + ":" + ss;
	}
	
	var dtString = dateString + " " + timeString;
	utc = new Date(dtString);
	utc = toISOString(utc);

//	document.getElementById("output").innerHTML = "from " + from + ", to " + to + " at " + utc + "<br>";
	
	if (from == -1) { // there is no origin input
		$('#from').focus();
	    alert("Unknown origin stop!");
	    return;
	}
	
	if (to == -1) { // there is no destination input
		$('#to').focus();
		alert("Unknown destination stop!");
		return;
	}

	//  $('#search').button("option", "disabled", true);
//	document.getElementById("output").innerHTML = "start ajax" + "<br>";
	$.ajax({ 
		url : 'api/search', 
		data : {fromStopId : from, toStopId : to, utc : utc}, 
		dataType : 'json', 
		success : showJourneys,
		timeout : 10000
	})
	.fail(function(a, b, c) {
		api_status("Couldn't load results.", true);
	});
}

function showJourneys(data) { // data is an array of journey objects
//	document.getElementById("output").innerHTML = "Start showing results <br>";
	api_path_clear();
	var currResId = 0;
	$.each(data, function(key, item) {
		currResId ++;
		
		// this journey
		$('#results').append(
				'<h3><label for="result' + currResId + '"><input type="checkbox" id="result' + currResId
				+ '" checked />Result #' + currResId + '</label></h3>');
		var content = $('<div></div>');
		var list = $('<ul></ul>');
		
		journey = [];
		var currPath = [];
		
		// key = sequence; item = [stop_obj_1, stop_obj_2, ...]
//		document.getElementById("output").innerHTML += key + ": " + item + "<br>";
		
		$.each(item, function(key, item) {
			
			// key = 0, 1, ...; item = ith journey events
//			document.getElementById("output").innerHTML += key + ": " + item + "<br>";
			
//			$.each(item, function(key, item) {
//				document.getElementById("output").innerHTML += key + ": " + item + "<br>";
//			});
			
			// handle this journey event
			var jeFrom = item.fromStop;
			var jeTo = item.toStop;
			var jeTrajectory = item.trajectory;
			
//			document.getElementById("output").innerHTML += item.tripId + ", " + from.stop_name + "<br>";
			
			var jeContext = journeyEventContext(item);
			
//			document.getElementById("output").innerHTML += jeContext + '<br>';
			
			// append result list
			list.append('<li>' + jeContext + '</li>');
			
			// add marker
			$.each(jeTrajectory, function(key, item) {
				var tmpMarker = new google.maps.LatLng(item.lat, item.lon);
				currPath.push(tmpMarker);
			});
//			if (key == 0) {
//				var tmpMarker = new google.maps.LatLng(jeFrom.stop_lat, jeFrom.stop_lon);
//				currPath.push(tmpMarker);
//			}
//			
//			var tmpMarker = new google.maps.LatLng(jeTo.stop_lat, jeTo.stop_lon);
//			currPath.push(tmpMarker);
		});
		

//		$.each(item, function(key, item) {
////			document.getElementById("output").innerHTML += key + ": " + item + "<br>";
//			
//			if (item.hasOwnProperty("time_timetable_utc")) { // this is a departure
//				var lat = item.platform.stop.lat;
//				var lon = item.platform.stop.lon;
//				var stopName = item.platform.stop.location_name;
//				
////				document.getElementById("output").innerHTML += stopName + ": " + "(" + lat + ", " + lon + ")" + "<br>";
//				
//				// append result list
//				list.append('<li>' + stopName + '</li>');
//				
//				// add marker
//				var tmpMarker = new google.maps.LatLng(lat, lon);
//				currPath.push(tmpMarker);
//			}
//			else { // this is the final destination stop
//				var lat = item.lat;
//				var lon = item.lon;
//				var stopName = item.location_name;
//				
////				document.getElementById("output").innerHTML += stopName + ": " + "(" + lat + ", " + lon + ")" + "<br>";
//				
//				// append result list
//				list.append('<li>' + stopName + '</li>');
//				
//				// add marker
//				var tmpMarker = new google.maps.LatLng(lat, lon);
//				currPath.push(tmpMarker);
//			}
//		});
		
		// add the current path to l_paths
		l_paths.push(GoogleMapPath(currPath));
		
		content.append(list);
		$('#results').append(content);
	});
	$('#results').accordion('refresh');
	$('#results input[type="checkbox"]').click(updatePaths);
	updatePaths();
	if (data.length == 0)
		api_status('No results found.', false);
}

function GoogleMapPath(path) {
	 var gMPath = new google.maps.Polyline({
		path : path, 
		map : map,
	    strokeColor : '#40a040', 
	    strokeOpacity : 0.5, 
	    strokeWeight : 8, 
	    zIndex : 2,
	    icons : [ { icon : arrow, offset : '100%' } ], 
	    visible : false });
	 
	 return gMPath;
}

function process_results(data)
{
	api_path_clear();
	$.each(data, function(key, item) {
		api_path_add(item.stops);
	});
	$('#results').accordion('refresh');
	$('#results input[type="checkbox"]').click(updatePaths);
	updatePaths();
	if (data.length == 0)
		api_status('No results found.', false);
}

// api

function api_log(text)
{
	// thanks IE
	if (typeof (console) != 'undefined')
		console.log(text);
}

function api_status(text, error)
{
	$('#status').removeClass('ui-state-error ui-state-highlight');
	$('#status').addClass(error ? 'ui-state-error' : 'ui-state-highlight');
	if (typeof (text) == 'undefined') {
		$('#status').text('');
		$('#status').hide();
	}
	else {
		$('#status').text(text);
		$('#status').show();
	}
}

function api_select(id, name)
{
  if (from != -1 && to != -1)
  {
    $('#from, #to').val('');
    $('#from, #to').removeClass('valid');
    from = to = -1;
  }

  if (from == -1)
  {
    $('#from').val(name);
    $('#from').addClass('valid');
    from = id;
  }
  else if (to == -1)
  {
    $('#to').val(name);
    $('#to').addClass('valid');
    to = id;
  }
}

function api_stop_clear()
{
  while (l_stops.length)
  {
    l_stops[0].setMap(null);
    l_stops.shift();
  }
}

function api_stop_add(id, name, lat, lng, mode)
{
//  var marker = new google.maps.Marker({
//    position : new google.maps.LatLng(lat, lng), 
//    map : v_stops ? map : null,
//    title : name + ' (' + id + ')', 
//    icon : icon, 
//    zIndex : 5 
//    });
//  
//  marker.id = id;
//  marker.name = name;
//  marker.mode = mode;
//  google.maps.event.addListener(marker, 'click', function()
//  {
//    api_select(id, name);
//  });
	
  var marker = {
		  id: id,
		  name: name,
		  mode: mode,
		  position : new google.maps.LatLng(lat, lng)
  };
  
  l_stops.push(marker);
}

function api_stop_search(part, cb)
{
	// get the stop names and ids from the PTV search to autoCompleteResults
	PTVHealthCheck();
	$.ajax({ 
		url : 'api/PTVSearch', 
		data : {search: part.term}, 
		dataType : 'json', 
		success : getAutoCompleteStops,
		timeout : 10000 
	})
	.fail(function(a, b, c) {
		api_status("Search failed!", true);
	})
	.complete(function(d,e) {
		cb(autoCompleteResults);
//		document.getElementById("output").innerHTML = "finish";
	});
}

function sql_stopname_search(part, cb)
{
	// get the stop names and ids from the sql to autoCompleteResults
	$.ajax({ 
		url : 'api/searhStopByName', 
		data : {subName: part.term}, 
		dataType : 'json', 
		success : getAutoCompleteStops,
		timeout : 10000 
	})
	.fail(function(a, b, c) {
		api_status("Search failed!", true);
	})
	.complete(function(d,e) {
		cb(autoCompleteResults);
//		document.getElementById("output").innerHTML = "finish";
	});
}

function sql_stopid_search(input)
{
	// get the stop names and ids from the sql to autoCompleteResults
	$.ajax({ 
		url : 'api/searhStopById', 
		data : {id: input}, 
		dataType : 'json', 
		success : function(data) {
			return data;
		},
		timeout : 10000 
	})
	.fail(function(a, b, c) {
		api_status("Stop id search failed!", true);
	});
}

function api_link_clear()
{
  while (l_links.length)
  {
    l_links[0].setMap(null);
    l_links.shift();
  }
}

function api_link_add(from, to)
{
  from_ = getStop(from);
  to_ = getStop(to);
  var line = new google.maps.Polyline({
    path : [ from_.position, to_.position ], map : v_links ? map : null,
    strokeOpacity : 0.5, strokeWeight : 2,
    /* icons : [ { icon : arrow, offset : '100%' } ], */zIndex : 4 });
  line.setOptions(getLinkColor(from_.mode, to_.mode));
  l_links.push(line);
}

function api_path_clear()
{
  while (l_paths.length)
  {
    l_paths[0].setMap(null);
    l_paths.shift();
  }
  $('#results').empty();
}

function api_path_add(stack)
{
  var pos = [];
  for (var k = 0; k < stack.length; k++)
    pos.push(getStop(stack[k]).position);
  l_paths.push(new google.maps.Polyline({ path : pos, map : map,
    strokeColor : '#40a040', strokeOpacity : 0.5, strokeWeight : 8, zIndex : 2,
    icons : [ { icon : arrow, offset : '100%' } ], visible : false }));
  var n = l_paths.length;
  $('#results').append(
      '<h3><label for="result' + n + '"><input type="checkbox" id="result' + n
          + '" checked />Result #' + n + '</label></h3>');
  var content = $('<div></div>');
  var list = $('<ul></ul>');
  for (var k = 0; k < stack.length; k++)
    list.append('<li>' + getStop(stack[k]).name + '</li>');
  content.append(list);
  $('#results').append(content);
}

// util

function getStop(id)
{
  for (var i = 0; i < l_stops.length; i++)
    if (l_stops[i].id == id)
      return l_stops[i];
}

function getLinkColor(from, to)
{
  if (from == to)
    return linkColors[from];
  else
    return linkColors[0];
}

function updatePaths(e)
{
  if (typeof (e) != 'undefined')
    e.stopPropagation();

  setLayer(l_paths, false);
  var bounds = getBounds(l_paths[0]);
  for (var i = 0; i < l_paths.length; i++)
  {
    if ($('#result' + (i + 1)).prop('checked'))
    {
      l_paths[i].setOptions(resultColors[i % resultColors.length]);
      l_paths[i].setVisible(true);
      bounds = bounds.union(getBounds(l_paths[i]));
    }
  }
  map.fitBounds(bounds);
}

function showPath(n)
{
  setLayer(l_paths, false);
  l_paths[n - 1].setVisible(true);
  map.fitBounds(getBounds(l_paths[n - 1]));
}

function getBounds(line)
{
  var path = line.getPath().getArray();
  var lat_lo = 9999, lat_hi = -9999;
  var lng_lo = 9999, lng_hi = -9999;
  for (var i = 0; i < path.length; i++)
  {
    if (path[i].lat() < lat_lo)
      lat_lo = path[i].lat();
    if (path[i].lat() > lat_hi)
      lat_hi = path[i].lat();
    if (path[i].lng() < lng_lo)
      lng_lo = path[i].lng();
    if (path[i].lng() > lng_hi)
      lng_hi = path[i].lng();
  }

  return new google.maps.LatLngBounds(new google.maps.LatLng(lat_lo, lng_lo),
      new google.maps.LatLng(lat_hi, lng_hi));
}

function setLayer(layer, show)
{
  for (var i = 0; i < layer.length; i++)
    layer[i].setVisible(show);
}

/******************* Change date to ISO8601 UTC format *****************************/

function padzero(n) {
   return n < 10 ? '0' + n : n;
}

function pad2zeros(n) {
  if (n < 100) {
	n = '0' + n;
  }
  if (n < 10) {
    n = '0' + n;
  }
  return n;     
}

function toISOString(d) {
  return d.getUTCFullYear() + '-' +  padzero(d.getUTCMonth() + 1) + '-' + padzero(d.getUTCDate()) + 'T' 
  + padzero(d.getUTCHours()) + ':' +  padzero(d.getUTCMinutes()) + ':' + padzero(d.getUTCSeconds()) + '.' 
  + pad2zeros(d.getUTCMilliseconds()) + 'Z';
}

function fnToISO(datetime) {
  if (datetime == "")
	datetime = new Date();
  
  return toISOString(datetime);
}

/******************* PTV timetable API functions: Yi Mei - 21/08/2014 *****************************/

// do HealthCheck
function PTVHealthCheck() {
	var now = new Date(); // get
	now = fnToISO(now); // change to IOS8601 UTC format
	getJSON('api/PTVHealthCheck', {timestamp: now}, checkHealth, "Couldn't connect the server", 10);
}

function checkHealth(data) {
	$.each(data, function(key, item)
	{
		document.getElementById("output").innerHTML += key + " " + item + "<br>";
//		if (item != "true") {
//			document.getElementById("output").innerHTML = "!" + key + "<br>";
//		}
	});
}

// get the stop name and id of the JSON data along with type (maximal 10)
function getAutoCompleteStops(data) {
//	document.getElementById("output").innerHTML += "start <br>";
	autoCompleteResults = [];
	$.each(data, function(key, item) {
		var id = item.stop_id;
		var name = item.stop_name;
//		$.each(stop, function(key, item) {
//			
////			document.getElementById("output").innerHTML += key + "<br>";
//			
//			if (key == "stop_id") { // obtain the stop id
//				id = item;
//			}
//			
//			if (key == "stop_name") { // obtain the stop name
//				name = item;
//			}
//		});
		
		autoCompleteResults.push({label: name, value: id});
		
//		if (autoCompleteResults.length == 10)
//			break;
	});
}

//get the stop name and id of the JSON data along with type (maximal 10)
function showGtfsStops(data) {
//	document.getElementById("output").innerHTML += "start <br>";
	autoCompleteResults = [];
	$.each(data, function(key, item) {
		document.getElementById("output").innerHTML += key + ", " + item;
		var json = item;
		var resString;
//		$.each(json, function(key, item) {
////			document.getElementById("output").innerHTML += key + "<br>";
//			
//			if (key == "result") { // obtain the result string
////				document.getElementById("output").innerHTML += item + "<br>";
//				resString = item;
//			}
//			
////			document.getElementById("output").innerHTML += resString + "<br>";
//			
//			if (key == "type" && item == "stop") { // only select the stops
////				document.getElementById("output").innerHTML += item + "<br>";
//				autoCompleteResults.push({
//					label: resString.location_name + " [" + resString.transport_type + "]",
//					value: JSON.stringify(resString)
//				});
//			}
//		});
		
//		if (autoCompleteResults.length == 10)
//			break;
	});
}

function journeyEventContext(je) {
	var context = "";
	
	var depTitle = "Dep: ";
	var byTitle = "By: ";
	var ArrTitle = "Arr: ";
	
	context = context + depTitle.bold() + je.fromStop.stop_name + " (" + je.departureTimeStr + ") <br> ";
	context = context + "<b>By</b>: " + je.route + " <br> ";
	context = context + "<b>Arr</b>: " + je.toStop.stop_name + " (" + je.arrivalTimeStr + ")";
	
//	document.getElementById("output").innerHTML = context;
	
//	if (je.tripId == 'walking') {
//		context = "Walk";
//	}
//	else {
//		context = "Take trip " + je.tripId;
//	}
//	
//	context = context + " from <b>" + je.fromStop.stop_name + "</b> at " + je.departureTimeStr
//		+ " to <b>" + je.toStop.stop_name + "</b> at " + je.arrivalTimeStr;
	
	return context;
}

// Get the JSON object
function getJSON(url, params, success, errorMessage, timeout)
{
  return $.ajax({ 
	  url : url, 
	  data : params, 
	  dataType : 'json', 
	  success : success,
	  timeout : timeout * 1000 
	  })
	    .fail(function(a, b, c) {
		    api_status(errorMessage, true);
	    });
}

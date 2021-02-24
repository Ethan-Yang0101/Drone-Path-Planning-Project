package uk.ac.ed.inf.aqmaps;

import java.io.IOException;
import java.net.ConnectException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

/*
 * This class provides methods to get data from web server and 
 * provides necessary information needed for Drone to use drone control algorithm 
 * and provides necessary information needed for GeoJsonServer to write GeoJSON file.
 */
public class DataServer {

	private final String port_number; // A field refer to port number of web server.
	private final String year; // A field refer to year of DataServer connection.
	private final String month; // A field refer to month of DataServer connection.
	private final String day; // A field refer to day of DataServer connection.

	public DataServer(String year, String month, String day, String port_number) {
		this.port_number = port_number;
		this.year = year;
		this.month = month;
		this.day = day;
	}

	// This method connects to web server and gets JSON String from web server based
	// on specific URL String. URL String is the address of data on web server.
	private String get_json_string(String urlString) throws IOException, InterruptedException {
		try {
			HttpClient client = HttpClient.newHttpClient();
			HttpRequest request = HttpRequest.newBuilder().uri(URI.create(urlString)).build();
			HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
			return response.body();
		} catch (ConnectException e) {
			System.out.println("Fatal Error: Cannot Connect to " + urlString + " at port " + port_number);
			System.exit(1);
		}
		return null;
	}

	// This method gets sensors’ info and stores in SensorDetail objects.
	public ArrayList<SensorDetail> get_sensors_detail() throws IOException, InterruptedException {
		var urlString = "http://localhost:" + port_number + "/maps/" + year + "/" + month + "/" + day
				+ "/air-quality-data.json";
		String json_string = get_json_string(urlString);
		Type listType = new TypeToken<ArrayList<SensorDetail>>() {
		}.getType();
		ArrayList<SensorDetail> sensor_list = new Gson().fromJson(json_string, listType);
		return sensor_list;
	}

	// This method gets what3words info and stores in LocationDetail object.
	public LocationDetail get_location_detail(String W3W_Location) throws IOException, InterruptedException {
		var words = W3W_Location.replace('.', ',').split(",");
		var urlString = "http://localhost:" + port_number + "/words/" + words[0] + "/" + words[1] + "/" + words[2]
				+ "/details.json";
		String json_string = get_json_string(urlString);
		LocationDetail location = new Gson().fromJson(json_string, LocationDetail.class);
		return location;
	}

	// This method gets no fly zones info from web server and process data into a
	// list of list of points of no fly zones which will be used by Drone.
	public ArrayList<ArrayList<Point>> get_buildings_info() throws IOException, InterruptedException {
		ArrayList<ArrayList<Point>> buildings = new ArrayList<>();
		var urlString = "http://localhost:" + port_number + "/buildings/no-fly-zones.geojson";
		String json_string = get_json_string(urlString);
		var collection = FeatureCollection.fromJson(json_string);
		ArrayList<Feature> features = (ArrayList<Feature>) collection.features();
		for (int i = 0; i < features.size(); i++) {
			Feature feature = features.get(i);
			Polygon polygon = (Polygon) feature.geometry();
			List<List<Point>> point_list = polygon.coordinates();
			ArrayList<Point> points = (ArrayList<Point>) point_list.get(0);
			buildings.add(points);
		}
		return buildings;
	}

	// This method generates a HashMap which contains 33 what3words locations of
	// sensors as keys and corresponding points as values.
	public HashMap<String, Point> get_sensors_info() throws IOException, InterruptedException {
		HashMap<String, Point> sensors_info = new HashMap<>();
		ArrayList<SensorDetail> sensors = get_sensors_detail();
		for (SensorDetail sensor : sensors) {
			String location = sensor.getLocation();
			LocationDetail localDetail = get_location_detail(location);
			double longitude = localDetail.getCoordinates().getLng();
			double latitude = localDetail.getCoordinates().getLat();
			sensors_info.put(location, Point.fromLngLat(longitude, latitude));
		}
		return sensors_info;
	}

	// This method generates a list of corners of confinement area.
	public ArrayList<Point> get_confinement_info() {
		ArrayList<Point> confinement_info = new ArrayList<>();
		var left_up = Point.fromLngLat(-3.192473, 55.946233);
		confinement_info.add(left_up);
		var right_up = Point.fromLngLat(-3.184319, 55.946233);
		confinement_info.add(right_up);
		var right_down = Point.fromLngLat(-3.184319, 55.942617);
		confinement_info.add(right_down);
		var left_down = Point.fromLngLat(-3.192473, 55.942617);
		confinement_info.add(left_down);
		confinement_info.add(left_up);
		return confinement_info;
	}

}

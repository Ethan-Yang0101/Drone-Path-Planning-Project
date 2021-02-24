package uk.ac.ed.inf.aqmaps;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
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

public class DataServer {

	public static String port_number;
	public static String year;
	public static String month;
	public static String day;

	public static String get_json_string(String urlString) throws IOException, InterruptedException {
		var client = HttpClient.newHttpClient();
		var request = HttpRequest.newBuilder().uri(URI.create(urlString)).build();
		var response = client.send(request, BodyHandlers.ofString());
		return response.body();
	}

	public static String get_sensors_json() throws IOException, InterruptedException {
		var urlString = "http://localhost:" + port_number + "/maps/" + year + "/" + month + "/" + day
				+ "/air-quality-data.json";
		return get_json_string(urlString);
	}

	public static String get_word_json(String W3W_Location) throws IOException, InterruptedException {
		var words = W3W_Location.replace('.', ',').split(",");
		var urlString = "http://localhost:" + port_number + "/words/" + words[0] + "/" + words[1] + "/" + words[2]
				+ "/details.json";
		return get_json_string(urlString);
	}

	public static String get_buildings_json() throws IOException, InterruptedException {
		var urlString = "http://localhost:" + port_number + "/buildings/no-fly-zones.geojson";
		return get_json_string(urlString);
	}

	public static ArrayList<SensorDetail> get_sensors_detail() throws IOException, InterruptedException {
		var json_string = get_sensors_json();
		Type listType = new TypeToken<ArrayList<SensorDetail>>() {
		}.getType();
		ArrayList<SensorDetail> sensor_list = new Gson().fromJson(json_string, listType);
		return sensor_list;
	}

	public static LocationDetail get_location_detail(String W3W_Location) throws IOException, InterruptedException {
		var json_string = get_word_json(W3W_Location);
		var location = new Gson().fromJson(json_string, LocationDetail.class);
		return location;
	}

	public static ArrayList<ArrayList<Point>> get_buildings_info() throws IOException, InterruptedException {
		ArrayList<ArrayList<Point>> buildings = new ArrayList<>();
		var json_string = get_buildings_json();
		var collection = FeatureCollection.fromJson(json_string);
		ArrayList<Feature> features = (ArrayList<Feature>) collection.features();
		for (Feature feature : features) {
			Polygon polygon = (Polygon) feature.geometry();
			List<List<Point>> point_list = polygon.coordinates();
			ArrayList<Point> points = (ArrayList<Point>) point_list.get(0);
			buildings.add(points);
		}
		return buildings;
	}

	public static HashMap<String, Point> get_sensors_info() throws IOException, InterruptedException {
		HashMap<String, Point> sensors_info = new HashMap<>();
		ArrayList<SensorDetail> sensors = get_sensors_detail();
		for (SensorDetail sensor : sensors) {
			var location = sensor.location;
			var localDetail = get_location_detail(location);
			var longitude = localDetail.coordinates.lng;
			var latitude = localDetail.coordinates.lat;
			sensors_info.put(location, Point.fromLngLat(longitude, latitude));
		}
		return sensors_info;
	}

	public static ArrayList<Point> get_confinement_info() {
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

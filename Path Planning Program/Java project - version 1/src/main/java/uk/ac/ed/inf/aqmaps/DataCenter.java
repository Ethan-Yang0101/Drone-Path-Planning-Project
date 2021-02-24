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

public class DataCenter {

	public static ArrayList<Feature> database;

	public static String get_json_string(String urlString) throws IOException, InterruptedException {
		var client = HttpClient.newHttpClient();
		var request = HttpRequest.newBuilder().uri(URI.create(urlString)).build();
		var response = client.send(request, BodyHandlers.ofString());
		return response.body();
	}

	public static String get_map_json(String year, String month, String day) throws IOException, InterruptedException {
		var urlString = "http://localhost:80/maps/" + year + "/" + month + "/" + day + "/air-quality-data.json";
		return get_json_string(urlString);
	}

	public static String get_word_json(String W3W_Location) throws IOException, InterruptedException {
		var words = W3W_Location.replace('.', ',').split(",");
		var urlString = "http://localhost:80/words/" + words[0] + "/" + words[1] + "/" + words[2] + "/details.json";
		return get_json_string(urlString);
	}

	public static String get_buildings_json() throws IOException, InterruptedException {
		var urlString = "http://localhost:80/buildings/no-fly-zones.geojson";
		return get_json_string(urlString);
	}

	public static ArrayList<SensorDetail> get_sensor_detail(String year, String month, String day)
			throws IOException, InterruptedException {
		var json_string = get_map_json(year, month, day);
		Type listType = new TypeToken<ArrayList<SensorDetail>>() {
		}.getType();
		ArrayList<SensorDetail> sensor_list = new Gson().fromJson(json_string, listType);
		return sensor_list;
	}

	public static ArrayList<ArrayList<Point>> get_buildings_detail() throws IOException, InterruptedException {
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

	public static LocationDetail get_location_detail(String W3W_Location) throws IOException, InterruptedException {
		var json_string = get_word_json(W3W_Location);
		var location = new Gson().fromJson(json_string, LocationDetail.class);
		return location;
	}

	public static HashMap<String, Point> get_map_info(String year, String month, String day)
			throws IOException, InterruptedException {
		HashMap<String, Point> map_info = new HashMap<>();
		ArrayList<SensorDetail> sensors = get_sensor_detail(year, month, day);
		for (SensorDetail sensor : sensors) {
			String location = sensor.location;
			double longitude = get_location_detail(location).coordinates.lng;
			double latitude = get_location_detail(location).coordinates.lat;
			map_info.put(location, Point.fromLngLat(longitude, latitude));
		}
		return map_info;
	}

	public static ArrayList<Point> get_confine_info() {
		ArrayList<Point> confine_info = new ArrayList<>();
		Point left_up = Point.fromLngLat(-3.192473, 55.946233);
		confine_info.add(left_up);
		Point right_up = Point.fromLngLat(-3.184319, 55.946233);
		confine_info.add(right_up);
		Point right_down = Point.fromLngLat(-3.184319, 55.942617);
		confine_info.add(right_down);
		Point left_down = Point.fromLngLat(-3.192473, 55.942617);
		confine_info.add(left_down);
		confine_info.add(left_up);
		return confine_info;
	}

	public static String generate_rgb_string(String reading) {
		double value = 0.0;
		if (reading.equals("NaN") || reading.equals("null")) {
			return "#000000";
		} else {
			value = Double.parseDouble(reading);
		}
		if (0 <= value && value < 32) {
			return "#00ff00";
		} else if (32 <= value && value < 64) {
			return "#40ff00";
		} else if (64 <= value && value < 96) {
			return "#80ff00";
		} else if (96 <= value && value < 128) {
			return "#c0ff00";
		} else if (128 <= value && value < 160) {
			return "#ffc000";
		} else if (160 <= value && value < 192) {
			return "#ff8000";
		} else if (192 <= value && value < 224) {
			return "#ff4000";
		} else {
			return "#ff0000";
		}
	}

	public static String generate_marker_symbol(String reading) {
		double value = 0.0;
		if (reading.equals("NaN") || reading.equals("null")) {
			return "cross";
		} else {
			value = Double.parseDouble(reading);
		}
		if (0 <= value && value < 128) {
			return "lighthouse";
		} else {
			return "danger";
		}
	}

	public static void set_up_database(String year, String month, String day) throws IOException, InterruptedException {
		ArrayList<SensorDetail> sensors = get_sensor_detail(year, month, day);
		ArrayList<Feature> database = new ArrayList<>();
		for (SensorDetail sensor : sensors) {
			LocationDetail location = get_location_detail(sensor.location);
			Point point = Point.fromLngLat(location.coordinates.lng, location.coordinates.lat);
			Feature feature = Feature.fromGeometry(point);
			feature.addStringProperty("marker-size", "medium");
			feature.addStringProperty("location", sensor.location);
			feature.addStringProperty("rgb-string", "#aaaaaa");
			feature.addStringProperty("marker-color", "#aaaaaa");
			database.add(feature);
		}
		DataCenter.database = database;
	}

	public static void update_database(String location, String year, String month, String day)
			throws IOException, InterruptedException {
		for (Feature feature : DataCenter.database) {
			if (feature.getStringProperty("location").equals(location)) {
				ArrayList<SensorDetail> sensors = get_sensor_detail(year, month, day);
				for (SensorDetail sensor : sensors) {
					if (sensor.location.equals(location)) {
						String reading_rgb = generate_rgb_string(sensor.reading);
						String reading_marker = generate_marker_symbol(sensor.reading);
						feature.addStringProperty("rgb-string", reading_rgb);
						feature.addStringProperty("marker-color", reading_rgb);
						feature.addStringProperty("marker-symbol", reading_marker);
					}
				}
			}
		}
	}

}

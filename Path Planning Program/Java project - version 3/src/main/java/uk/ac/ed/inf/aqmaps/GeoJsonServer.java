package uk.ac.ed.inf.aqmaps;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;

public class GeoJsonServer {

	public static ArrayList<Feature> sensors_data;
	public static String year;
	public static String month;
	public static String day;

	public static String generate_rgb_string(String reading) {
		var value = 0.0;
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
		var value = 0.0;
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

	public static void set_up_sensors_data() throws IOException, InterruptedException {
		ArrayList<SensorDetail> sensors = DataServer.get_sensors_detail();
		ArrayList<Feature> data = new ArrayList<>();
		for (SensorDetail sensor : sensors) {
			var location = DataServer.get_location_detail(sensor.location);
			var point = Point.fromLngLat(location.coordinates.lng, location.coordinates.lat);
			var feature = Feature.fromGeometry(point);
			feature.addStringProperty("marker-size", "medium");
			feature.addStringProperty("location", sensor.location);
			feature.addStringProperty("rgb-string", "#aaaaaa");
			feature.addStringProperty("marker-color", "#aaaaaa");
			data.add(feature);
		}
		GeoJsonServer.sensors_data = data;
	}

	public static void update_sensors_data(String location) throws IOException, InterruptedException {
		for (Feature feature : GeoJsonServer.sensors_data) {
			if (feature.getStringProperty("location").equals(location)) {
				ArrayList<SensorDetail> sensors = DataServer.get_sensors_detail();
				for (SensorDetail sensor : sensors) {
					if (sensor.location.equals(location)) {
						var reading_rgb = generate_rgb_string(sensor.reading);
						var reading_marker = generate_marker_symbol(sensor.reading);
						feature.addStringProperty("rgb-string", reading_rgb);
						feature.addStringProperty("marker-color", reading_rgb);
						feature.addStringProperty("marker-symbol", reading_marker);
					}
				}
			}
		}
	}

	public static FeatureCollection generate_feature_collection(ArrayList<Point> points)
			throws IOException, InterruptedException {
		var features = GeoJsonServer.sensors_data;
		var confine_area = LineString.fromLngLats(DataServer.get_confinement_info());
		var confine_feature = Feature.fromGeometry(confine_area);
		var flight_path = LineString.fromLngLats(points);
		var flight_feature = Feature.fromGeometry(flight_path);
		var buildings = FeatureCollection.fromJson(DataServer.get_buildings_json());
		for (Feature building : buildings.features()) {
			features.add(building);
		}
		features.add(flight_feature);
		features.add(confine_feature);
		return FeatureCollection.fromFeatures(features);
	}

	public static void write_geojson_file(ArrayList<Point> points) throws IOException, InterruptedException {
		var collection = generate_feature_collection(points);
		var json_string = collection.toJson();
		var filename = "readings-" + day + "-" + month + "-" + year + ".geojson";
		FileWriter writer = new FileWriter(filename);
		writer.write(json_string);
		writer.close();
	}

}

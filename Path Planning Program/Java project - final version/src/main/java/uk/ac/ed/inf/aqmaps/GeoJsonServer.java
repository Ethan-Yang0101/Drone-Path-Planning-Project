package uk.ac.ed.inf.aqmaps;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;

/*
This class is used to provide GeoJSON service for Drone to update
information received from sensors and write the GeoJSON file.
 */
public class GeoJsonServer {

	private final DataServer data_server; // A field refer to DataServer object.
	private final ArrayList<Feature> sensors_data; // A field refer to sensors’ data.
	private final String year; // A field refer to year of GeoJsonServer connection.
	private final String month; // A field refer to month of GeoJsonServer connection.
	private final String day; // A field refer to day of GeoJsonServer connection.

	public GeoJsonServer(String year, String month, String day, String port_number)
			throws IOException, InterruptedException {
		this.data_server = new DataServer(year, month, day, port_number);
		this.sensors_data = set_up_sensors_data();
		this.year = year;
		this.month = month;
		this.day = day;
	}

	// This method converts reading to RGB String given reading and battery.
	private String generate_rgb_string(String reading, double battery) {
		if (battery < 10) {
			return "#000000";
		}
		var value = Double.parseDouble(reading);
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

	// This method converts reading to marker name given reading and battery.
	private String generate_marker_symbol(String reading, double battery) {
		if (battery < 10) {
			return "cross";
		}
		var value = Double.parseDouble(reading);
		if (0 <= value && value < 128) {
			return "lighthouse";
		} else {
			return "danger";
		}
	}

	// This method sets up field sensors data which contains 33 sensors’ Features
	// with no marker properties and with gray color RGB String properties.
	private ArrayList<Feature> set_up_sensors_data() throws IOException, InterruptedException {
		ArrayList<SensorDetail> sensors = this.data_server.get_sensors_detail();
		ArrayList<Feature> data = new ArrayList<>();
		for (SensorDetail sensor : sensors) {
			LocationDetail location = this.data_server.get_location_detail(sensor.getLocation());
			var point = Point.fromLngLat(location.getCoordinates().getLng(), location.getCoordinates().getLat());
			var feature = Feature.fromGeometry(point);
			feature.addStringProperty("marker-size", "medium");
			feature.addStringProperty("location", sensor.getLocation());
			feature.addStringProperty("rgb-string", "#aaaaaa");
			feature.addStringProperty("marker-color", "#aaaaaa");
			data.add(feature);
		}
		return data;
	}

	// This method updates the field sensors data (add properties of Feature related
	// to what3words location parameter based on sensor reading received by Drone).
	public void update_sensor_data(String location) throws IOException, InterruptedException {
		for (Feature feature : this.sensors_data) {
			if (feature.getStringProperty("location").equals(location)) {
				ArrayList<SensorDetail> sensors = this.data_server.get_sensors_detail();
				for (SensorDetail sensor : sensors) {
					if (sensor.getLocation().equals(location)) {
						String reading_rgb = generate_rgb_string(sensor.getReading(), sensor.getBattery());
						String reading_marker = generate_marker_symbol(sensor.getReading(), sensor.getBattery());
						feature.addStringProperty("rgb-string", reading_rgb);
						feature.addStringProperty("marker-color", reading_rgb);
						feature.addStringProperty("marker-symbol", reading_marker);
					}
				}
			}
		}
	}

	// This method writes GeoJSON file given output of drone control algorithm and
	// sensors data which has been updated by drone control algorithm.
	public void write_geojson_file(ArrayList<Point> points) throws IOException, InterruptedException {
		ArrayList<Feature> features = this.sensors_data;
		var flight_path = LineString.fromLngLats(points);
		var flight_feature = Feature.fromGeometry(flight_path);
		features.add(flight_feature);
		var collection = FeatureCollection.fromFeatures(features);
		String json_string = collection.toJson();
		var filename = "readings-" + this.day + "-" + this.month + "-" + this.year + ".geojson";
		FileWriter writer = new FileWriter(filename);
		writer.write(json_string);
		writer.close();
	}

}

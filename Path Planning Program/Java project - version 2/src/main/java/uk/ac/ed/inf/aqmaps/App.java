package uk.ac.ed.inf.aqmaps;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;

public class App {

	public static FeatureCollection generate_feature_collection(ArrayList<Feature> features, ArrayList<Point> points)
			throws IOException, InterruptedException {
		LineString confine_area = LineString.fromLngLats(DataCenter.get_confinement_info());
		Feature confine_feature = Feature.fromGeometry(confine_area);
		LineString flight_path = LineString.fromLngLats(points);
		Feature flight_feature = Feature.fromGeometry(flight_path);
		FeatureCollection buildings = FeatureCollection.fromJson(DataCenter.get_buildings_json());
		for (Feature building : buildings.features()) {
			features.add(building);
		}
		features.add(flight_feature);
		features.add(confine_feature);
		return FeatureCollection.fromFeatures(features);
	}

	public static void write_geojson_file(FeatureCollection collection, String year, String month, String day) {
		String json_string = collection.toJson();
		String filename = "readings-" + day + "-" + month + "-" + year + ".geojson";
		try (FileWriter file = new FileWriter(filename)) {
			file.write(json_string);
			file.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		String day = args[0];
		String month = args[1];
		String year = args[2];
		double latitude = Double.parseDouble(args[3]);
		double longitude = Double.parseDouble(args[4]);
		String seed = args[5];
		String port_number = args[6];
		Drone drone = new Drone(year, month, day, port_number);
		Point start_position = Point.fromLngLat(longitude, latitude);
		ArrayList<Point> points = drone.drone_control_algorithm(start_position);
		if (points.size() == 0) {
			System.out.println("Cannot find flight path");
			System.exit(0);
		}
		FeatureCollection collection = generate_feature_collection(DataCenter.sensors_data, points);
		write_geojson_file(collection, year, month, day);
	}
}

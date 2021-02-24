package uk.ac.ed.inf.aqmaps;

import java.io.IOException;
import java.util.ArrayList;
import com.mapbox.geojson.Point;

/*
This class is used to provide the main method which integrates methods
from Drone to write both GeoJSON file and flight path file based on command line inputs.
 */
public class App {

	// This methods writes GeoJSON file and txt file by choosing one output of drone
	// control algorithm that has minimum number of steps among three different
	// mode. All parameters come from command line.
	public static void main(String[] args) throws IOException, InterruptedException {
		String day = args[0];
		String month = args[1];
		String year = args[2];
		String port_number = args[6];
		var latitude = Double.parseDouble(args[3]);
		var longitude = Double.parseDouble(args[4]);
		var drone_position = Point.fromLngLat(longitude, latitude);
		var drone = new Drone(year, month, day, port_number);
		ArrayList<Point> path1 = drone.drone_control_algorithm(drone_position, "clockwise", 1);
		ArrayList<Point> path2 = drone.drone_control_algorithm(drone_position, "anticlockwise", 1);
		ArrayList<Point> path3 = drone.drone_control_algorithm(drone_position, "double", 10);
		if (path1.size() < path2.size() && path1.size() < path3.size()) {
			path1 = drone.drone_control_algorithm(drone_position, "clockwise", 1);
			drone.write_geojson_file(path1);
			drone.write_flight_path_file();
		} else if (path2.size() < path1.size() && path2.size() < path3.size()) {
			path2 = drone.drone_control_algorithm(drone_position, "anticlockwise", 1);
			drone.write_geojson_file(path2);
			drone.write_flight_path_file();
		} else {
			path3 = drone.drone_control_algorithm(drone_position, "double", 10);
			drone.write_geojson_file(path3);
			drone.write_flight_path_file();
		}
	}

}

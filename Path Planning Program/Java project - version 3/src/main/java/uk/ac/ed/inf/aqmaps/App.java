package uk.ac.ed.inf.aqmaps;

import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;
import com.mapbox.geojson.Point;

public class App {

	public static void main(String[] args) throws IOException, InterruptedException {
		try {
			var day = args[0];
			var month = args[1];
			var year = args[2];
			var latitude = Double.parseDouble(args[3]);
			var longitude = Double.parseDouble(args[4]);
			var port_number = args[6];
			var drone_position = Point.fromLngLat(longitude, latitude);
			var drone = new Drone(year, month, day, port_number);
			ArrayList<Point> path = drone.drone_control_algorithm(drone_position);
			if (path.size() == 0) {
				System.out.println("Cannot find flight path");
				System.exit(0);
			} else {
				GeoJsonServer.write_geojson_file(path);
				drone.write_flight_path();
				System.out.println("Flight path move steps: " + (path.size() - 1));
			}
		} catch (ConnectException e1) {
			System.out.println("Please Connect WebServer And Try Again!");
		} catch (IllegalArgumentException e2) {
			System.out.println("Error Occur In Command Line, Please Try Again!");
		} catch (IndexOutOfBoundsException e3) {
			System.out.println("Error Occur In Command Line, Please Try Again!");
		}
	}

}

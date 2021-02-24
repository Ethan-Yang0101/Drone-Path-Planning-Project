package uk.ac.ed.inf.aqmaps;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import com.mapbox.geojson.Point;
import java.awt.geom.Line2D;

/*
This class is used to provide Drone object which contains drone control
algorithm and method to write flight path file and GeoJSON file.
 */
public class Drone {

	private ArrayList<FlightDetail> flight_log; // list of FlightDetail objects which record flight info of Drone.
	private ArrayList<String> explored; // list of what3words explored.
	private GeoJsonServer geojson_server; // GeoJsonServer owned by Drone.
	private final DataServer data_server; // DataServer owned by Drone.
	private final HashMap<String, Point> sensors_info; // 33 what3words locations and corresponding points.
	private final ArrayList<Point> confinement_info; // corners of confinement area.
	private final ArrayList<ArrayList<Point>> buildings_info; // points of no-fly zones.
	private final String port_number; // port number of web server connection.
	private final String year; // year set by Drone.
	private final String month; // month set by Drone.
	private final String day; // day set by Drone.

	public Drone(String year, String month, String day, String port_number) throws IOException, InterruptedException {
		this.data_server = new DataServer(year, month, day, port_number);
		this.sensors_info = data_server.get_sensors_info();
		this.confinement_info = data_server.get_confinement_info();
		this.buildings_info = data_server.get_buildings_info();
		this.port_number = port_number;
		this.year = year;
		this.month = month;
		this.day = day;
	}

	// This method returns euclidean distance between two points.
	private double euclidean_distance(Point point1, Point point2) {
		var square1 = Math.pow(point1.longitude() - point2.longitude(), 2);
		var square2 = Math.pow(point1.latitude() - point2.latitude(), 2);
		return Math.sqrt(square1 + square2);
	}

	// This method returns next point given current point and move degree.
	private Point next_step_position(Point current_step, int degree) {
		double rad = Math.toRadians(degree);
		double longitude = current_step.longitude() + 0.0003 * Math.cos(rad);
		double latitude = current_step.latitude() + 0.0003 * Math.sin(rad);
		return Point.fromLngLat(longitude, latitude);
	}

	// This method is used to determine whether line between current point and next
	// point will intersect with boundaries of no fly zones or confinement area.
	private boolean cross_no_fly_zone(Point current_step, int degree) {
		ArrayList<ArrayList<Point>> restriction = this.buildings_info;
		ArrayList<Point> confine = this.confinement_info;
		restriction.add(confine);
		Point next_step = next_step_position(current_step, degree);
		for (ArrayList<Point> restrict : restriction) {
			for (int i = 0; i < restrict.size() - 1; i++) {
				double lon1 = restrict.get(i).longitude();
				double lat1 = restrict.get(i).latitude();
				double lon2 = restrict.get(i + 1).longitude();
				double lat2 = restrict.get(i + 1).latitude();
				double lon3 = current_step.longitude();
				double lat3 = current_step.latitude();
				double lon4 = next_step.longitude();
				double lat4 = next_step.latitude();
				if (Line2D.linesIntersect(lon1, lat1, lon2, lat2, lon3, lat3, lon4, lat4)) {
					return true;
				}
			}
		}
		return false;
	}

	// This method returns direction of move in degree which avoids the boundaries
	// of no fly zones or confinement area. Choosing strategies by mode.
	private int boundary_avoid_degree(Point current_step, int degree, String mode) {
		int clockwise_degree = degree;
		int anticlockwise_degree = degree;
		while (cross_no_fly_zone(current_step, anticlockwise_degree)) {
			anticlockwise_degree = anticlockwise_degree + 10;
		}
		while (cross_no_fly_zone(current_step, clockwise_degree)) {
			clockwise_degree = clockwise_degree - 10;
		}
		if (clockwise_degree == anticlockwise_degree) {
			return degree;
		}
		if (mode.equals("clockwise")) {
			return clockwise_degree;
		}
		if (mode.equals("anticlockwise")) {
			return anticlockwise_degree;
		}
		double clockwise = Math.abs(clockwise_degree - degree);
		double anticlockwise = Math.abs(anticlockwise_degree - degree);
		if (mode.equals("double") && (clockwise < anticlockwise)) {
			return clockwise_degree;
		} else {
			return anticlockwise_degree;
		}
	}

	// This method returns next point which avoids boundaries of no fly zones and
	// confinement area and also make sure that next point is not in moved points.
	private Point move_one_step(Point current_step, int degree, String mode, ArrayList<Point> moved_points) {
		int avoid_degree = boundary_avoid_degree(current_step, degree, mode);
		Point next_step = next_step_position(current_step, avoid_degree);
		if (moved_points.contains(next_step)) {
			int alternative_degree = boundary_avoid_degree(current_step, avoid_degree - 180, mode);
			return next_step_position(current_step, alternative_degree);
		}
		return next_step;
	}

	// This method finds sensors in area of Drone detection at current point and
	// returns the what3words location of the closest sensor.
	private String receive_sensor_data(Point current_step) throws IOException, InterruptedException {
		ArrayList<String> available = new ArrayList<>();
		ArrayList<Double> distances = new ArrayList<>();
		for (String location : this.sensors_info.keySet()) {
			Point point = this.sensors_info.get(location);
			double distance = euclidean_distance(point, current_step);
			if (distance < 0.0002 && !this.explored.contains(location)) {
				available.add(location);
				distances.add(distance);
			}
		}
		if (available.size() != 0) {
			int min_distance_index = distances.indexOf(Collections.min(distances));
			String min_available = available.get(min_distance_index);
			this.geojson_server.update_sensor_data(min_available);
			this.explored.add(min_available);
			return min_available;
		}
		return "null";
	}

	// This method returns adjusted flight direction in degree which is x10.
	private int adjust_flight_direction(Point current_step, Point destination) {
		double latitude = destination.latitude() - current_step.latitude();
		double longitude = destination.longitude() - current_step.longitude();
		double flight_angle = Math.atan2(-latitude, -longitude) / Math.PI * 180 + 180;
		return (int) Math.round(flight_angle / 10) * 10;
	}

	// This method finds the sensor that is nearest to sensor in search domain.
	private Point find_nearest_sensor(Point sensor, ArrayList<Point> search_domain) {
		double min_distance = 999999;
		Point min_sensor = sensor;
		for (Point unexplored_sensor : search_domain) {
			double distance = euclidean_distance(unexplored_sensor, sensor);
			if (distance < min_distance) {
				min_distance = distance;
				min_sensor = unexplored_sensor;
			}
		}
		return min_sensor;
	}

	// This method makes a flight plan of sensors based on greedy method.
	private ArrayList<Point> greedy_flight_planning(Point first_sensor) {
		Collection<Point> points = this.sensors_info.values();
		ArrayList<Point> search_domain = new ArrayList<>(points);
		ArrayList<Point> flight_plan = new ArrayList<Point>();
		Point current_sensor = first_sensor;
		while (!search_domain.isEmpty()) {
			Point next_sensor = find_nearest_sensor(current_sensor, search_domain);
			search_domain.remove(next_sensor);
			flight_plan.add(next_sensor);
			current_sensor = next_sensor;
		}
		return flight_plan;
	}

	// This method makes a flight plan based on greedy with swap heuristic.
	private ArrayList<Point> swap_greedy_flight_planning(Point start_position, Point first_sensor) {
		ArrayList<Point> plan = greedy_flight_planning(first_sensor);
		plan.add(0, start_position);
		plan.add(plan.size(), start_position);
		while (true) {
			boolean swap_occur = false;
			for (int i = 1; i < plan.size() - 2; i++) {
				for (int j = i + 1; j < plan.size() - 1; j++) {
					double original_distance = euclidean_distance(plan.get(i), plan.get(i - 1))
							+ euclidean_distance(plan.get(j), plan.get(j + 1));
					double swap_distance = euclidean_distance(plan.get(j), plan.get(i - 1))
							+ euclidean_distance(plan.get(i), plan.get(j + 1));
					if (swap_distance < original_distance) {
						Collections.swap(plan, i, j);
						swap_occur = true;
					}
				}
			}
			if (!swap_occur) {
				break;
			}
		}
		return plan;
	}

	// This method returns an ArrayList of sensors from closest to furtherest.
	private ArrayList<Point> first_sensor_planning(Point start_position) {
		Collection<Point> points = this.sensors_info.values();
		ArrayList<Point> search_domain = new ArrayList<>(points);
		ArrayList<Point> result = new ArrayList<>();
		while (!search_domain.isEmpty()) {
			Point nearest_sensor = find_nearest_sensor(start_position, search_domain);
			result.add(nearest_sensor);
			search_domain.remove(nearest_sensor);
		}
		return result;
	}

	// This method controls the Drone move from current position to target sensor.
	private ArrayList<Point> move_to_next_destination(Point current_step, Point destination, int count_steps,
			String mode) throws IOException, InterruptedException {
		int step_number = 0;
		ArrayList<Point> moved_points = new ArrayList<>();
		while (step_number < 1 || (euclidean_distance(current_step, destination) >= 0.0002 && count_steps <= 150)) {
			int adjust_degree = adjust_flight_direction(current_step, destination);
			Point next_step = move_one_step(current_step, adjust_degree, mode, moved_points);
			int move_degree = adjust_flight_direction(current_step, next_step);
			String location = receive_sensor_data(next_step);
			moved_points.add(next_step);
			count_steps++;
			this.flight_log.add(new FlightDetail(count_steps, current_step.longitude(), current_step.latitude(),
					move_degree, next_step.longitude(), next_step.latitude(), location));
			current_step = next_step;
			step_number++;
		}
		return moved_points;
	}

	// This method controls Drone to collect data from sensors following plan.
	public ArrayList<Point> drone_control_algorithm(Point start_position, String mode, int tolerant)
			throws IOException, InterruptedException {
		ArrayList<Point> first_sensor_plan = first_sensor_planning(start_position);
		int count_planning = 0;
		for (Point first_sensor : first_sensor_plan) {
			count_planning++;
			int count_steps = 0;
			this.explored = new ArrayList<String>();
			this.flight_log = new ArrayList<FlightDetail>();
			this.geojson_server = new GeoJsonServer(year, month, day, port_number);
			ArrayList<Point> total_path = new ArrayList<>();
			boolean track_error = false;
			Point current_step = start_position;
			total_path.add(current_step);
			ArrayList<Point> flight_plan = swap_greedy_flight_planning(start_position, first_sensor);
			for (int i = 1; i < flight_plan.size(); i++) {
				Point next_destination = flight_plan.get(i);
				ArrayList<Point> points = move_to_next_destination(current_step, next_destination, count_steps, mode);
				total_path.addAll(points);
				if (total_path.size() == 152) {
					track_error = true;
					break;
				}
				current_step = total_path.get(total_path.size() - 1);
				count_steps = total_path.size() - 1;
			}
			if (!track_error) {
				return total_path;
			} else if (count_planning == tolerant) {
				total_path.remove(151);
				this.flight_log.remove(150);
				return total_path;
			} else {
				continue;
			}
		}
		return new ArrayList<Point>();
	}

	// This method writes GeoJSON file by GeoJsonServer’s method given flight path.
	public void write_geojson_file(ArrayList<Point> path) throws IOException, InterruptedException {
		this.geojson_server.write_geojson_file(path);
		System.out.println("Flight path move steps: " + (path.size() - 1));
	}

	// This method writes flight path file by using field flight log.
	public void write_flight_path_file() throws IOException {
		var filename = "flightpath-" + this.day + "-" + this.month + "-" + this.year + ".txt";
		var writer = new FileWriter(filename);
		for (int i = 0; i < this.flight_log.size(); i++) {
			writer.write(this.flight_log.get(i).toString() + "\n");
		}
		writer.close();
	}

}

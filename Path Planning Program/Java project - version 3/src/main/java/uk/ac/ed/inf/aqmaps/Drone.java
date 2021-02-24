package uk.ac.ed.inf.aqmaps;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import com.mapbox.geojson.Point;
import java.awt.geom.Line2D;

public class Drone {

	private HashMap<String, Point> sensors_info;
	private ArrayList<ArrayList<Point>> buildings_info;
	private ArrayList<Point> confinement_info;
	private ArrayList<FlightDetail> flight_log;
	private ArrayList<String> explored;
	private final String year;
	private final String month;
	private final String day;

	public Drone(String year, String month, String day, String port_number) throws IOException, InterruptedException {
		this.connect_data_server(year, month, day, port_number);
		this.request_data();
		this.connect_geojson_server(year, month, day);
		this.year = year;
		this.month = month;
		this.day = day;
	}

	private void connect_data_server(String year, String month, String day, String port_number)
			throws IOException, InterruptedException {
		DataServer.year = year;
		DataServer.month = month;
		DataServer.day = day;
		DataServer.port_number = port_number;
	}

	private void request_data() throws IOException, InterruptedException {
		this.sensors_info = DataServer.get_sensors_info();
		this.confinement_info = DataServer.get_confinement_info();
		this.buildings_info = DataServer.get_buildings_info();
	}

	private void connect_geojson_server(String year, String month, String day)
			throws IOException, InterruptedException {
		GeoJsonServer.year = year;
		GeoJsonServer.month = month;
		GeoJsonServer.day = day;
		GeoJsonServer.set_up_sensors_data();
	}

	private Point next_step_position(Point current_step, int degree) {
		var rad = Math.toRadians(degree);
		var longitude = current_step.longitude() + 0.0003 * Math.cos(rad);
		var latitude = current_step.latitude() + 0.0003 * Math.sin(rad);
		var next_step = Point.fromLngLat(longitude, latitude);
		return next_step;
	}

	private boolean intersect(Point point1, Point point2, Point point3, Point point4) {
		var lon1 = point1.longitude();
		var lat1 = point1.latitude();
		var lon2 = point2.longitude();
		var lat2 = point2.latitude();
		var lon3 = point3.longitude();
		var lat3 = point3.latitude();
		var lon4 = point4.longitude();
		var lat4 = point4.latitude();
		return Line2D.linesIntersect(lon1, lat1, lon2, lat2, lon3, lat3, lon4, lat4);
	}

	private boolean cross_no_fly_zone(Point start, int degree) {
		ArrayList<ArrayList<Point>> restriction = this.buildings_info;
		ArrayList<Point> confine = this.confinement_info;
		restriction.add(confine);
		var end = next_step_position(start, degree);
		for (ArrayList<Point> restrict : restriction) {
			for (int i = 0; i < restrict.size() - 1; i++) {
				if (intersect(restrict.get(i), restrict.get(i + 1), start, end)) {
					return true;
				}
			}
		}
		return false;
	}

	private int boundary_avoid_degree(Point current_step, int current_degree, boolean safe_mode) {
		var clockwise_degree = current_degree;
		var anticlockwise_degree = current_degree;
		while (cross_no_fly_zone(current_step, anticlockwise_degree)) {
			anticlockwise_degree = anticlockwise_degree + 10;
		}
		while (cross_no_fly_zone(current_step, clockwise_degree)) {
			clockwise_degree = clockwise_degree - 10;
		}
		if (safe_mode) {
			return anticlockwise_degree;
		}
		if (clockwise_degree == anticlockwise_degree) {
			return current_degree;
		}
		if (Math.abs(clockwise_degree - current_degree) < Math.abs(anticlockwise_degree - current_degree)) {
			return clockwise_degree;
		}
		return anticlockwise_degree;
	}

	private Point one_step_move(Point current_step, int degree) {
		var next_degree = boundary_avoid_degree(current_step, degree, true);
		var next_step = next_step_position(current_step, next_degree);
		return next_step;
	}

	private double euclidean_distance(Point point1, Point point2) {
		var square1 = Math.pow(point1.longitude() - point2.longitude(), 2);
		var square2 = Math.pow(point1.latitude() - point2.latitude(), 2);
		return Math.sqrt(square1 + square2);
	}

	private String nearest_available_sensor(ArrayList<String> available, ArrayList<Double> distances)
			throws IOException, InterruptedException {
		double min_distance = 999;
		String min_available = "";
		for (int i = 0; i < distances.size(); i++) {
			if (distances.get(i) < min_distance) {
				min_distance = distances.get(i);
				min_available = available.get(i);
			}
		}
		if (available.size() != 0) {
			GeoJsonServer.update_sensors_data(min_available);
			this.explored.add(min_available);
			return min_available;
		}
		return "null";
	}

	private String receive_sensor_data(Point current_step) throws IOException, InterruptedException {
		ArrayList<String> available = new ArrayList<>();
		ArrayList<Double> distances = new ArrayList<>();
		for (String location : this.sensors_info.keySet()) {
			var distance = euclidean_distance(this.sensors_info.get(location), current_step);
			if (distance < 0.0002 && !this.explored.contains(location)) {
				available.add(location);
				distances.add(distance);
			}
		}
		return nearest_available_sensor(available, distances);
	}

	private Point find_nearest_sensor(Point sensor, ArrayList<Point> search_domain) {
		double min_distance = 999;
		Point next_sensor = sensor;
		for (Point next : search_domain) {
			var distance = euclidean_distance(next, sensor);
			if (distance < min_distance) {
				min_distance = distance;
				next_sensor = next;
			}
		}
		return next_sensor;
	}

	private ArrayList<Point> greedy_flight_planning(Point drone_location, Point start_position) {
		Collection<Point> points = this.sensors_info.values();
		ArrayList<Point> search_domain = new ArrayList<>(points);
		ArrayList<Point> flight_plan = new ArrayList<Point>();
		flight_plan.add(drone_location);
		var current_sensor = start_position;
		while (!search_domain.isEmpty()) {
			var next_sensor = find_nearest_sensor(current_sensor, search_domain);
			search_domain.remove(next_sensor);
			flight_plan.add(next_sensor);
			current_sensor = next_sensor;
		}
		flight_plan.add(drone_location);
		return flight_plan;
	}

	private int adjust_flight_direction(Point current_step, Point destination) {
		var latitude = destination.latitude() - current_step.latitude();
		var longitude = destination.longitude() - current_step.longitude();
		var flight_angle = Math.atan2(-latitude, -longitude) / Math.PI * 180 + 180;
		return (int) Math.round(flight_angle / 10) * 10;
	}

	private ArrayList<Point> move_to_next_destination(Point current_step, Point destination, int count_steps)
			throws IOException, InterruptedException {
		ArrayList<Point> path_points = new ArrayList<>();
		var adjust_degree = adjust_flight_direction(current_step, destination);
		var next_step = one_step_move(current_step, adjust_degree);
		var move_degree = adjust_flight_direction(current_step, next_step);
		var sensor = receive_sensor_data(next_step);
		path_points.add(next_step);
		count_steps++;
		this.flight_log.add(new FlightDetail(count_steps, current_step.longitude(), current_step.latitude(),
				move_degree, next_step.longitude(), next_step.latitude(), sensor));
		current_step = next_step;
		var track_error = false;
		while (euclidean_distance(current_step, destination) >= 0.0002) {
			adjust_degree = adjust_flight_direction(current_step, destination);
			next_step = one_step_move(current_step, adjust_degree);
			move_degree = adjust_flight_direction(current_step, next_step);
			sensor = receive_sensor_data(next_step);
			path_points.add(next_step);
			count_steps++;
			this.flight_log.add(new FlightDetail(count_steps, current_step.longitude(), current_step.latitude(),
					move_degree, next_step.longitude(), next_step.latitude(), sensor));
			if (count_steps >= 150) {
				track_error = true;
				break;
			}
			current_step = next_step;
		}
		if (track_error) {
			return new ArrayList<Point>();
		}
		return path_points;
	}

	public ArrayList<Point> drone_control_algorithm(Point drone_location) throws IOException, InterruptedException {
		Collection<Point> points = this.sensors_info.values();
		ArrayList<Point> start_options = new ArrayList<>(points);
		var default_position = find_nearest_sensor(drone_location, start_options);
		start_options.add(0, default_position);
		for (Point start_position : start_options) {
			var count_steps = 0;
			this.explored = new ArrayList<String>();
			this.flight_log = new ArrayList<FlightDetail>();
			var track_error = false;
			while (true) {
				ArrayList<Point> total_path = new ArrayList<>();
				ArrayList<Point> flight_plan = greedy_flight_planning(drone_location, start_position);
				var current_step = drone_location;
				total_path.add(current_step);
				for (int i = 1; i < flight_plan.size(); i++) {
					var next_destination = flight_plan.get(i);
					ArrayList<Point> path_points = move_to_next_destination(current_step, next_destination,
							count_steps);
					if (path_points.size() == 0) {
						track_error = true;
						break;
					}
					total_path.addAll(path_points);
					current_step = total_path.get(total_path.size() - 1);
					count_steps = total_path.size() - 1;
				}
				if (track_error) {
					break;
				}
				return total_path;
			}
		}
		return new ArrayList<Point>();
	}

	public void write_flight_path() throws IOException {
		var filename = "flightpath-" + day + "-" + month + "-" + year + ".txt";
		var writer = new FileWriter(filename);
		for (FlightDetail log : this.flight_log) {
			writer.write(log.toString());
		}
		writer.close();
	}

}

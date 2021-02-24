package uk.ac.ed.inf.aqmaps;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import com.mapbox.geojson.Point;
import java.awt.geom.Line2D;

public class Drone {

	private HashMap<String, Point> map_info;
	private ArrayList<ArrayList<Point>> buildings_info;
	private ArrayList<Point> confine_info;
	private ArrayList<String> explored;
	private String year;
	private String month;
	private String day;

	public Drone(String year, String month, String day) throws IOException, InterruptedException {
		this.map_info = DataCenter.get_map_info(year, month, day);
		this.buildings_info = DataCenter.get_buildings_detail();
		this.explored = new ArrayList<>();
		this.confine_info = DataCenter.get_confine_info();
		this.year = year;
		this.month = month;
		this.day = day;
	}

	public Point next_step_position(Point current_step, int degree) {
		double rad = Math.toRadians(degree);
		double longitude = current_step.longitude() + 0.0003 * Math.cos(rad);
		double latitude = current_step.latitude() + 0.0003 * Math.sin(rad);
		Point next_step = Point.fromLngLat(longitude, latitude);
		return next_step;
	}

	public boolean intersect(Point point1, Point point2, Point point3, Point point4) {
		double lon1 = point1.longitude();
		double lat1 = point1.latitude();
		double lon2 = point2.longitude();
		double lat2 = point2.latitude();
		double lon3 = point3.longitude();
		double lat3 = point3.latitude();
		double lon4 = point4.longitude();
		double lat4 = point4.latitude();
		return Line2D.linesIntersect(lon1, lat1, lon2, lat2, lon3, lat3, lon4, lat4);
	}

	public boolean cross_no_fly_zone(Point start, int degree) {
		ArrayList<ArrayList<Point>> restriction = this.buildings_info;
		ArrayList<Point> confine = this.confine_info;
		restriction.add(confine);
		Point end = next_step_position(start, degree);
		for (ArrayList<Point> restrict : restriction) {
			for (int i = 0; i < restrict.size() - 1; i++) {
				if (intersect(restrict.get(i), restrict.get(i + 1), start, end)) {
					return true;
				}
			}
		}
		return false;
	}

	public int boundary_avoid_degree(Point current_step, int current_degree) {
		int clockwise_degree = current_degree;
		int anticlockwise_degree = current_degree;
		while (cross_no_fly_zone(current_step, anticlockwise_degree)) {
			anticlockwise_degree = anticlockwise_degree + 10;
		}
		while (cross_no_fly_zone(current_step, clockwise_degree)) {
			clockwise_degree = clockwise_degree - 10;
		}
		if (clockwise_degree == anticlockwise_degree) {
			return current_degree;
		}
		if (Math.abs(clockwise_degree - current_degree) < Math.abs(anticlockwise_degree - current_degree)) {
			return clockwise_degree;
		}
		return anticlockwise_degree;
	}

	public Point one_step_move(Point current_step, int degree) {
		int next_degree = boundary_avoid_degree(current_step, degree);
		Point next_step = next_step_position(current_step, next_degree);
		return next_step;
	}

	public double euclidean_distance(Point point1, Point point2) {
		double square1 = Math.pow(point1.longitude() - point2.longitude(), 2);
		double square2 = Math.pow(point1.latitude() - point2.latitude(), 2);
		return Math.sqrt(square1 + square2);
	}

	public String nearest_available_sensor(ArrayList<String> available, ArrayList<Double> distances)
			throws IOException, InterruptedException {
		double min_distance = 999;
		String min_available = null;
		for (int i = 0; i < distances.size(); i++) {
			if (distances.get(i) < min_distance) {
				min_distance = distances.get(i);
				min_available = available.get(i);
			}
		}
		if (available.size() != 0) {
			DataCenter.update_database(min_available, year, month, day);
			explored.add(min_available);
			return min_available;
		}
		return "null";
	}

	public String receive_sensor_data(Point current_step) throws IOException, InterruptedException {
		ArrayList<String> available = new ArrayList<>();
		ArrayList<Double> distances = new ArrayList<>();
		for (String location : map_info.keySet()) {
			double distance = euclidean_distance(map_info.get(location), current_step);
			if (distance < 0.0002 && !explored.contains(location)) {
				available.add(location);
				distances.add(distance);
			}
		}
		return nearest_available_sensor(available, distances);
	}

	public Point find_nearest_sensor(Point sensor, ArrayList<Point> search_domain) {
		double min_distance = 999;
		Point next_sensor = null;
		for (Point next : search_domain) {
			double distance = euclidean_distance(next, sensor);
			if (distance < min_distance) {
				min_distance = distance;
				next_sensor = next;
			}
		}
		return next_sensor;
	}

	public ArrayList<Point> greedy_flight_planning(Point drone_location, Point start_position) {
		Collection<Point> points = map_info.values();
		ArrayList<Point> search_domain = new ArrayList<>(points);
		ArrayList<Point> flight_plan = new ArrayList<Point>();
		flight_plan.add(drone_location);
		Point current_sensor = start_position;
		while (!search_domain.isEmpty()) {
			Point next_sensor = find_nearest_sensor(current_sensor, search_domain);
			search_domain.remove(next_sensor);
			flight_plan.add(next_sensor);
			current_sensor = next_sensor;
		}
		flight_plan.add(drone_location);
		return flight_plan;
	}

	public int adjust_flight_direction(Point current_step, Point destination) {
		double latitude = destination.latitude() - current_step.latitude();
		double longitude = destination.longitude() - current_step.longitude();
		double flight_angle = Math.atan2(-latitude, -longitude) / Math.PI * 180 + 180;
		return (int) Math.round(flight_angle / 10) * 10;
	}

	public double flight_direction(Point current_step, Point destination) {
		double latitude = destination.latitude() - current_step.latitude();
		double longitude = destination.longitude() - current_step.longitude();
		double flight_angle = Math.atan2(-latitude, -longitude) / Math.PI * 180 + 180;
		return flight_angle;
	}

	public ArrayList<Point> move_to_next_destination(Point current_step, Point destination, int count_steps,
			FileWriter file) throws IOException, InterruptedException {
		ArrayList<Point> path_points = new ArrayList<>();
		int adjust_degree = adjust_flight_direction(current_step, destination);
		Point next_step = one_step_move(current_step, adjust_degree);
		double move_degree = flight_direction(current_step, next_step);
		String sensor = receive_sensor_data(next_step);
		path_points.add(next_step);
		current_step = next_step;
		count_steps++;
		String path_info = count_steps + "," + current_step.longitude() + "," + current_step.latitude() + ","
				+ move_degree + "," + next_step.longitude() + "," + next_step.latitude() + "," + sensor + "\n";
		file.write(path_info);
		boolean track_error = false;
		while (euclidean_distance(current_step, destination) >= 0.0002) {
			adjust_degree = adjust_flight_direction(current_step, destination);
			next_step = one_step_move(current_step, adjust_degree);
			move_degree = adjust_flight_direction(current_step, next_step);
			sensor = receive_sensor_data(next_step);
			path_points.add(next_step);
			count_steps++;
			if (count_steps >= 150) {
				track_error = true;
				break;
			}
			path_info = count_steps + "," + current_step.longitude() + "," + current_step.latitude() + "," + move_degree
					+ "," + next_step.longitude() + "," + next_step.latitude() + "," + sensor + "\n";
			file.write(path_info);
			current_step = next_step;
		}
		if (track_error) {
			return new ArrayList<Point>();
		}
		return path_points;
	}

	public ArrayList<Point> drone_control_algorithm(Point drone_location) throws IOException, InterruptedException {
		String filename = "flightpath-" + day + "-" + month + "-" + year + ".txt";
		int count_steps = 0;
		Collection<Point> points = map_info.values();
		ArrayList<Point> search_domain = new ArrayList<>(points);
		Point default_position = find_nearest_sensor(drone_location, search_domain);
		search_domain.add(0, default_position);
		for (Point start_position : search_domain) {
			boolean track_error = false;
			while (true) {
				FileWriter file = new FileWriter(filename);
				ArrayList<Point> total_path = new ArrayList<>();
				ArrayList<Point> flight_plan = greedy_flight_planning(drone_location, start_position);
				Point current_step = drone_location;
				total_path.add(current_step);
				for (int i = 1; i < flight_plan.size(); i++) {
					Point next_destination = flight_plan.get(i);
					ArrayList<Point> path_points = move_to_next_destination(current_step, next_destination, count_steps,
							file);
					if (path_points.size() == 0) {
						track_error = true;
						break;
					}
					total_path.addAll(path_points);
					current_step = total_path.get(total_path.size() - 1);
					count_steps = total_path.size() - 1;
				}
				if (track_error) {
					file.close();
					break;
				}
				file.close();
				return total_path;
			}
		}
		return new ArrayList<Point>();
	}

}

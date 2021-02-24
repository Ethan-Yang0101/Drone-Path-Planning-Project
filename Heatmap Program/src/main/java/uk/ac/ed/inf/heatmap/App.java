package uk.ac.ed.inf.heatmap;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;

public class App {

	// This method will be used to read 10x10 text matrix into a String matrix
	public static String[][] read_txt_file(String filename) throws FileNotFoundException {
		var matrix = new String[10][10];
		File file_obj = new File(filename);
		Scanner scanner = new Scanner(file_obj);
		int row_number = 0;
		while (scanner.hasNextLine()) {
			String row_data = scanner.nextLine();
			if (!row_data.equals("")) {
				String[] str_list = row_data.replace(" ", "").split(",");
				for (int i = 0; i < str_list.length; i++) {
					matrix[row_number][i] = str_list[i];
				}
			}
			row_number++;
		}
		scanner.close();
		return matrix;
	}

	// This method will be used to generate a double matrix from a String matrix
	public static double[][] generate_double_matrix(String[][] matrix) {
		var double_matrix = new double[matrix.length][matrix[0].length];
		for (int i = 0; i < double_matrix.length; i++) {
			for (int j = 0; j < double_matrix[0].length; j++) {
				double_matrix[i][j] = Double.parseDouble(matrix[i][j]);
			}
		}
		return double_matrix;
	}

	// This method will be used to generate a rgb String matrix from a double matrix
	public static String[][] generate_rgb_matrix(double[][] double_matrix) {
		var rgb_matrix = new String[double_matrix.length][double_matrix[0].length];
		for (int i = 0; i < double_matrix.length; i++) {
			for (int j = 0; j < double_matrix[0].length; j++) {
				double value = double_matrix[i][j];
				if (0 <= value && value < 32) {
					rgb_matrix[i][j] = "#00ff00";
				} else if (32 <= value && value < 64) {
					rgb_matrix[i][j] = "#40ff00";
				} else if (64 <= value && value < 96) {
					rgb_matrix[i][j] = "#80ff00";
				} else if (96 <= value && value < 128) {
					rgb_matrix[i][j] = "#c0ff00";
				} else if (128 <= value && value < 160) {
					rgb_matrix[i][j] = "#ffc000";
				} else if (160 <= value && value < 192) {
					rgb_matrix[i][j] = "#ff8000";
				} else if (192 <= value && value < 224) {
					rgb_matrix[i][j] = "#ff4000";
				} else if (224 <= value && value < 256) {
					rgb_matrix[i][j] = "#ff0000";
				} else {
					rgb_matrix[i][j] = "#aaaaaa";
				}
			}
		}
		return rgb_matrix;
	}

	// This method will be used to generate a rgb String list from a double matrix
	public static ArrayList<String> generate_rgb_list(double[][] double_matrix) {
		var rgb_list = new ArrayList<String>();
		String[][] rgb_matrix = generate_rgb_matrix(double_matrix);
		for (int i = 0; i < rgb_matrix.length; i++) {
			for (int j = 0; j < rgb_matrix[0].length; j++) {
				rgb_list.add(rgb_matrix[i][j]);
			}
		}
		return rgb_list;
	}

	// This method will be used to generate a coordinate matrix
	// This method will use coordinates of four corners as input
	public static double[][][] generate_coordinate_matrix(double[][] corners) {
		var coordinate_matrix = new double[11][11][2];
		double[] coordinate1 = corners[0];
		double[] coordinate2 = corners[1];
		double[] coordinate4 = corners[3];
		double row_step_length = (coordinate2[1] - coordinate1[1]) / 10;
		double col_step_length = (coordinate1[0] - coordinate4[0]) / 10;
		for (int i = 0; i < coordinate_matrix.length; i++) {
			for (int j = 0; j < coordinate_matrix[0].length; j++) {
				var coordinate = new double[2];
				coordinate[0] = coordinate1[0] - col_step_length * i;
				coordinate[1] = coordinate1[1] + row_step_length * j;
				coordinate_matrix[i][j] = coordinate;
			}
		}
		return coordinate_matrix;
	}

	// This method will be used to generate a point list from a coordinate matrix
	public static ArrayList<Point> generate_point_list(double[][][] coordinate_matrix) {
		var point_list = new ArrayList<Point>();
		for (int i = 0; i < coordinate_matrix.length; i++) {
			for (int j = 0; j < coordinate_matrix[0].length; j++) {
				double[] coordinate = coordinate_matrix[i][j];
				Point point = Point.fromLngLat(coordinate[1], coordinate[0]);
				point_list.add(point);
			}
		}
		return point_list;
	}

	// This method will be used to generate a polygon list from a point list
	public static ArrayList<Polygon> generate_polygon_list(ArrayList<Point> point_list) {
		var polygon_list = new ArrayList<Polygon>();
		int last_left_up_point_index = 108;
		for (int i = 0; i <= last_left_up_point_index; i++) {
			if ((i + 1) % 11 == 0) {
				continue;
			}
			Point left_up_coordinate = point_list.get(i);
			Point right_up_coordinate = point_list.get(i + 1);
			Point left_down_coordinate = point_list.get(i + 11);
			Point right_down_coordinate = point_list.get(i + 12);
			var coordinates = new ArrayList<Point>();
			coordinates.add(left_up_coordinate);
			coordinates.add(right_up_coordinate);
			coordinates.add(right_down_coordinate);
			coordinates.add(left_down_coordinate);
			coordinates.add(left_up_coordinate);
			var coordinates_list = new ArrayList<List<Point>>();
			coordinates_list.add(coordinates);
			Polygon polygon = Polygon.fromLngLats(coordinates_list);
			polygon_list.add(polygon);
		}
		return polygon_list;
	}

	// This method is used to generate a feature list and a rgb String list
	public static ArrayList<Feature> generate_feature_list(ArrayList<Polygon> polygon_list,
			ArrayList<String> rgb_list) {
		var feature_list = new ArrayList<Feature>();
		for (int i = 0; i < polygon_list.size(); i++) {
			Feature feature = Feature.fromGeometry(polygon_list.get(i));
			feature.addStringProperty("rgb-string", rgb_list.get(i));
			feature.addStringProperty("fill", rgb_list.get(i));
			feature.addNumberProperty("fill-opacity", 0.75);
			feature_list.add(feature);
		}
		return feature_list;
	}

	// This method is used to generate a feature collection from a feature list
	public static FeatureCollection generate_feature_collection(ArrayList<Feature> feature_list) {
		FeatureCollection collection = FeatureCollection.fromFeatures(feature_list);
		return collection;
	}

	// This method is used to write a GeoJson file by using toJson() method
	public static void write_geojson_file(FeatureCollection collection) {
		String json_string = collection.toJson();
		try (FileWriter file = new FileWriter("heatmap.geojson")) {
			file.write(json_string);
			file.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws FileNotFoundException {
		// read file and generate a String matrix
		String[][] matrix = read_txt_file(args[0]);
		// change a String matrix to a double matrix
		double[][] double_matrix = generate_double_matrix(matrix);
		// use double matrix to generate a rgb String list
		ArrayList<String> rgb_list = generate_rgb_list(double_matrix);
		// define the four corners of the confinement area
		double[][] corners = { { 55.946233, -3.192473 }, { 55.946233, -3.184319 }, { 55.942617, -3.184319 },
				{ 55.942617, -3.192473 } };
		// generate a coordinate matrix by using four corners
		double[][][] coordinate_matrix = generate_coordinate_matrix(corners);
		// generate a point list by using coordinate matrix
		ArrayList<Point> point_list = generate_point_list(coordinate_matrix);
		// generate a polygon list by using a point list
		ArrayList<Polygon> polygon_list = generate_polygon_list(point_list);
		// generate a feature list by using a polygon list and a rgb String list
		ArrayList<Feature> feature_list = generate_feature_list(polygon_list, rgb_list);
		// generate a feature collection object by using a feature list
		FeatureCollection collection = generate_feature_collection(feature_list);
		// write a GeoJson file by using a feature collection object
		write_geojson_file(collection);
	}
}

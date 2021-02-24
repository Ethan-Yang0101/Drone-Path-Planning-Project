package uk.ac.ed.inf.aqmaps;

public class FlightDetail {

	public final int count_step;
	public final double current_step_longitude;
	public final double current_step_latitude;
	public final int move_degree;
	public final double next_step_longitude;
	public final double next_step_latitude;
	public final String sensor_location;

	public FlightDetail(int count_step, double current_step_longitude, double current_step_latitude, int move_degree,
			double next_step_longitude, double next_step_latitude, String sensor_location) {
		this.count_step = count_step;
		this.current_step_longitude = current_step_longitude;
		this.current_step_latitude = current_step_latitude;
		this.move_degree = move_degree;
		this.next_step_longitude = next_step_longitude;
		this.next_step_latitude = next_step_latitude;
		this.sensor_location = sensor_location;
	}

	public String toString() {
		String path_info = count_step + "," + current_step_longitude + "," + current_step_latitude + "," + move_degree
				+ "," + next_step_longitude + "," + next_step_latitude + "," + sensor_location + "\n";
		return path_info;
	}

}

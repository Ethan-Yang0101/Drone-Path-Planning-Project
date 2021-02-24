package uk.ac.ed.inf.aqmaps;

/*
This class is used to provide FlightDetail object which contains all the
information needed for writing flight path file by Drone.
*/
public class FlightDetail {

	private final int count_step; // A field refer to step number of current step.
	private final double current_step_longitude; // A field refer to longitude of current step.
	private final double current_step_latitude; // A field refer to latitude of current step.
	private final int move_degree; // A field refer to direction of move in degree.
	private final double next_step_longitude; // A field refer to longitude of next step.
	private final double next_step_latitude; // A field refer to latitude of next step.
	private final String sensor_location; // A field refer to what3words location of sensor.

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

	// This method returns String of a row of flight path file by using fields.
	public String toString() {
		String path_info = count_step + "," + current_step_longitude + "," + current_step_latitude + "," + move_degree
				+ "," + next_step_longitude + "," + next_step_latitude + "," + sensor_location;
		return path_info;
	}

}

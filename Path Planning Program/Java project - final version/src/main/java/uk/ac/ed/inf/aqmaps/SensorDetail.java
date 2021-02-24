package uk.ac.ed.inf.aqmaps;

/*
This class is used to provide SensorDetail object which contains 
location, reading and battery value of sensor. 
This class is used when store web data into its object.
*/
public class SensorDetail {

	private String location;
	private double battery;
	private String reading;

	public String getLocation() {
		return location;
	}

	public double getBattery() {
		return battery;
	}

	public String getReading() {
		return reading;
	}

}

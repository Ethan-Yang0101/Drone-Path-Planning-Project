package uk.ac.ed.inf.aqmaps;

/*
This class is used to provide LocationDetail object 
which stores location data get from web server. 
This class is used when store web data into its object.
*/
public class LocationDetail {

	private Coordinates coordinates;

	public static class Coordinates {
		private double lng;
		private double lat;

		public double getLng() {
			return lng;
		}

		public double getLat() {
			return lat;
		}
	}

	public Coordinates getCoordinates() {
		return coordinates;
	}

}

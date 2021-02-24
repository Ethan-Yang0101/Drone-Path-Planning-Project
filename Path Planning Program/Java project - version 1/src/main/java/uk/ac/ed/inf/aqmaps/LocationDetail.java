package uk.ac.ed.inf.aqmaps;

public class LocationDetail {

	public String country;
	public String nearestPlace;
	public String words;
	public String language;
	public String map;
	public Square square;
	public Coordinates coordinates;

	public static class Square {

		public SouthWest southwest;
		public NorthEast northeast;

		public static class SouthWest {
			public double lng;
			public double lat;
		}

		public static class NorthEast {
			public double lng;
			public double lat;
		}

	}

	public static class Coordinates {
		public double lng;
		public double lat;
	}

}

package gov.usgs.earthworm;

/**
 * A class for holding SCN data.
 * 
 * @author Dan Cervelli
 */
public class SCN {
	public final String station;
	public final String channel;
	public final String network;

	public SCN(String s, String c, String n) {
		station = s;
		channel = c;
		network = n;
	}

	public String toString() {
		return station + "_" + channel + "_" + network;
	}
}
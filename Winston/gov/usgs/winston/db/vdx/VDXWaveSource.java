package gov.usgs.winston.db.vdx;

import gov.usgs.util.UtilException;
import gov.usgs.vdx.client.VDXClient.DownsamplingType;
import gov.usgs.vdx.data.BinaryDataSet;

/**
 * 
 * @author Dan Cervelli
 */
public class VDXWaveSource extends VDXSource {
	
	public String getType() {
		return "wave";
	}
	
	public void disconnect() {
		defaultDisconnect();
	}

	protected BinaryDataSet getData(String channel, double st, double et, int maxrows, DownsamplingType ds, int dsInt) throws UtilException {
		return data.getWave(channel, st, et, maxrows);
	}
}

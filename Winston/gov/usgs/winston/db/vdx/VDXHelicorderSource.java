package gov.usgs.winston.db.vdx;

import gov.usgs.util.UtilException;
import gov.usgs.vdx.client.VDXClient.DownsamplingType;
import gov.usgs.vdx.data.BinaryDataSet;

/**
 * 
 * @author Dan Cervelli
 */
public class VDXHelicorderSource extends VDXSource {
	
	public String getType() {
		return "helicorder";
	}
	
	public void disconnect() {
		defaultDisconnect();
	}
	
	protected BinaryDataSet getData(String channel, double st, double et, int maxrows, DownsamplingType ds, int dsInt) throws UtilException{
		return data.getHelicorderData(channel, st, et, maxrows);
	}
}

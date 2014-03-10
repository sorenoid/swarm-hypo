package gov.usgs.winston.db.vdx;

import gov.usgs.util.UtilException;
import gov.usgs.vdx.client.VDXClient.DownsamplingType;
import gov.usgs.vdx.data.BinaryDataSet;
import gov.usgs.vdx.data.rsam.RSAMData;

/**
 * 
 * $Log: not supported by cvs2svn $
 *
 * @author Tom Parker
 */
public class VDXRatSAMSource extends VDXSource
{
	public String getType()
	{
		return "rsam";
	}
	
	public void disconnect() {
		defaultDisconnect();
	}

	protected BinaryDataSet getData(String channel, double st, double et, int maxrows, DownsamplingType ds, int dsInt) throws UtilException
	{
		RSAMData d1 = null;
		RSAMData d2 = null;
		String[] channels = channel.split(",");
		if (channels.length == 2)
		{
			d1 = data.getRSAMData(channels[0], st, et, maxrows, ds, dsInt);
			d2 = data.getRSAMData(channels[1], st, et, maxrows, ds, dsInt);
		}
		
		return (BinaryDataSet) d1.getRatSAM(d2);
	}
}

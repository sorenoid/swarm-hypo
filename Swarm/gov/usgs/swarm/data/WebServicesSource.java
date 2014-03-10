package gov.usgs.swarm.data;

import gov.usgs.swarm.ChannelGroupInfo;
import gov.usgs.swarm.ChannelInfo;
import gov.usgs.swarm.Swarm;
import gov.usgs.util.CurrentTime;
import gov.usgs.vdx.data.heli.HelicorderData;
import gov.usgs.vdx.data.wave.Wave;

import java.util.Collections;
import java.util.List;

public class WebServicesSource extends SeismicDataSource
{
	/** Web services source tab title. */
	public static final String TAB_TITLE = "IRIS WS";
	/** Web services source description. */
	public static final String DESCRIPTION = "an IRIS Web Services server";
	/** Web-Services client code */
	public static final String WEB_SERVICES_CLIENT_CODE = "wsc";
	/** Parameter split text. */
	public static final String PARAM_SPLIT_TEXT = "\\|";
	/** Parameter format text. */
	public static final String PARAM_FMT_TEXT = "%s|%s|%s|%s|%d|%d|%s|%s";
	/** instance counter */
	private static int counter = 0;
	/** The channel. */
	private final String chan;
	/** Web Services Client. */
	private final WebServicesClient client;
	/** instance count */
	private final int count = ++counter;
	/** The gulp delay. */
	private final int gulpDelay;
	/** The gulp size. */
	private final int gulpSize;
	/** The location. */
	private final String loc;
	/** The network. */
	private final String net;
	/** The colon separated parameters. */
	private final String params;
	/** The station. */
	private final String sta;
	/** Web Services dataselect URL. */
	private final String wsDataSelectUrl;
	/** Web Services station URL. */
	private final String wsStationUrl;
	
	/**
	 * Create a Web Services server source.
	 * 
	 * @param s the colon separated parameters.
	 */
	public WebServicesSource(String s)
	{
		/** IRIS prefers a 60 second interval */
		minimumRefreshInterval = 60;

		params = s;
		String[] ss = params.split(PARAM_SPLIT_TEXT);
		int ssIndex = 0;
		net = ss[ssIndex++];
		sta = ss[ssIndex++];
		loc = ss[ssIndex++];
		chan = ss[ssIndex++];
		gulpSize = Integer.parseInt(ss[ssIndex++]);
		gulpDelay = Integer.parseInt(ss[ssIndex++]);
		wsDataSelectUrl = ss[ssIndex++];
		wsStationUrl = ss[ssIndex++];
		client = new WebServicesClient(this, net, sta, loc, chan,
				wsDataSelectUrl, wsStationUrl);
		if (WebServiceUtils.isDebug())
		{
			WebServiceUtils.debug("web service started " + count);
		}
	}

	/**
	 * Create a Web Services server source with the same parameters as the
	 * specified Web Services server source.
	 * 
	 * @param sls the Web Services server source.
	 */
	public WebServicesSource(WebServicesSource sls)
	{
		this(sls.params);
		name = sls.name;
	}

	/**
	 * Close the data source.
	 */
	public synchronized void close()
	{
		if (WebServiceUtils.isDebug())
		{
			WebServiceUtils.debug("web service closed " + count);
		}
	}

	/**
	 * Get the channels.
	 * 
	 * @return the list of channels.
	 */
	public synchronized List<String> getChannels()
	{
		List<String> channels = client.getChannels();
		return Collections.unmodifiableList(channels);
	}

	/**
	 * Get a copy of this data source.
	 * 
	 * @return a copy of this data source.
	 */
	public SeismicDataSource getCopy()
	{
		return new WebServicesSource(this);
	}

	/**
	 * Get the gulper key for the specified station.
	 * 
	 * @param station the station.
	 * @return the gulper key.
	 */
	private String getGulperKey(String station)
	{
		return WEB_SERVICES_CLIENT_CODE + ":" + station;
	}

	/**
	 * Get the helicorder data.
	 * 
	 * @param station the station.
	 * @param t1 the start time.
	 * @param t2 the end time.
	 * @param gl the gulper listener.
	 * @return the helicorder data or null if none.
	 */
	public synchronized HelicorderData getHelicorder(String station, double t1,
			double t2, GulperListener gl)
	{
		// lifted from gov.usgs.swarm.data.DHIDataSource
		double now = CurrentTime.getInstance().nowJ2K();
		// if a time later than now has been asked for make sure to get the
		// latest so that, if possible, a small bit of helicorder data will be
		// displayed
		// if ((t2 - now) >= -20)
		// {
		// getWave(station, now - 2*60, now);
		// }

		CachedDataSource cache = CachedDataSource.getInstance();

		HelicorderData hd = cache.getHelicorder(station, t1, t2,
				(GulperListener) null);

		if (hd == null || hd.rows() == 0 || (hd.getStartTime() - t1 > 10))
			GulperList.getInstance().requestGulper(getGulperKey(station), gl,
					this, station, t1, t2, gulpSize, gulpDelay);

		// this gets the tail end, replacing commented out section above
		if (hd != null && hd.getEndTime() < now)
			getWave(station, hd.getEndTime(), now);

		return hd;
	}

	/**
	 * Either returns the wave successfully or null if the data source could not
	 * get the wave.
	 * 
	 * @param station the station.
	 * @param t1 the start time.
	 * @param t2 the end time.
	 * @return the wave or null if none.
	 */
	public synchronized Wave getWave(String station, double t1, double t2)
	{
		CachedDataSource cache = CachedDataSource.getInstance();

		Wave sw = null;
		if (useCache)
			sw = cache.getWave(station, t1, t2);
		if (sw == null)
		{
			ChannelInfo channelInfo = new ChannelGroupInfo(station);
			sw = client.getRawData(channelInfo, t1, t2);
			if (sw == null)
				return null;
			if (useCache)
			{
				cache.cacheWaveAsHelicorder(station, sw);
				cache.putWave(station, sw);
			}
		}
		return sw;
	}

	/**
	 * Is this data source active; that is, is new data being added in real-time
	 * to this data source?
	 * 
	 * @return whether or not this is an active data source.
	 */
	public synchronized boolean isActiveSource()
	{
		return true;
	}

	public synchronized void notifyDataNotNeeded(String station, double t1,
			double t2, GulperListener gl)
	{
		GulperList.getInstance().killGulper(getGulperKey(station), gl);
	}

	/**
	 * Get the configuration string.
	 * 
	 * @return the configuration string.
	 */
	public String toConfigString()
	{
		return String.format("%s;%s:" + PARAM_FMT_TEXT, name,
				WEB_SERVICES_CLIENT_CODE, net, sta, loc, chan, gulpSize,
				gulpDelay, wsDataSelectUrl, wsStationUrl);
	}
}

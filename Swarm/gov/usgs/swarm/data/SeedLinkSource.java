package gov.usgs.swarm.data;

import gov.usgs.swarm.ChannelUtil;
import gov.usgs.util.Util;
import gov.usgs.vdx.data.heli.HelicorderData;
import gov.usgs.vdx.data.wave.Wave;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An implementation of <code>SeismicDataSource</code> that connects to an
 * SeedLink Server.
 * 
 * @author Kevin Frechette (ISTI)
 */
public class SeedLinkSource extends SeismicDataSource
{
	/** SeedLinkSource class name */
	private static final String CLASS_NAME = "gov.usgs.swarm.data.SeedLinkSource";

	/** The gulp delay. */
	private static final int gulpDelay = 1000;

	/** The gulp size. */
	private static final int gulpSize = 60;

	/** Info string prefix text or null if none. */
	private static final String infoFileText;

	/** The logger. */
	private static final Logger logger;

	/** Log prefix text. */
	private static final String logPrefixText;

	/** SeedLink Source code */
	public static final String SEEDLINK_SOURCE_CODE = "sls";

	static
	{
		logger = SeedLinkLog.logger;
		logPrefixText = CLASS_NAME + ": ";
		infoFileText = System.getProperty(SEEDLINK_SOURCE_CODE + "infofile");
	}

	/**
	 * Gets a <code>String</code> representation of the object.
	 * 
	 * @return the <code>String</code> representation of the object
	 */
	public static String toString(Object obj)
	{
		if (obj instanceof Wave)
		{
			Wave wave = (Wave) obj;
			return "Wave(s=" + Util.j2KToDateString(wave.getStartTime())
					+ ", e=" + Util.j2KToDateString(wave.getEndTime())
					+ ", sr=" + wave.getSamplingRate() + ", n="
					+ wave.buffer.length + ")";
		}
		else if (obj instanceof HelicorderData)
		{
			HelicorderData hd = (HelicorderData) obj;
			return "HelicorderData(s="
					+ Util.j2KToDateString(hd.getStartTime()) + ", e="
					+ Util.j2KToDateString(hd.getEndTime()) + ")";
		}
		else
		{
			return String.valueOf(obj);
		}
	}

	/** The server host. */
	private final String host;

	/** The information string File or null if none. */
	private File infoStringFile;

	/** The colon separated parameters. */
	private final String params;

	/** The server port. */
	private final int port;

	/** SeedLink client list. */
	private final List<SeedLinkClient> seedLinkClientList;

	/**
	 * Create a SeedLink server source with the same parameters as the specified
	 * SeedLink server source.
	 * 
	 * @param sls the SeedLink server source.
	 */
	public SeedLinkSource(SeedLinkSource sls)
	{
		this(sls.params);
		name = sls.name;
	}

	/**
	 * Create a SeedLink server source.
	 * 
	 * @param s the colon separated parameters.
	 */
	public SeedLinkSource(String s)
	{
		params = s;
		String[] ss = params.split(":");
		int ssIndex = 0;
		host = ss[ssIndex++];
		port = Integer.parseInt(ss[ssIndex++]);
		seedLinkClientList = new ArrayList<SeedLinkClient>();
		if (infoFileText != null)
		{
			infoStringFile = new File(infoFileText + host + port + ".xml");
		}
	}

	/**
	 * Close the data source.
	 */
	public void close()
	{
		// close clients
		synchronized (seedLinkClientList)
		{
			if (seedLinkClientList.size() != 0)
			{
				logger.fine(logPrefixText + "close the data source");
				for (SeedLinkClient client : seedLinkClientList)
				{
					client.close();
				}
				seedLinkClientList.clear();
			}
		}
	}

	/**
	 * Create a client.
	 * 
	 * @return the client.
	 */
	protected SeedLinkClient createClient()
	{
		final SeedLinkClient client = new SeedLinkClient(host, port);
		synchronized (seedLinkClientList)
		{
			seedLinkClientList.add(client);
		}
		return client;
	}

	public IGulper createGulper(GulperList gl, String k, String ch, double t1,
			double t2, int size, int delay)
	{
		return new SeedLinkGulper(gl, k, this, ch, t1, t2, size, delay);
	}

	/**
	 * Get the channels.
	 * 
	 * @return the list of channels.
	 */
	public List<String> getChannels()
	{
		SeedLinkChannelInfo seedLinkChannelInfo = new SeedLinkChannelInfo(this);
		List<String> channels = null;
		try
		{
			final String infoString;
			if (infoStringFile != null && infoStringFile.canRead())
			{
				infoString = SeedLinkChannelInfo.readFile(infoStringFile);
			}
			else
			{
				infoString = getInfoString();
				if (infoStringFile != null && infoString.length() != 0)
				{
					SeedLinkChannelInfo.writeString(infoStringFile, infoString);
				}
			}
			if (infoString.length() != 0)
			{
				seedLinkChannelInfo.parse(infoString);
				channels = seedLinkChannelInfo.getChannels();
			}
		}
		catch (Exception ex)
		{
			logger.log(Level.WARNING, logPrefixText + "could not get channels",
					ex);
		}
		if (channels == null || channels.size() == 0)
		{
			logger.warning(logPrefixText + "could not get channels");
			return Collections.emptyList();
		}
		ChannelUtil.assignChannels(channels, this);
		return Collections.unmodifiableList(channels);
	}

	/**
	 * Get a copy of this data source.
	 * 
	 * @return a copy of this data source.
	 */
	public SeismicDataSource getCopy()
	{
		return new SeedLinkSource(this);
	}

	/**
	 * Get the gulper key for the specified station.
	 * 
	 * @param station the station.
	 * @return the gulper key.
	 */
	private String getGulperKey(String station)
	{
		return SEEDLINK_SOURCE_CODE + ":" + station;
	}

	/**
	 * Get the helicorder data.
	 * 
	 * @param scnl the scnl.
	 * @param t1 the start time.
	 * @param t2 the end time.
	 * @param gl the gulper listener.
	 * @return the helicorder data or null if none.
	 */
	public HelicorderData getHelicorder(String scnl, double t1, double t2,
			GulperListener gl)
	{
		// check if data is in the cache
		HelicorderData hd = CachedDataSource.getInstance().getHelicorder(scnl,
				t1, t2, gl);
		// if no data or data start time is greater than requested
		if (hd == null || hd.rows() == 0 || (hd.getStartTime() - t1 > 10)
				|| hd.getEndTime() < t2)
		{
			requestGulper(scnl, t1, t2, gl);
		}
		// if data end time is less than requested
		else if (hd.getEndTime() < t2)
		{
			requestGulper(scnl, hd.getEndTime(), t2, gl);
		}
		final Level level = Level.FINE;
		if (logger.isLoggable(level))
		{
			logger.log(
					level,
					logPrefixText + "getHelicorder(scnl=" + scnl + ", start="
							+ Util.j2KToDateString(t1) + ", end="
							+ Util.j2KToDateString(t2) + ")\nDATA="
							+ (hd == null ? "NONE" : toString(hd)));
		}
		return hd;
	}

	/**
	 * Get the SeedLink information string.
	 * 
	 * @return the SeedLink information string or null if error.
	 */
	public String getInfoString()
	{
		final SeedLinkClient client = createClient();
		final String infoString = client.getInfoString();
		removeClient(client);
		return infoString;
	}

	/**
	 * Either returns the wave successfully or null if the data source could not
	 * get the wave.
	 * 
	 * @param scnl the scnl.
	 * @param t1 the start time.
	 * @param t2 the end time.
	 * @return the wave or null if none.
	 */
	public Wave getWave(String scnl, double t1, double t2)
	{
		// check if data is in the cache
		Wave wave = CachedDataSource.getInstance().getWave(scnl, t1, t2);
		if (wave == null)
		{
			// remove all data in the future to avoid blocking
			final double now = Util.nowJ2K();
			if (t1 <= now)
			{
				final SeedLinkClient client = createClient();
				wave = client.getWave(scnl, t1, t2);
				removeClient(client);
			}
		}
		final Level level = Level.FINE;
		if (logger.isLoggable(level))
		{
			logger.log(
					level,
					logPrefixText + "getWave(scnl=" + scnl + ", start="
							+ Util.j2KToDateString(t1) + ", end="
							+ Util.j2KToDateString(t2) + ")\nDATA="
							+ (wave == null ? "NONE" : toString(wave)));
		}
		return wave;
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
	 * Remove the client.
	 * 
	 * @param client the client.
	 */
	protected void removeClient(SeedLinkClient client)
	{
		synchronized (seedLinkClientList)
		{
			seedLinkClientList.remove(client);
		}
	}

	/**
	 * Request data from the gulper.
	 * 
	 * @param scnl the scnl.
	 * @param t1 the start time.
	 * @param t2 the end time.
	 * @param gl the gulper listener.
	 */
	protected void requestGulper(String scnl, double t1, double t2,
			GulperListener gl)
	{
		GulperList.getInstance().requestGulper(getGulperKey(scnl), gl, this,
				scnl, t1, t2, gulpSize, gulpDelay);
	}

	/**
	 * Get the configuration string.
	 * 
	 * @return the configuration string.
	 */
	public String toConfigString()
	{
		return String.format("%s;%s:%s:%d", name, SEEDLINK_SOURCE_CODE, host,
				port);
	}
}

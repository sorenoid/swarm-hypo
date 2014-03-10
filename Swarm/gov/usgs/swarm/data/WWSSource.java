package gov.usgs.swarm.data;

import gov.usgs.earthworm.Menu;
import gov.usgs.earthworm.MenuItem;
import gov.usgs.net.ReadListener;
import gov.usgs.swarm.Metadata;
import gov.usgs.swarm.Swarm;
import gov.usgs.util.CurrentTime;
import gov.usgs.util.Util;
import gov.usgs.vdx.data.heli.HelicorderData;
import gov.usgs.vdx.data.wave.Wave;
import gov.usgs.winston.Channel;
import gov.usgs.winston.Instrument;
import gov.usgs.winston.server.WWSClient;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

/**
 * An implementation of <code>SeismicDataSource</code> that communicates
 * with a WinstonWaveServer.  This is essentially just a copy of 
 * WaveServerSource with different helicorder functions.  It should probably 
 * be made a descendant of WaveServerSource. 
 * 
 *
 * @author Dan Cervelli
 */
public class WWSSource extends SeismicDataSource
{
	private String params;
	private WWSClient winstonClient;
	private int timeout = 2000;
	private boolean compress = false;
	private int protocolVersion = 1;
	
	private String server;
	private int port;
	
	private boolean established;
	
	public WWSSource(String s)
	{
		params = s;
		String[] ss = params.split(":");
		server = ss[0];
		port = Integer.parseInt(ss[1]);
		timeout = Integer.parseInt(ss[2]);
		compress = ss[3].equals("1");
		
		winstonClient = new WWSClient(server, port);
		
		setTimeout(timeout);
	}
	
	public WWSSource(WWSSource wws)
	{
		this(wws.params);
		protocolVersion = wws.protocolVersion;
		name = wws.name;
	}
 
	public SeismicDataSource getCopy()
	{
		return new WWSSource(this);	
	}
	
	public void establish()
	{
		if (!established)
		{
			protocolVersion = winstonClient.getProtocolVersion();
			established = true;
		}
	}
	
	public String toConfigString()
	{
		return String.format("%s;wws:%s:%d:%d:%s", name, server, port, timeout, compress ? "1" : "0");
	}
	
	public synchronized void setTimeout(int to)
	{
		winstonClient.setTimeout(to);
	}
	
	public void close()
	{
		if (winstonClient != null)
			winstonClient.close();
	}
	
	public String getFormattedSCNL(MenuItem mi)
	{
		return mi.getSCNSCNL(" ");
	}
	
	public List<String> getMenuList(List<MenuItem> items)
	{
		List<String> list = new ArrayList<String>(items.size());
		for (Iterator<MenuItem> it = items.iterator(); it.hasNext(); )
		{
			MenuItem mi = it.next();
			list.add(getFormattedSCNL(mi));
		}
		return list;
	}
	
	public String[] parseSCNL(String channel)
	{
		String[] result = new String[4];
		StringTokenizer st = new StringTokenizer(channel, " ");
		result[0] = st.nextToken();
		result[1] = st.nextToken();
		result[2] = st.nextToken();
		if (st.hasMoreTokens())
			result[3] = st.nextToken();
		else
			result[3] = "--";
		return result;
	}
	
	public synchronized Wave getWave(String station, double t1, double t2)
	{
		Wave wave = null;
		if (useCache) {
			CachedDataSource cache = CachedDataSource.getInstance();
			wave = cache.getWave(station, t1, t2);
		}

		if (wave == null)
		{
			String[] scnl = parseSCNL(station);
			if (protocolVersion == 1)
			{
				wave = winstonClient.getRawData(scnl[0], scnl[1], scnl[2], scnl[3], Util.j2KToEW(t1), Util.j2KToEW(t2));
				if (wave != null)
					wave.convertToJ2K();
			}
			else
				wave = winstonClient.getWave(scnl[0], scnl[1], scnl[2], scnl[3], t1, t2, compress);
			
			if (wave == null)
				return null;
			
			wave.register();
			if (useCache) {
				CachedDataSource cache = CachedDataSource.getInstance();
				cache.putWave(station, wave);
			}
		}
		else
		{
			//System.out.println("cached");	
		}
		return wave;
	}
	
	public synchronized HelicorderData getHelicorder(final String station, double t1, double t2, GulperListener gl)
	{
		CachedDataSource cache = CachedDataSource.getInstance();

		HelicorderData hd = cache.getHelicorder(station, t1, t2, this);
		if (hd == null)
		{
			String[] scnl = parseSCNL(station);
			fireHelicorderProgress(station, -1);
			winstonClient.setReadListener(new ReadListener()
					{
						public void readProgress(double p)
						{
							fireHelicorderProgress(station, p);
						}
					});
			hd = winstonClient.getHelicorder(scnl[0], scnl[1], scnl[2], scnl[3], t1, t2, compress);
			winstonClient.setReadListener(null);
			fireHelicorderProgress(station, 1.0);
			
			if (hd != null && hd.rows() != 0)
			{
				HelicorderData noLatest = hd.subset(hd.getStartTime(), CurrentTime.getInstance().nowJ2K() - 30);
				if (noLatest != null && noLatest.rows() > 0)
					cache.putHelicorder(station, noLatest);
				//cache.putHelicorder(station, hd);
			}
			else
				hd = null;
		}
		return hd;
	}
	
	public synchronized List<String> getChannels()
	{
		if (protocolVersion == 1)
//		if (true)
		{
			Menu menu = winstonClient.getMenuSCNL();
			List<String> channels = getMenuList(menu.getSortedItems());
			Swarm.config.assignMetadataSource(channels, this);
			return channels;
		}
		else if (protocolVersion == 2)
		{
			List<Channel> channels = winstonClient.getChannels();
			List<String> result = new ArrayList<String>(channels.size());
			for (Channel ch : channels)
			{
				String code = ch.getCode().replace('$', ' ');
				Metadata md = Swarm.config.getMetadata(code, true);
				Instrument ins = ch.getInstrument();
				md.updateLongitude(ins.getLongitude());
				md.updateLatitude(ins.getLatitude());
				md.source = this;
				result.add(code);
			}
			return result;
		}
		else if (protocolVersion == 3)
		{
			List<Channel> channels = winstonClient.getChannels(true);
			List<String> result = new ArrayList<String>(channels.size());
			for (Channel ch : channels)
			{
				String code = ch.getCode().replace('$', ' ');
				Metadata md = Swarm.config.getMetadata(code, true);
				Instrument ins = ch.getInstrument();
				md.updateLongitude(ins.getLongitude());
				md.updateLatitude(ins.getLatitude());
				List<String> groups = ch.getGroups();
				if (groups != null)
				{
					for (String g : groups)
						md.addGroup(g);
				}
				md.updateLinearCoefficients(ch.getLinearA(), ch.getLinearB());
				md.updateAlias(ch.getAlias());
				md.updateUnits(ch.getUnit());
				md.updateTimeZone(ch.getInstrument().getTimeZone());
				md.source = this;
				result.add(code);
			}
			return result;
		}
		else 
			return null;
	}
	
	public synchronized boolean isActiveSource()
	{
		return true;	
	}
}

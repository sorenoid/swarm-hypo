package gov.usgs.winston.monitor;

import gov.usgs.net.Server;
import gov.usgs.util.ConfigFile;
import gov.usgs.util.Log;
import gov.usgs.util.Util;
import gov.usgs.winston.monitor.server.ServerHandler;

import java.util.ArrayList;
import java.util.logging.Level;

public class MonitorServer extends Server implements Runnable
{

	private ConfigFile config;
	public ArrayList<Collector> collectors;
	
	private static final String DEFAULT_WWS = "WWS";
	private static final String DEFAULT_TITLE = "Winston Dashboard";
	
	public String defaultWWS;
	public String title;
	
	public MonitorServer (ConfigFile cf, ArrayList<Collector> collectors){
		super();
		config = cf;
		this.collectors = collectors;
		
		if (cf == null)
			logger.info("Starting MonitorServer with default config");
		
		processConfigFile();
		
		for (int i = 0; i < 4; i++)
			addCommandHandler(new ServerHandler(this));
	}

	/**
	 * Processes the configuration file (default 'WWS.config').  See the default
	 * file for documentation on the different options.
	 */
	public void processConfigFile()
	{

		int p = Util.stringToInt(config.getString("port"), 9090);
		serverPort = p;
		logger.info("config: server=" + serverPort);
		
		defaultWWS = Util.stringToString(config.getString("defaultWWS"), DEFAULT_WWS);
		logger.info("config: server.defaultWWS=" + defaultWWS);
		
		title = Util.stringToString(config.getString("title"), DEFAULT_TITLE);
	}

	public void setLogLevel(Level level)
	{
		if (level.equals(Level.ALL))
			logger.info("Logging set to high.");
		else if (level.equals(Level.FINE))
			logger.info("Logging set to normal.");
		else if (level.equals(Level.SEVERE))
			logger.info("Logging set to low.");
		else if (level.equals(Level.OFF))
			logger.info("Logging turned off.");
		
		// change root logger
		Log.getLogger("gov.usgs").setLevel(level);
	}

	public void run()
	{
		startListening();
	}

	public Collector getCollector(String collectorName) {
		for (Collector c : collectors)
			if (c.getName().equals(collectorName))
			return c;
		
		return null;
	}
}

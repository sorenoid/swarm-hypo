package gov.usgs.winston.monitor;

import gov.usgs.net.HttpRequest;
import gov.usgs.util.ConfigFile;
import gov.usgs.util.Log;
import gov.usgs.util.Util;
import gov.usgs.winston.db.WinstonDatabase;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.rrd4j.graph.RrdGraphDef;

abstract public class Collector extends Thread
{
	public static final String DEFAULT_LOG_LEVEL = "FINE";
	public static final String DEFAULT_DRIVER = "com.mysql.jdbc.Driver";
	public static final int DEFAULT_PLOT_HEIGHT = 134;
	public static final int DEFAULT_PLOT_WIDTH = 581;

	protected final Logger logger;
	protected String logFile;
	protected int logNumFiles;
	protected int logSize;
	protected boolean quit;
	protected String rrdBase;
	protected int inspectionInterval;

	protected Color avgColor = new Color(0x02,0x47,0x69);
	protected Color minColor = new Color(1f,1f,1f);
	protected Color maxColor = new Color(0x95,0xcb,0xe9,0x88);

	protected WinstonDatabase winston;
	
	abstract public void configure(String name, ConfigFile collectorConfig, ConfigFile winstonConfig);
	abstract public byte[] createPlot(HttpRequest request) throws IOException;
	abstract public String createPlotPane(HttpRequest request) throws IOException;
	abstract protected void poll();
	abstract public String getMenu();
	
	public void run() {
		while (!quit)
		{
			poll();
			try {
				Thread.sleep(inspectionInterval * 60 * 1000);
			} catch (InterruptedException e) {}
		}
	}
	
	
	public ArrayList<String> getStations()
	{
		ArrayList<String> files = new ArrayList<String>();
		File rrdDir = new File(rrdBase);

		Pattern p = Pattern.compile(getName() + "_(.*)\\.rrd");
		
		for (String fn : rrdDir.list())
		{
			Matcher m = p.matcher(fn);
			if (m.matches())
				files.add(m.replaceFirst("$1").replace('$', ' '));
		}
		
		Collections.sort(files);
		return files;
	}
	
	/**
	 * Sets up all the standard private member variables, the logger and
	 * instantiates a ConfigFile using configFilename.
	 */
	public Collector()
	{
		Thread.currentThread().setName("Collector");
		
		logger = Log.getLogger("gov.usgs.winston.monitor.collector");
		logger.setLevel(Level.parse(DEFAULT_LOG_LEVEL));
	}

	/**
	 * Logs a severe message and exits uncleanly.
	 */
	protected void fatalError(String msg)
	{
		logger.severe(msg);
		System.exit(1);
	}
	protected void winstonConfig(ConfigFile wc)
	{
		String winstonDriver = Util.stringToString(wc.getString("driver"), DEFAULT_DRIVER);
		logger.info("config: winston.driver=" + winstonDriver);
		
		String winstonPrefix = wc.getString("prefix");
		if (winstonPrefix == null)
			fatalError("winston.prefix is missing from config file.");
		logger.info("config: winston.prefix=" + winstonPrefix);

		String winstonURL = wc.getString("url");
		if (winstonURL == null)
			fatalError("winston.url is missing from config file.");
		logger.info("config: winston.url=" + winstonURL);

		winston = new WinstonDatabase(winstonDriver, winstonURL, winstonPrefix);
		if (!winston.checkDatabase())
			fatalError("Winston database does not exist.");
	}

	public void quit() {
		quit = true;
	}
	
	protected RrdGraphDef getGDef()
	{
		RrdGraphDef gDef = new RrdGraphDef();
		gDef.setPoolUsed(false);
		gDef.setShowSignature(false);
		gDef.setImageFormat("png");
		gDef.setFilename("-");
		gDef.setHeight(DEFAULT_PLOT_HEIGHT);
		gDef.setWidth(DEFAULT_PLOT_WIDTH);
		gDef.setEndTime(System.currentTimeMillis()/1000);
		return gDef;
	}

}

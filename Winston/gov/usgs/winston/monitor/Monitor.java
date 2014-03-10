package gov.usgs.winston.monitor;


import gov.usgs.earthworm.ImportGeneric;
import gov.usgs.util.ConfigFile;
import gov.usgs.util.Log;
import gov.usgs.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.SimpleJSAP;
import com.martiansoftware.jsap.Switch;
import com.martiansoftware.jsap.UnflaggedOption;

/**
 * The Winston monitor.
 * 
 * @author Tom Parker
 * 
 */

public class Monitor extends Thread 
{
	public static final String DEFAULT_CONFIG_FILENAME = "WinstonMonitor.config";
	public static final String DEFAULT_HOST = "localhost";
	public static final int DEFAULT_PORT = 8080;
	public static final int DEFAULT_TIMEOUT = 2000;
	public static final int DEFAULT_STATUS_INTERVAL = 60;
	public static final String DEFAULT_DRIVER = "com.mysql.jdbc.Driver";
	public static final int DEFAULT_MAX_DAYS = 0;
	public static final int DEFAULT_MAX_BACKLOG = 100;
	public static final String DEFAULT_LOG_LEVEL = "FINE";
	public static final String DEFAULT_LOG_FILE = "monitor.log";
	public static final int DEFAULT_LOG_NUM_FILES = 10;
	public static final int DEFAULT_LOG_FILE_SIZE = 1000000;
	public static final String DEFAULT_RRD_DIR = "rrds";
	
	// JSAP related stuff.
	public static String JSAP_PROGRAM_NAME = "java gov.usgs.winston.monitor.Monitor";
	public static String JSAP_EXPLANATION_PREFACE =
			"Winston Monitor\n" +
			"\n" +
			"This program collects data from winston sources and provides plots over HTTP\n" +
			"See 'winstonMonitor.config' for more options.\n" +
			"\n";
	
	private static final String DEFAULT_JSAP_EXPLANATION =
			"All output goes to both standard error and the file log.\n" +
			"\n" +
			"While the process is running (and accepting console input) you can enter\n" +
			"these commands into the console (followed by [Enter]):\n" +
			"0: turn logging off.\n" +
			"1: normal logging level (WARNING).\n" +
			"2: high logging level (FINE).\n" +
			"3: log everything.\n" +
			"i: no longer accept console input.\n" +
			"q: quit cleanly.\n" +
			"ctrl-c: quit now.\n" +
			"\n" +
			"Note that if console input is disabled the only way to\n" +
			"terminate the program is with ctrl-c or by killing the process.\n";
	
	private static final Parameter[] DEFAULT_JSAP_PARAMETERS = new Parameter[] {
		new FlaggedOption("logLevel", JSAP.STRING_PARSER, JSAP.NO_DEFAULT,
				JSAP.NOT_REQUIRED, 'l', "log-level",
				"The level of logging to start with\n" +
				"This may consist of either a java.util.logging.Level name or an integer value.\n" +
				"For example: \"SEVERE\", or \"1000\""),
		new Switch("logoff", '0', "logoff", "Turn logging off (equivalent to --log-level OFF)."),
		new Switch("lognormal", '1', "lognormal", "Normal (default) logging level (equivalent to --log-level FINE)."),
		new Switch("loghigh", '2', "loghigh", "High logging level (equivalent to --log-level ALL)."),
		new Switch("noinput", 'i', "noinput", "Don't accept input from the console."),
		new UnflaggedOption("configFilename", JSAP.STRING_PARSER,
				JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, JSAP.NOT_GREEDY,
				"The config file name.")
	};
	
	
	protected String configFilename;
	protected ConfigFile config;
	
	protected ImportGeneric importGeneric;

	protected final Logger logger;
	protected String logFile;
	protected int logNumFiles;
	protected int logSize;
	private MonitorServer server;
	private ArrayList<Collector> collectors;

	public Monitor(String fn)
	{
		this();
		
		configFilename = Util.stringToString(fn, DEFAULT_CONFIG_FILENAME);
		System.out.println(String.format("Reading config file %s in directory %s", configFilename, System.getProperty("user.dir")));
		
		config = new ConfigFile(configFilename);
		processConfigFile();
		
	}

	/**
	 * Sets up all the standard private member variables, the logger and
	 * instantiates a ConfigFile using configFilename.
	 */
	public Monitor()
	{
		setName("Monitor");
		
		logger = Log.getLogger("gov.usgs.winston");
		logger.setLevel(Level.parse(DEFAULT_LOG_LEVEL));
	}
	
	/**
	 * Processes logger, import and winston config, plus options, default options
	 * and filters.
	 */
	protected void processConfigFile()
	{
		processLoggerConfig();
		processMonitorConfig();
		processCollectorConfig();
		processServerConfig();
	}

	private void processServerConfig()
	{
		server = new MonitorServer(config.getSubConfig("server"), collectors);
	    new Thread(server).start();
	}
	
	private void processCollectorConfig()
	{
		
		collectors = new ArrayList<Collector>();
		ConfigFile wc = config.getSubConfig("winston");

		for (String collectorName : config.getList("collector"))
		{
				ConfigFile cc = config.getSubConfig(collectorName);
				try
				{
					Collector collector = (Collector)Class.forName(cc.getString("class")).newInstance();
					collector.configure(collectorName, cc, wc);
					collectors.add(collector);
				}
				catch (Exception e)
				{
					logger.log(Level.SEVERE, String.format("Could not create %s collector: %s", collectorName, e.getMessage()));
					e.printStackTrace();
				}
		}
		
		for (Collector collector : collectors)
		{
			logger.info("starting collector: " + collector.getName());
			 collector.start();
		}
	}
	/**
	 * Logs a severe message and exits uncleanly.
	 */
	protected void fatalError(String msg)
	{
		logger.severe(msg);
		System.exit(1);
	}


	/**
	 * Extracts logging configuration information.
	 */
	protected void processLoggerConfig()
	{
		logFile = Util.stringToString(config.getString("monitor.log.name"), DEFAULT_LOG_FILE);
		logNumFiles = Util.stringToInt(config.getString("monitor.log.numFiles"), DEFAULT_LOG_NUM_FILES);
		logSize = Util.stringToInt(config.getString("monitor.log.maxSize"), DEFAULT_LOG_FILE_SIZE);

		if (logNumFiles > 0)
			Log.attachFileLogger(logger, logFile, logSize, logNumFiles, true);
		
		String[] version = Util.getVersion("gov.usgs.winston");
		if (version != null)
			logger.info("Version: " + version[0] + " Built: " + version[1]);
		else
			logger.info("No version information available.");
		
		logger.info("config: monitor.log.name=" + logFile);
		logger.info("config: monitor.log.numFiles=" + logNumFiles);
		logger.info("config: monitor.log.maxSize=" + logSize);
	}

	/**
	 * Extracts generalised configuration information.
	 */
	protected void processMonitorConfig()
	{
		int port = Util.stringToInt(config.getString("monitor.port"), DEFAULT_PORT);
		logger.info("config: monitor.port=" + port);
	}

	public void run()
	{
	}
	
	public void quit()
	{
		System.out.println("Quitting cleanly...");
		for (Collector c : collectors)
		{
			System.out.print("Stopping collector " + c.getName() + "... ");
			c.quit();
			System.out.println(" done.");
		}
	}
	
	public void printStatus()
	{
	}
	
	/**
	 * Find and parse the command line arguments.
	 *
	 * @param args The command line arguments.
	 */
	public static JSAPResult getArguments(String[] args) {
		JSAPResult config = null;
		try
		{
			SimpleJSAP jsap = new SimpleJSAP(
					JSAP_PROGRAM_NAME,
					JSAP_EXPLANATION_PREFACE + DEFAULT_JSAP_EXPLANATION,
					DEFAULT_JSAP_PARAMETERS
					);
			
			config = jsap.parse(args);
			
			if (jsap.messagePrinted())
			{
				// The following error message is useful for catching the case
				// when args are missing, but help isn't printed.
				if (!config.getBoolean("help"))
				{
					System.err.println("Try using the --help flag.");
				}
				System.exit(1);
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			System.exit(1);
		}
		return config;
	}
	
	public void setLogLevel(Level level)
	{
		if (level.equals(Level.ALL))
			logger.fine("Logging set to high.");
		else if (level.equals(Level.FINE))
			logger.fine("Logging set to normal.");
		else if (level.equals(Level.OFF))
			logger.fine("Logging turned off.");
		logger.setLevel(level);
	}

	/**
	 * Manage the console input.
	 *
	 * @param im The Winston importer.
	 */
	public static void consoleInputManager(Monitor im)
	{
		im.logger.entering(im.getClass().getName(), "consoleInputManager");
		boolean acceptCommands = true;
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		while (acceptCommands)
		{
			try
			{
				String s = null;
				try
				{
					s = in.readLine();
				}
				catch (IOException ioex)
				{
					im.logger.log(Level.SEVERE, "IOException encountered while attempting to read console input.", ioex);
				}
				
				if (s != null)
				{
					s = s.toLowerCase().trim();
					if (s.equals("q"))
					{
						im.quit();
						try
						{
							im.join();
						}
						catch (Throwable e)
						{
							im.logger.log(Level.SEVERE, "Failed to quit cleanly.", e);
						}
						finally
						{
							im.printStatus();
						}
						System.exit(0);
					}
					else if (s.equals("0"))
						im.setLogLevel(Level.OFF);
					else if (s.equals("1"))
						im.setLogLevel(Level.FINE);
					else if (s.equals("2"))
						im.setLogLevel(Level.ALL);
				}
			}
			catch (OutOfMemoryError e)
			{
			}
		}
		im.logger.exiting(im.getClass().getName(), "consoleInputManager");
	}
	
	public static void main(String[] args)
	{
		JSAPResult config = getArguments(args);

		String fn = Util.stringToString(
				config.getString("configFilename"), 
				DEFAULT_CONFIG_FILENAME
				);

		Level logLevel = Level.parse(DEFAULT_LOG_LEVEL);

		if (config.getString("logLevel") != null)
		{
			try
			{
				logLevel = Level.parse(config.getString("logLevel"));
			}
			catch (IllegalArgumentException ex)
			{
				System.err.println("Invalid log level: " + config.getString("logLevel"));
				System.err.println("Using default log level: " + logLevel);
			}
		}
		else
		{
			if (config.getBoolean("logoff"))
				logLevel = Level.OFF;

			if (config.getBoolean("lognormal"))
				logLevel = Level.FINE;

			if (config.getBoolean("loghigh"))
				logLevel = Level.ALL;
		}


		Monitor mon = new Monitor(fn);
		
		// Start the importer.
		
		// Decide if we're accepting commands based on the args parsed by jsap
		// and pass the importer to the consoleInputManager if we are.
		if (!(config.getBoolean("noinput")))
			consoleInputManager(mon);
	}
}

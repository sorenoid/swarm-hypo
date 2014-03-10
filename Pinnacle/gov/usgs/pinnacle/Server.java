package gov.usgs.pinnacle;

import gov.usgs.net.BroadcastServer;
import gov.usgs.util.ConfigFile;
import gov.usgs.util.Log;
import gov.usgs.util.Util;

/**
 * <p>Communication server. It listens for network clients on a dedicated port. Should be placed near 
 * pinnacle device to communicate with it via it's serial port. The server translates data between clients and the device.</p>
 * 
 * $Log: not supported by cvs2svn $
 * Revision 1.3  2005/10/13 23:01:15  dcervelli
 * Renamed log files and changed default values.
 *
 * Revision 1.2  2005/10/13 21:01:16  dcervelli
 * Log file changes.
 *
 * Revision 1.1  2005/09/20 18:22:06  dcervelli
 * Initial commit.
 *
 * @author Dan Cervelli
 * @version $Id: Server.java,v 1.4 2007-04-25 21:01:26 dcervelli Exp $ 
 */
public class Server extends BroadcastServer
{
	private static final String CONFIG_FILE = "PinnServer.config";
	private static final String DEFAULT_LOG_FILE = "PinnServer.log";
	private static final int DEFAULT_LOG_NUM_FILES = 100;
	private static final int DEFAULT_LOG_FILE_SIZE = 10000000;
	private Manager pinnMan;
	private int interval;
	private int sampleRate;
	private Thread broadcaster;
	private int clockReset;
	private int gain;
	
	private String logFile;
	private int logNumFiles;
	private int logSize;
	
	/**
	 * <p>Constructor</p>
	 * @param n server name
	 * @param p port number to listen
	 */
	public Server(String n, int p)
	{
		super(n, p);
	}
	
	/**
	 * Factory method to produce Server
	 * @return Server configured as described in 'PinnServer.config' configuration file
	 */
	public static Server createPinnServer()
	{
		ConfigFile cf = new ConfigFile(CONFIG_FILE);
		
		int port = Util.stringToInt(cf.getString("server.port"), 17000);
		Server ps = new Server("PinnServer", port);
		ps.logFile = Util.stringToString(cf.getString("log.name"), DEFAULT_LOG_FILE);
		ps.logNumFiles = Util.stringToInt(cf.getString("log.numFiles"), DEFAULT_LOG_NUM_FILES);
		ps.logSize = Util.stringToInt(cf.getString("log.maxSize"), DEFAULT_LOG_FILE_SIZE);

		if (ps.logNumFiles > 0)
			Log.attachFileLogger(ps.logger, ps.logFile, ps.logSize, ps.logNumFiles, true);
		
		String com = Util.stringToString(cf.getString("pinn.com"), "COM1");
		int sn = Util.stringToInt(cf.getString("pinn.serialNumber"), -1);
		if (sn == -1)
		{
			System.err.println("pinn.serialNumber must be in the configuration file.");
			System.exit(1);
		}
		ps.gain = Util.stringToInt(cf.getString("pinn.gain"), 1);
		ps.interval = Util.stringToInt(cf.getString("server.interval"), 1);
		ps.sampleRate = ps.interval;
		ps.interval *= 1000;
		int to = Util.stringToInt(cf.getString("pinn.timeout"), 10000);
		ps.clockReset = Util.stringToInt(cf.getString("server.clockReset"), 600);
		ps.pinnMan = new Manager(com, 9600, sn, to);
		return ps;
	}
	
	/**
	 * <p>Connect to Pinnacle device via serial port, check and maintain connection.</p>
	 */
	public void startBroadcasting()
	{
		broadcaster  = new Thread(new Runnable()
				{
					public void run()
					{
						pinnMan.openPort();
						boolean needStart = true;
						int count = 0;
						while (true)
						{
							try
							{
								if (needStart)
								{
									pinnMan.flushInput();
									logger.finer("starting data acquisition.");
									pinnMan.startDataAcquisition();
									logger.finer("setting gain.");
									pinnMan.setGain(gain);
									logger.finer("setting sample rate.");
									pinnMan.setSampleRate(sampleRate);
									count = 0;
								}
								
								needStart = false;
								if (count++ % clockReset == 0)
								{
									logger.finer("setting clock.");
									pinnMan.setClock();
								}
								long st = System.currentTimeMillis();
								StatusBlock sb = pinnMan.getStatus();
								long et = System.currentTimeMillis();
								if (et - st < interval)
									try { Thread.sleep(interval - (et - st)); } catch (Exception e) {}
									
								String bs = Util.bytesToHex(sb.getBuffer());
								String msg = "SB: " + bs;
								logger.finer(msg);
								sendMessage(msg + "\n");
							}
							catch (PinnacleException pe)
							{
								logger.info(pe.getMessage());
								logger.info("attempting reconnect.");
								pinnMan.close();
								pinnMan.openPort();
								needStart = true;
							}
						}
					}
				});
		
		broadcaster.start();
	}

	/**
	 * <p>Main method</p>
	 * <p>Initialize server, configure it, connect to device and start network port listening</p>
	 * @param args no args really
	 */
	public static void main(String[] args)
	{
		Server ps = createPinnServer();
		ps.startBroadcasting();
		ps.startListening();
	}
	
}

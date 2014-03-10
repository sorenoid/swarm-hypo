package gov.usgs.netRSFileMover;

import gov.usgs.util.ConfigFile;
import gov.usgs.util.Log;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Retrieve files from a Trimble NetRS device via FTP
 * 
 * TODO: add option to resume file transfers
 * 
 * @author Tom Parker
 */
public final class NetRSFileMover {
	public static final boolean DEFAULT_BACKFILL_FIRST = true;
	public static final int DEFAULT_MAX_RUNTIME = 60 * 60 * 24;
	public static final int ONE_DAY = 1000 * 60 * 60 * 24;

	private final static Logger LOGGER = Log.getLogger(NetRSFileMover.class
			.getName());

	private List<NetRSConnection> receivers;

	/**
	 * simple constructor
	 * 
	 * @param configFile
	 */
	public NetRSFileMover(ConfigFile configFile) {

		List<String> receiverList = configFile.getList("receiver");
		if (receiverList == null || receiverList.size() == 0) {
			System.err
					.println("I didn't find any receiver directives. Guess I'm done.");
			System.exit(1);
		}

		receivers = new LinkedList<NetRSConnection>();
		for (String receiverName : configFile.getList("receiver")) {

			NetRSSettings settings = new NetRSSettings(receiverName,
					configFile.getSubConfig(receiverName, true));

			NetRSConnection connection = new NetRSConnection(settings);
			receivers.add(connection);
		}
	}

	/**
	 * Do the work
	 */
	private void go() {
		while (!receivers.isEmpty()) {
			Iterator<NetRSConnection> it = receivers.iterator();

			while (it.hasNext()) {
				NetRSConnection receiver = it.next();
				receiver.poll();

				if (receiver.polledLast())
					it.remove();
			}
		}
	}

	/**
	 * Main method. Always a good place to start.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		Log.getLogger("gov.usgs.netRSFileMover");
		LogManager.getLogManager().getLogger("gov.usgs.netRSFileMover").setLevel(Level.FINEST);
		
		if (args.length != 1) {
			System.err.println("Usage: NetRSFileMover <config>");
			System.exit(1);
		}

		ConfigFile cf = new ConfigFile(args[0]);
		if (!cf.wasSuccessfullyRead()) {
			System.err.print("Can't read config file " + args[0]);
			System.exit(1);
		}
		
		if (cf.getList("debug") != null)
		{
			for (String name : cf.getList("debug")) {
				Logger l = Log.getLogger(name);
				l.setLevel(Level.ALL);
				LOGGER.fine("debugging " + name);
			}
		}
		
		NetRSFileMover arch = new NetRSFileMover(cf);
		arch.go();

		LOGGER.info("Got everything I'm going to get. Exiting.");
	}
}

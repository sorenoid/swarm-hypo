package gov.usgs.winston.monitor.server;

import gov.usgs.net.CommandHandler;
import gov.usgs.net.NetTools;
import gov.usgs.winston.monitor.MonitorServer;

import java.nio.channels.SocketChannel;

/**
 * A class that handles Monitor requests.
 *
 * @author Tom Parker
 */
public class ServerHandler extends CommandHandler
{
	private static final int PROTOCOL_VERSION = 3;
	private static int instances = 0;
	private NetTools netTools;
	private MonitorServer mon;
	
	public ServerHandler(MonitorServer s)
	{
		super(s, "MonitorServerHandler-" + instances++);
		
		mon = s;
		netTools = new NetTools();
		netTools.setServer(mon);

		setupCommandHandlers();
	}

	protected void setupCommandHandlers()
	{
		addCommand("VERSION", new BaseCommand(netTools, mon)
				{
					public void doCommand(Object info, SocketChannel channel)
					{
						netTools.writeString("PROTOCOL_VERSION: " + PROTOCOL_VERSION + "\n", channel);
					}
				});
		addCommand("GET", new HttpGetCommand(netTools, mon, this));
	}
}

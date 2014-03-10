package gov.usgs.net;

import gov.usgs.util.CodeTimer;
import gov.usgs.util.Log;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A class that handles server requests on a separate thread. 
 * 
 * @author Dan Cervelli
 */
abstract public class CommandHandler extends Thread
{
	protected static final int COMMAND_BUFFER_SIZE = 2048;
	protected Server server;

	protected Map<String, Command> commands;
	protected Command executeCommand;
	protected String executeCommandInfo;
	protected SocketChannel channel;
	protected SelectionKey selectionKey;
	protected int slowCommandTime;
	
	protected Logger logger;
	
	public CommandHandler(Server svr, String name)
	{
		logger = Log.getLogger("gov.usgs.net");
		server = svr;
		commands = new HashMap<String, Command>();
		slowCommandTime = 0;
		
		this.setName(name);
		this.start();
	}
	
	abstract protected void setupCommandHandlers();
	
	protected void addCommand(String id, Command cmd)
	{
		commands.put(id, cmd);
	}
	
	protected Command getCommand(String cmd)
	{
		Command command = commands.get(cmd);
		return command;
	}
	
	protected void doCommand(SocketChannel ch, SelectionKey key, String cmd)
	{
		// logger.log(Level.INFO, "Command: " + cmd);
		channel = ch;
		selectionKey = key;
		String cmdName = "";
		int indexSpace = cmd.indexOf(' ');
		int indexColon = cmd.indexOf(':');
		if (indexSpace == -1 && indexColon == -1)
		{
			cmdName = cmd.trim();
		}
		else
		{
			
			int index = -1;
			if (indexSpace == -1 || indexColon == -1)
			{
				index = indexSpace;
				if (index == -1)
					index = indexColon;
			}
			else
				index = Math.min(indexSpace, indexColon);
			
			if (index == -1)
				index = indexColon;
			cmdName = cmd.substring(0, index);
		}
		
		Command command = getCommand(cmdName);
		if (command != null)
		{
			executeCommand = command;
			executeCommandInfo = cmd;
		} 
		// quit is special, it has no Command class.
		else if (cmdName.equalsIgnoreCase("quit"))
		{
			closeConnection();
		}
		interrupt();
	}
	
	public void closeConnection()
	{
		server.closeConnection(channel, selectionKey);
	}
	
	public void run()
	{
		while (true)
		{
			boolean interrupted = false;
			try	{ Thread.sleep(3600000); } 
			catch (InterruptedException e) 
			{
				interrupted = true;
			}
			if (interrupted)
			{
				try
				{
					if (executeCommand != null) {
						CodeTimer ct = new CodeTimer(executeCommandInfo);
						executeCommand.doCommand(executeCommandInfo, channel);
						ct.stop();
						if (slowCommandTime > 0 && ct.getRunTimeMillis() > slowCommandTime)
							logger.log(Level.INFO, String.format(Server.getHost(channel) + "/slow command (%1.2f ms) " + executeCommandInfo, ct.getRunTimeMillis()));
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
					logger.log(Level.SEVERE, "Unhandled exception in main CommandHandler loop.", e);
				}
				finally
				{
					executeCommand = null;
					server.addCommandHandler(this);
				}
			}
		}
	}
}

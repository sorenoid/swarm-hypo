package gov.usgs.net;

import gov.usgs.util.Log;
import gov.usgs.util.Util;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * @author Dan Cervelli
 */
public class BroadcastServer
{
	protected String name = "Server";
	protected int port = -1;
	protected int maxConnections = 20;
	protected int connections = 0;
	protected Logger logger;
	protected NetTools netTools;
	
	protected List<SocketChannel> currentConnections;
	
	public BroadcastServer(String n, int p)
	{
		name = n;
		port = p;
		currentConnections = Collections.synchronizedList(new ArrayList<SocketChannel>());
		logger = Log.getLogger("gov.usgs.net");
		netTools = new NetTools();
	}
	
	public static String getHost(SocketChannel channel)
	{
		if (channel == null || channel.socket() == null || channel.socket().getInetAddress() == null)
			return "(closed)";
		
		return channel.socket().getInetAddress().getHostAddress();
	}
	
	public void log(Level level, String msg, SocketChannel channel)
	{
		String channelString = (channel == null ? "" : getHost(channel) + "/");
		String logMsg = channelString + msg;
		logger.log(level, logMsg);
	}
	
	protected void closeConnection(SocketChannel channel, SelectionKey selectionKey)
	{
		try
		{
			currentConnections.remove(channel);
			channel.close();
			selectionKey.cancel();
			selectionKey.selector().wakeup(); // what does this do?
			selectionKey.attach(null);
			connectionClosed(channel);
		}
		catch (Exception e)
		{}
	}
	
	public void connectionClosed(SocketChannel channel)
	{
		connections--;
		log(Level.FINE, "Connection closed: " + connections + "/" + maxConnections + ".", channel);
	}
	
	public void sendMessage(String msg)
	{
		
		synchronized (currentConnections)
		{
			for (SocketChannel ch : currentConnections)
				netTools.writeString(msg, ch);
		}
	}
	
	public void sendBinary(byte[] buf)
	{
		logger.log(Level.FINER, "Broadcast: " + Util.bytesToHex(buf));
		synchronized (currentConnections)
		{
			for (SocketChannel ch : currentConnections)
				netTools.writeByteBuffer(ByteBuffer.wrap(buf), ch);
		}
	}
	
	public void startListening()
	{
		if (port == -1)
			return;
		
		try 
		{
			Selector selector = Selector.open();
			
			ServerSocketChannel serverChannel = ServerSocketChannel.open();
			serverChannel.configureBlocking(false);
			serverChannel.socket().bind(new InetSocketAddress(port));
			serverChannel.register(selector, SelectionKey.OP_ACCEPT);
			
			logger.info("listening on port " + port + ".");
			
			while (true)
			{
				selector.select();
				
				Iterator<SelectionKey> it = selector.selectedKeys().iterator();
				
				while (it.hasNext()) 
				{
					SelectionKey selKey = (SelectionKey)it.next();
					it.remove();
					
					if (selKey.isAcceptable()) 
					{
						ServerSocketChannel ssChannel = (ServerSocketChannel)selKey.channel();
						SocketChannel channel = ssChannel.accept();
						connections++;
						if (maxConnections != 0 && connections > maxConnections)
						{
							channel.close();
							connections--;
							continue;
						}
						log(Level.FINE, "Connection accepted: " + connections + "/" + maxConnections + ".", channel);
						channel.configureBlocking(false);
						channel.register(selector, SelectionKey.OP_READ);
						currentConnections.add(channel);
					}
					
					if (selKey.isReadable())
					{
						// close channel on any read
						SocketChannel channel = (SocketChannel)selKey.channel();
						closeConnection(channel, selKey);
			        }
			    }
			}
	    } 
		catch (IOException e) 
		{
			logger.log(Level.SEVERE, "Fatal exception.", e);
	    }
	}
	
	public static void main(String[] args)
	{
		final BroadcastServer bs = new BroadcastServer("Random number server", 16000);
		Thread t = new Thread(new Runnable()
				{
					public void run()
					{
						while (true)
						{
							bs.sendMessage("Random: " + Math.random() + "\n");
							try { Thread.sleep(1000); } catch (Exception e) {}
						}
					}
				});
		t.start();
		bs.startListening();
	}
}

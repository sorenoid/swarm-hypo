package gov.usgs.net;

import gov.usgs.util.CodeTimer;
import gov.usgs.util.Log;
import gov.usgs.util.Pool;
import gov.usgs.util.Util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The base class for implementing a Java NIO-based server.
 * 
 * @author Dan Cervelli
 */
public class Server {
	protected ByteBuffer inBuffer = ByteBuffer.allocate(65536);
	protected NetTools netTools = new NetTools();
	protected static final int COMMAND_BUFFER_SIZE = 2048;

	protected DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	protected String name = "Server";
	protected int serverPort = -1;
	protected InetAddress serverIP = null;
	protected boolean keepalive = false;
	protected int maxConnections = 20;

	protected long connectionIndex = 0;

	private Pool<CommandHandler> commandHandlerPool;

	protected Logger logger;
	protected int maxReadHandlers = -1;

	protected Map<SocketChannel, ConnectionStatistics> connectionStats;

	protected boolean dropOldest = true;

	protected long totalBytesSent = 0;

	protected Server() {
		connectionStats = Collections.synchronizedMap(new HashMap<SocketChannel, ConnectionStatistics>());
		logger = Log.getLogger("gov.usgs.net");
		commandHandlerPool = new Pool<CommandHandler>();
	}

	protected Server(InetAddress a, int p) {
		this();
		serverIP = a;
		serverPort = p;
	}

	protected void addCommandHandler(CommandHandler rh) {
		commandHandlerPool.checkin(rh);
		int max = Math.max(maxReadHandlers, commandHandlerPool.size());
		if (max > maxReadHandlers)
			logger.log(Level.FINE, "command handler pool size: " + max);
		maxReadHandlers = max;
	}

	public int getNumConnections() {
		return connectionStats.size();
	}

	public Collection<ConnectionStatistics> getConnectionStats() {
		return connectionStats.values();
	}

	public int getPoolSize() {
		return commandHandlerPool.size();
	}

	public static String getHost(SocketChannel channel) {
		String addr;

		// avoid a check-then-act error, by catching the NullPointerException
		// rather than checking for null
		try {
			addr = channel.socket().getInetAddress().getHostAddress();
		} catch (NullPointerException e) {
			addr = "(closed)";
		}

		return addr;
	}

	public void log(Level level, String msg, SocketChannel channel) {
		String channelString = (channel == null ? "" : getHost(channel) + "/");
		String logMsg = channelString + msg;
		logger.log(level, logMsg);
	}

	protected void closeConnection(SocketChannel channel, SelectionKey selectionKey) {
		try {
			connectionStats.remove(channel);
			if (channel != null && channel.isOpen())
				channel.close();
			if (selectionKey != null) {
				selectionKey.cancel();
				selectionKey.selector().wakeup(); // what does this do?
				selectionKey.attach(null);
			}
			log(Level.FINER, String.format("Connection closed: %d/%d.", getNumConnections(), maxConnections), channel);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void dispatchCommand(SocketChannel channel, SelectionKey key, String s) {
		CodeTimer ct = new CodeTimer("getReadHandler");
		CommandHandler ch = commandHandlerPool.checkout();
		ct.stop();
		if (ct.getRunTimeMillis() > 1000)
			log(Level.FINE, String.format("long wait for read handler: %1.2f ms. ", ct.getRunTimeMillis()), channel);
		ch.doCommand(channel, key, s);
	}

	public ConnectionStatistics getConnectionStatistics(SocketChannel channel) {
		ConnectionStatistics cs = connectionStats.get(channel);
		if (cs == null) {
			cs = new ConnectionStatistics(channel);
			cs.address = getHost(channel);
			cs.index = connectionIndex++;
			cs.connectTime = System.currentTimeMillis();
			connectionStats.put(channel, cs);
		}
		return cs;
	}

	public void recordSent(SocketChannel channel, int nb) {
		totalBytesSent += nb;
		ConnectionStatistics cs = connectionStats.get(channel);
		if (cs != null)
			cs.sent(nb);
	}

	public void processRead(SelectionKey selectionKey) {
		ByteBuffer commandBuffer;
		SocketChannel channel = (SocketChannel) selectionKey.channel();
		if (!channel.isConnected() || !channel.isOpen())
			return;

		ConnectionStatistics cs = getConnectionStatistics(channel);

		Object attachment = selectionKey.attachment();
		if (attachment != null)
			commandBuffer = (ByteBuffer) attachment;
		else
			commandBuffer = ByteBuffer.allocate(COMMAND_BUFFER_SIZE);

		boolean close = false;
		try {
			inBuffer.clear();
			int i = channel.read(inBuffer);
			if (i == -1)
				close = true;
			else {
				cs.read(i);
				inBuffer.flip();
				while (inBuffer.position() < inBuffer.limit()) {
					byte b = inBuffer.get();
					if (b == '\n') {
						commandBuffer.flip();
						CharBuffer cb = netTools.decoder.decode(commandBuffer);
						dispatchCommand(channel, selectionKey, cb.toString());
						commandBuffer.clear();
					} else
						commandBuffer.put(b);
				}
				if (commandBuffer.position() != 0)
					selectionKey.attach(commandBuffer);
				else
					selectionKey.attach(null);
			}
		} catch (IOException e) {
			close = true;
		} catch (BufferOverflowException e) {
			logger.log(Level.SEVERE, "Buffer overflow on read.  Possible malicious attack?");
			close = true;
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Unhandled exception.", e);
			close = true;
		}
		if (close)
			closeConnection(channel, selectionKey);
	}

	public void printConnections(String s) {
		char col = 'T';
		if (s.length() > 1)
			col = s.charAt(1);
		boolean desc = s.endsWith("-");

		List<ConnectionStatistics> css = new ArrayList<ConnectionStatistics>(connectionStats.size());
		css.addAll(connectionStats.values());

		Collections.sort(css, ConnectionStatistics.getComparator(ConnectionStatistics.SortOrder.parse(col), desc));
		StringBuffer sb = new StringBuffer();
		sb.append("------- Connections --------\n");
		sb.append(ConnectionStatistics.getHeaderString());
		for (ConnectionStatistics cs : css) {
			sb.append(cs.toString() + "\n");
		}
		sb.append(ConnectionStatistics.getHeaderString());
		sb.append("Open client connections:    " + connectionStats.size() + "/" + maxConnections + "\n");
		sb.append("Available command handlers: " + commandHandlerPool.size() + "/" + maxReadHandlers + "\n");
		sb.append("Total bytes sent:           " + Util.numBytesToString(totalBytesSent) + "\n");

		System.out.println(sb);
	}

	public void dropConnections() {
		dropConnections(0);
	}

	public void dropConnections(long idleLimit) {
		logger.info("Dropping connections.");
		List<SocketChannel> channels = new ArrayList<SocketChannel>(connectionStats.size());
		// must move current connections to a list so that they can be dropped
		for (SocketChannel sc : connectionStats.keySet()) {
			ConnectionStatistics cs = connectionStats.get(sc);
			if (System.currentTimeMillis() - cs.lastRequestTime > idleLimit)
				channels.add(sc);
		}

		for (SocketChannel sc : channels)
			closeConnection(sc, null);
	}

	public void dropOldestConnection() {
		long least = Long.MAX_VALUE;
		SocketChannel lc = null;
		for (ConnectionStatistics cs : connectionStats.values()) {
			if (cs.lastRequestTime < least) {
				least = cs.lastRequestTime;
				lc = cs.channel;
			}
		}
		if (lc != null)
			closeConnection(lc, null);
	}

	protected void startListening() {
		if (commandHandlerPool.size() <= 0 || serverPort == -1)
			return;

		try {
			Selector selector = Selector.open();

			ServerSocketChannel serverChannel = ServerSocketChannel.open();
			serverChannel.configureBlocking(false);
			if (serverIP == null) {
				serverChannel.socket().bind(new InetSocketAddress(serverPort));
				logger.info("listening on IP *.");
			} else {
				serverChannel.socket().bind(new InetSocketAddress(serverIP, serverPort));
				logger.info("listening on IP " + serverIP.getHostAddress() + ".");
			}

			serverChannel.register(selector, SelectionKey.OP_ACCEPT);

			logger.info("listening on port " + serverPort + ".");

			while (true) {
				selector.select();

				// TODO: figure out why this threw a nullPointer exception in
				// production
				Set<SelectionKey> sk = selector.selectedKeys();
				Iterator<SelectionKey> it = sk.iterator();

				while (it.hasNext()) {
					// it is actually possible that the key will go invalid
					// between the call to isValid and isAcceptable/isReadable
					// which will
					// then throw the CancelledKeyException
					try {
						SelectionKey selKey = (SelectionKey) it.next();
						it.remove();
						if (!selKey.isValid())
							continue;

						if (selKey.isAcceptable()) {
							ServerSocketChannel ssChannel = (ServerSocketChannel) selKey.channel();

							// Why are wee seeing null pointers here?
							SocketChannel channel = ssChannel.accept();
							if (channel == null) {
								System.err.println("channel is null in net.Server.startListening.");
								continue;
							}
							Socket socket = channel.socket();

							if (socket == null) {
								System.err.println("socket is null in net.Server.startListening.");
								continue;
							}
							socket.setKeepAlive(keepalive);
							socket.setTcpNoDelay(true);

							ConnectionStatistics cs = getConnectionStatistics(channel);
							cs.touch();
							if (maxConnections != 0 && getNumConnections() > maxConnections) {
								if (dropOldest) {
									logger.severe("Max connections reached, dropped least recently used connection.");
									dropOldestConnection();
								} else {
									logger.severe("Max connections reached, rejected connection.");
									channel.close();
									// DO NOT PASS selKey to closeConnection, it
									// will break the server.
									closeConnection(channel, null);
									continue;
								}
							}
							log(Level.FINER,
									String.format("Connection accepted: %d/%d", getNumConnections(), maxConnections),
									channel);
							channel.configureBlocking(false);
							channel.register(selector, SelectionKey.OP_READ);
						}

						if (selKey.isValid() && selKey.isReadable()) {
							SocketChannel channel = (SocketChannel) selKey.channel();
							if (channel.isOpen() && channel.isConnected()) {
								ConnectionStatistics cs = getConnectionStatistics(channel);
								cs.touch();
								processRead(selKey);
							}
						}
					} catch (CancelledKeyException e) {
					}
				}
			}
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Fatal exception.", e);
		}
	}
}

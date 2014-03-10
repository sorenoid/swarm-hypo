package gov.usgs.net;

import gov.usgs.util.Log;
import gov.usgs.util.Retriable;
import gov.usgs.util.UtilException;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.logging.Logger;

/**
 * TODO: make all read operations into a unified byte buffer.
 *
 * @author Dan Cervelli
 */
public class InternetClient
{
	public final String host;
	public final int port;

	protected Socket socket;
	protected PrintWriter socketOut;
	protected DataInputStream socketIn;

	protected int timeout = 15000;
	protected int maxRetries = 3;
	
	protected final static Logger logger = Log.getLogger("gov.usgs.net.InternetClient");

	public InternetClient(String sp)
	{
		String[] hp = sp.split(":");
		host = hp[0];
		port = Integer.parseInt(hp[1]);
	}

	public InternetClient(String s, int p)
	{
		host = s;
		port = p;
	}

	public void setMaxRetries(int mr)
	{
		maxRetries = mr;
	}

	public void setTimeout(int ms)
	{
		timeout = ms;
	}

	public boolean connected()
	{
		if (socket == null)
			return false;

		if (socket.isClosed())
			return false;

		if (socketOut == null || socketIn == null)
			return false;

		return true;
	}

	public void writeString(final String msg)
	{
		socketOut.print(msg);
		socketOut.flush();
	}
	
	public String readString() throws IOException
	{
		byte[] bb = new byte[256];
		byte b;
		int i = 0;
		while ((b = socketIn.readByte()) != '\n')
		{
			if (i == bb.length)
			{
				byte[] obb = bb;
				bb = new byte[obb.length * 2];
				System.arraycopy(obb, 0, bb, 0, obb.length);
			}
			bb[i++] = b;
		}
		return new String(bb, 0, i);
	}
	
	public byte[] readBinary(final int bytes) throws IOException
	{
		return readBinary(bytes, null);
	}
	
	public byte[] readBinary(final int bytes, final ReadListener listener) throws IOException
	{
		byte[] buf = new byte[bytes];
		int read = 0;
		while (read < bytes)
		{
			read += socketIn.read(buf, read, bytes - read);
			if (listener != null)
				listener.readProgress(read / (double)bytes);
		}
		
		return buf;
	}
	
	public boolean connect()
	{	
		Boolean b = null;
		Retriable<Boolean> result = new Retriable<Boolean>(this + "/connect()", maxRetries)
				{
					public boolean attempt() throws UtilException
					{
						try
						{
							socket = new Socket();
							socket.connect(new InetSocketAddress(host, port), timeout);
							socket.setSoLinger(false, 0);
							socket.setSoTimeout(timeout);
							socketOut = new PrintWriter(socket.getOutputStream());
							BufferedInputStream bis = new BufferedInputStream(socket.getInputStream());
							socketIn = new DataInputStream(bis);
							result = new Boolean(true);
							logger.finer(InternetClient.this + "/connection opened.");
							return true;
						}
						catch (SocketTimeoutException e)
						{
							logger.warning(InternetClient.this + "/connect() timeout: " + e.getMessage());
						}
						catch (IOException e)
						{
							logger.warning(InternetClient.this + "/connect() IOException: " + e.getMessage());
						}
						return false;
					}
				};

		try{
			b=result.go();
		}
		catch(UtilException e){
			//Do nothing 
		}
		return b != null && b.booleanValue();
	}

	public void close()
	{
		if (!connected())
			return;

		try
		{
			socketOut.close();
			socketIn.close();
			socket.close();
			logger.finer(this + "/connection closed.");
		}
		catch (IOException e)
		{
			logger.warning(this + "/close() IOException: " + e.getMessage());
		}
	}
	
	public String toString()
	{
		return host + ":" + port;
	}
}

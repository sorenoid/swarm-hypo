package gov.usgs.pinnacle;

import gov.usgs.util.Arguments;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

import javax.comm.CommPortIdentifier;
import javax.comm.SerialPort;

/**
 * <p>Class to communicate directly with Pinnacle tilt meter via serial port</p>
 * 
 * $Log: not supported by cvs2svn $
 * Revision 1.1  2005/09/20 18:22:06  dcervelli
 * Initial commit.
 *
 * @author Dan Cervelli
 * @version $Id: Manager.java,v 1.2 2007-04-25 21:01:26 dcervelli Exp $
 */
public class Manager 
{
	private SerialPort serialPort;
	private OutputStream outputStream;
	private InputStream inputStream;
	
	private String port;
	private int baud;
	private int serialNumber;
	private long lastReadTime;
	private int timeout;
	
	/**
	 * <p>Constructor</p>
	 * @param p serial port name
	 * @param b serial port baud rate
	 * @param sn device serial number
	 * @param to serial port communication timeout
	 */
	public Manager(String p, int b, int sn, int to)
	{
		port = p;
		baud = b;
		serialNumber = sn;
		timeout = to;
	}
	
	/**
	 * <p>Initialize serial port with given parameters</p>
	 */
	public void openPort()
	{
		try
		{
			CommPortIdentifier portId = CommPortIdentifier.getPortIdentifier(port);
			serialPort = (SerialPort)portId.open("PinnTech", 2000);
			outputStream = serialPort.getOutputStream();
			inputStream = serialPort.getInputStream();
			serialPort.enableReceiveTimeout(timeout);
			
			serialPort.setSerialPortParams(baud,
					SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
					SerialPort.PARITY_NONE);
		} 
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * <p>Close serial port</p>
	 */
	public void close()
	{
		try
		{
			serialPort.close();
			outputStream.close();
			inputStream.close();
		} 
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * <p>Write array of bytes to device</p>
	 * @return true if success
	 */
	public boolean write(byte[] buf)
	{
		try
		{
			outputStream.write(buf);
			return true;
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		return false;
	}
	
	/**
	 * <p>Read array of bytes from device</p>
	 * @param bytes - number of bytes to read
	 * @return
	 * @throws PinnacleException
	 */
	public byte[] readBinary(int bytes) throws PinnacleException
	{
		try
		{
			byte[] buf = new byte[bytes];
			int total = 0;
			while (total < bytes)
			{
				int nr = inputStream.read(buf, total, bytes - total);
				if (nr == 0)
					throw new PinnacleException("serial read timeout.");
				
				touchLastReadTime();
				total += nr;
			}
			return buf;
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * <p>Flush device's serial port buffer, i.e read waiting bytes to dev/null</p>
	 */
	public void flushInput()
	{
		try
		{
			byte[] buf = new byte[256];
			while (inputStream.available() > 0)
				inputStream.read(buf);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * <p>Send acknowledgment to device</p>
	 * @return true if the command was received by the device
	 * @throws PinnacleException
	 */
	public boolean ack() throws PinnacleException
	{
		if (write(Command.getACK(serialNumber)))
			return readBinary(1) != null;
		
		return false;
	}
	
	/**
	 * <p>Set device clock to current computer time</p>
	 * @return true if the command was received by the device
	 * @throws PinnacleException
	 */
	public boolean setClock() throws PinnacleException
	{
		if (write(Command.getSetClock(serialNumber)))
			return readBinary(1) != null;
		
		return false;
	}
	
	/**
	 * <p>Send rezero command to device</p>
	 * @return true if the command was received by the device
	 * @throws PinnacleException
	 */	
	public boolean rezeroNow() throws PinnacleException
	{
		if (write(Command.getRezeroNow(serialNumber)))
			return readBinary(1) != null;
		
		return false;
	}
	
	/**
	 * <p>Send Start Data Acquisition command to device</p>
	 * @return true if the command was received by the device
	 * @throws PinnacleException
	 */	
	public boolean startDataAcquisition() throws PinnacleException
	{
		if (write(Command.getStartDataAcquisition(serialNumber)))
			return readBinary(1) != null;
		
		return false;
	}
	
	/**
	 * <p>Send status request command to device</p>
	 * @return status block with results
	 * @throws PinnacleException
	 */	
	public StatusBlock getStatus() throws PinnacleException
	{
		write(Command.getStatus(serialNumber));
		byte[] buf = readBinary(32);
		
		int total = 0;
		for (int i = 0; i < buf.length - 1; i++)
			total += buf[i];
		
		total = ((0xff & total)); 
		if (total != buf[31])
			throw new PinnacleException("checksum doesn't match.");
		
		StatusBlock sb = new StatusBlock(buf);
		return sb;
	}
	
	/**
	 * <p>Set ampl gain on device</p>
	 * @param gain gain to set (0,1,2,3)
	 * @return true if the command was received by the device
	 * @throws PinnacleException
	 */	
	public boolean setGain(int gain) throws PinnacleException
	{
		if (write(Command.getSetGain(serialNumber, gain)))
			return readBinary(1) != null;
		
		return false;
	}
	
	/**
	 * <p>Set sample rate on device</p>
	 * @param sr sample rate to set (1-255 sec/sample)
	 * @return true if the command was received by the device
	 * @throws PinnacleException
	 */	
	public boolean setSampleRate(int sr) throws PinnacleException
	{
		if (write(Command.getSetSampleRate(serialNumber, sr)))
			return readBinary(1) != null;
		
		return false;
	}
	
	/**
	 * <p>Do nothing</p>
	 * @return false
	 * @throws PinnacleException
	 */	
	public boolean set9600() throws PinnacleException
	{
		return false;
	}
	
	/**
	 * Get time that data was last read from the device
	 * @return time of last data reading from device
	 */
	public synchronized long getLastReadTime()
	{
		return lastReadTime;
	}
	
	/**
	 * <p>Set last data reading time to current time</p>
	 */
	protected synchronized void touchLastReadTime()
	{
		lastReadTime = System.currentTimeMillis();
	}
	
	/**
	 * <p>Main method. Perform commands using port = "COM1", baud 9600 and device serial number 6266</p>
	 * <p>Syntax is:</p>
	 * <p>Manager options</p>
	 * <p>-a --ack		send acknowledge request and print results</p>
	 * <p>-s --status	send status request and print results</p>
	 * <p>-t --time		set device time to current time</p>
	 * <p>-z --zero		rezero device</p>
	 * @throws PinnacleException
	 */
	public static void main(String[] as) throws PinnacleException
	{
		Set<String> flags = new HashSet<String>();
		flags.add("-3");
		flags.add("--38400");
		flags.add("-9");
		flags.add("--9600");
		flags.add("-a");
		flags.add("--ack");
		flags.add("-s");
		flags.add("--status");
		flags.add("-t");
		flags.add("--time");
		flags.add("-z");
		flags.add("--zero");
		
		Set<String> kvs = new HashSet<String>();
		kvs.add("-p");
		kvs.add("--port");
		
		Arguments args = new Arguments(as, flags, kvs);
		
		String port = "COM1";
		int baud = 9600;
		int serialNumber = 6266;
		
		final Manager pm = new Manager(port, baud, serialNumber, 10000);
		pm.openPort();
		if (args.flagged("-a") || args.flagged("--ack"))
			System.out.println("ack: " + pm.ack());
		if (args.flagged("-s") || args.flagged("--status"))
			System.out.println(pm.getStatus());
		if (args.flagged("-t") || args.flagged("--time"))
			System.out.println(pm.setClock());
		if (args.flagged("-z") || args.flagged("--zero"))
			System.out.println(pm.rezeroNow());
	}
}

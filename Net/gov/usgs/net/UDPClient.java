package gov.usgs.net;

import gov.usgs.util.Util;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

/**
 * Experimental.
 * 
 * $Log: not supported by cvs2svn $
 * @author Dan Cervelli
 */
@Deprecated
public class UDPClient
{
	protected String group;
	protected int port;
	protected MulticastSocket socket;
	
	public UDPClient(String g, int p)
	{
		group = g;
		port = p;
	}
	
	public void connect()
	{
		try
		{
			socket = new MulticastSocket(port);
			socket.joinGroup(InetAddress.getByName(group));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public DatagramPacket receive()
	{
		try
		{
			byte[] buf = new byte[256];
			DatagramPacket packet = new DatagramPacket(buf, buf.length);
			socket.receive(packet);
			return packet;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	public static void main(String[] args)
	{
		UDPClient c = new UDPClient("231.232.233.234", 8888);
		c.connect();
		while (true)
		{
			DatagramPacket dp = c.receive();
			byte[] buf = dp.getData();
			System.out.println(Util.bytesToString(buf));
		}
	}
}

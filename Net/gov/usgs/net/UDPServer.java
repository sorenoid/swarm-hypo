package gov.usgs.net;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

/**
 * Experimental.
 * 
 * $Log: not supported by cvs2svn $
 * @author Dan Cervelli
 */
public class UDPServer
{
	protected int port;
	protected String group;
	protected MulticastSocket socket;
	
	public UDPServer(String g, int p)
	{
		port = p;
	}
	
	public void connect()
	{
		try
		{
			socket = new MulticastSocket(port);
			socket.setTimeToLive(5);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void send(DatagramPacket dp)
	{
		try
		{
			dp.setAddress(InetAddress.getByName(group));
			dp.setPort(port);
			socket.send(dp);
			System.out.println("sending");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) throws Exception
	{
		String group = "231.232.233.234";
		UDPServer s = new UDPServer(group, 8888);
		s.connect();
		while (true)
		{
			String t = "testing";
			byte[] buf = t.getBytes();
			DatagramPacket dp = new DatagramPacket(buf, buf.length, InetAddress.getByName(group), 8888);
			s.socket.send(dp);
//			s.send(dp);
			try { Thread.sleep(1000); } catch (Exception e) {}
		}
	}
}

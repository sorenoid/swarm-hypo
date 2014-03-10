package gov.usgs.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.Iterator;

/**
 * A simple Java NIO based dumb IP terminal.
 * 
 * @author Dan Cervelli
 */
@Deprecated
public class IPClient extends Thread 
{
	
	private static final int BUFFER_SIZE = 65536;
	
	private static String host = "localhost";
	private static int port = 16022;
	protected Socket socket;
	protected SocketChannel socketChannel;
	
	CharsetEncoder encoder = Charset.forName("US-ASCII").newEncoder();
	CharsetDecoder decoder = Charset.forName("US-ASCII").newDecoder();
	
	protected Selector selector;
	
	protected ReadHandler readHandler; 
	
	public IPClient()
	{
		try 
		{
			selector = Selector.open();
			
			socketChannel = SocketChannel.open(new InetSocketAddress(host, port));
			socketChannel.configureBlocking(false);
//            SelectionKey readKey = socketChannel.register(selector, SelectionKey.OP_READ);
            socketChannel.register(selector, SelectionKey.OP_READ);
            
            readHandler = new ReadHandler()
            		{
		            	ByteBuffer rbb = ByteBuffer.allocate(BUFFER_SIZE);
		    			CharBuffer rcb = CharBuffer.allocate(BUFFER_SIZE);
		    			
            			public void processRead(SelectionKey key)
            			{
            				SocketChannel socketChannel = (SocketChannel)key.channel();
            				try
							{
            					rbb.clear();
            					//int i = 0;
            					//i = 
            					socketChannel.read(rbb);
            					rbb.flip();
            					rcb = decoder.decode(rbb);
            					System.out.print(rcb.toString());
            				}
            				catch (Exception e)
							{
            					System.out.println("Connection closed.");
        						System.exit(1);
							}
            			}
            		};
            start();
            
			ByteBuffer bb = ByteBuffer.allocate(BUFFER_SIZE);
			CharBuffer cb = CharBuffer.allocate(BUFFER_SIZE);
			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
			while (true)
			{
				bb.clear();
				cb.clear();
				String s = in.readLine();
				if (s.startsWith("quit"))
					break;
				cb.put(s + "\n");
				cb.flip();
				bb.put(encoder.encode(cb));
				bb.flip();
				socketChannel.write(bb);
			}
			
			socketChannel.close();
			System.exit(1);
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		
	}
 
	public void run()
	{
		while (true) 
		{
			try
			{
			    // Wait for an event
				selector.select();
				
				// Get list of selection keys with pending events
				Iterator<SelectionKey> it = selector.selectedKeys().iterator();
				
				// Process each key
				while (it.hasNext()) 
				{
				    // Get the selection key
					SelectionKey selKey = (SelectionKey)it.next();
					
					// Remove it from the list to indicate that it is being processed
					it.remove();
					
					// Check if it's a connection request
					if (selKey.isReadable())
					{
						readHandler.processRead(selKey);
						//((ReadHandler)selKey.attachment()).processRead();
			        }
			    }
			}
			catch (IOException e) 
			{
				e.printStackTrace();
		    }
		}
	}
	
	public static void main(String[] args)
	{
		if (args.length == 2)
		{
			host = args[0];
			port = Integer.parseInt(args[1]);
		}
		new IPClient();
	}
}

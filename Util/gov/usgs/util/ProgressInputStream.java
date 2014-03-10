package gov.usgs.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Extension of InputStream, we provide expected stream length and
 * this class calls registered progress listeners after every read event 
 * with read percentage.
 * 
 * @see ProgressListener
 * 
 * $Log: not supported by cvs2svn $
 * @author Dan Cervelli
 * @version $Id: ProgressInputStream.java,v 1.1 2007-04-24 21:37:03 dcervelli Exp $
 */
public class ProgressInputStream extends InputStream
{
	/**
	 * Why, WHY do you need another InputStream as member. We already have one because this class extends it 
	 */
	protected InputStream source;
	protected int length;
	protected int read;
	protected int lastProgress = -1;
	
	protected List<ProgressListener> listeners;
	
	/**
	 * @param src Controlled input stream
	 * @param len expected stream length
	 */
	public ProgressInputStream(InputStream src, int len)
	{
		listeners = new ArrayList<ProgressListener>(2);
		source = src;
		length = len;
		read = 0;
	}
	
	/**
	 * Adds progress listener to this class
	 * @param pl
	 */
	public void addProgressListener(ProgressListener pl)
	{
		listeners.add(pl);
	}
	
	/**
	 * Add to read counter
	 * @param cnt Count of bytes to add to read counter
	 */
	protected void addRead(int cnt)
	{
		if (cnt <= 0)
			return;
		
		read += cnt;
		float pct = ((float)read / (float)length);
		int p = (int)(pct * 100);
		if (lastProgress != p)
		{
			lastProgress = p;
			for (ProgressListener pl : listeners)
				pl.progressDone(pct);
		}
	}
	
	/**
	 * @see InputStream#read()
	 */
	public int read() throws IOException
	{
		int r = source.read();
		addRead(1);
		return r;
	}

	/**
	 * @see InputStream#read(byte[])
	 */
	public int read(byte[] b) throws IOException
	{
		int r = source.read(b);
		addRead(r);
		return r;
	}

	/**
	 * @see InputStream#read(byte[], int, int)
	 */
	public int read(byte[] b, int off, int len) throws IOException
	{
		int r = source.read(b, off, len);
		addRead(r);
		return r;
	}
	
	/**
	 * @see InputStream#close()
	 */
	public void close() throws IOException
	{
		listeners.clear();
		listeners = null;
		source.close();
	}
}

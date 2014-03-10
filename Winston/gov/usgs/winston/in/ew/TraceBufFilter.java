package gov.usgs.winston.in.ew;

import gov.usgs.earthworm.message.TraceBuf;
import gov.usgs.util.ConfigFile;
import gov.usgs.util.Util;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * 
 * $Log: not supported by cvs2svn $
 * @author Dan Cervelli
 */
abstract public class TraceBufFilter implements Comparable<TraceBufFilter>
{
	protected boolean keepRejects = false;
	protected Level logLevel = Level.FINEST;
	protected boolean accept = true;
	protected int order = 1;
	protected boolean terminal = true;
	private Map<String, String> metadata;
	
	// return true if tb matches, otherwise false
	abstract protected boolean match(TraceBuf tb, Options options);
	
	public TraceBufFilter() 
	{}
	
	public void configure(ConfigFile cf)
	{
		if (cf == null)
			return;
		
		order = Util.stringToInt(cf.getString("order"), -1);
		
		String action = cf.getString("action");
		if (action == null)
		{
			accept = true;
			return;
		}
		
		if (action.toLowerCase().equals("reject"))
			accept = false;
		
		int log = Util.stringToInt(cf.getString("log"), 0);
		switch(log)
		{
			case 0:
				logLevel = Level.FINEST;
				break;
			case 1:
				logLevel = Level.FINE;
				break;
			case 2:
				logLevel = Level.WARNING;
				break;
		}
	}
	
	public void setKeepRejects(boolean b)
	{
		keepRejects = b;
	}
	
	public boolean keepRejects()
	{
		return keepRejects;
	}
	
	public Level getLogLevel()
	{
		return logLevel;
	}
	
	public boolean isAccept()
	{
		return accept;
	}
	
	public boolean isTerminal()
	{
		return terminal;
	}
	
	public Map<String, String> getMetadata() 
	{
		return metadata;
	}
	
	public void addMetadata(String n, String v)
	{
		if (metadata==null)
			metadata = new HashMap<String, String>();
		
		metadata.put(n, v);
	}
	
	public int compareTo(TraceBufFilter other)
	{
		return order - other.order;
	}
}

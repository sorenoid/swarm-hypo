package gov.usgs.net;

import java.nio.channels.SelectionKey;

/**
 * An interface that handles reads.
 *  
 * $Log: not supported by cvs2svn $
 * @author Dan Cervelli
 */
public interface ReadHandler
{
	public void processRead(SelectionKey key);
//	public boolean isBusy();
}

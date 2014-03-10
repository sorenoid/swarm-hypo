package gov.usgs.util;

/**
 * Listener for stream reading event in ProgressInputStream.
 * 
 * @see ProgressInputStream
 * 
 * $Log: not supported by cvs2svn $
 * @author Dan Cervelli
 * @version $Id: ProgressListener.java,v 1.1 2007-04-24 21:37:03 dcervelli Exp $
 */
public interface ProgressListener
{
	/**
	 * This method is called after each stream reading event.
	 * @param p part of overall progress (0 ... 1) relative whole ProgressInputStream's length
	 */
	public void progressDone(float p);
}

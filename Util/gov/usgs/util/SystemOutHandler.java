package gov.usgs.util;

import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

/**
 * A ConsoleHandler for logging to System.out instead of System.err.
 * 
 * @author Dan Cervelli
 */
public class SystemOutHandler extends StreamHandler
{
	/**
	 * Default constructor
	 */
    public SystemOutHandler()
    {
    	setOutputStream(System.out);
    }
    
    /**
     * Write record
     * @param record LogRecord
     */
    public void publish(LogRecord record) 
    {
        super.publish(record);	
    	flush();
    }

    /**
     * @see StreamHandler#flush()
     */
    public void close() 
    {
        flush();
    }
}

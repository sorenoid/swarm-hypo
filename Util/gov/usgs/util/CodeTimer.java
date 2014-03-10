package gov.usgs.util;

import java.util.logging.Logger;


/**
 * A class for timing blocks of code.  Usage:
 * 
 * <pre>
 * CodeTimer ct = new CodeTimer("timer");
 * bigObject.doSomethingLengthy();
 * ct.stop(); // automatically outputs result to System.out
 * </pre>
 * 
 * @author Dan Cervelli
 */
public class CodeTimer
{
	private String name;
	private long startTime;
	private long endTime;
	private long runTime;
	private long lastMark;
	private long totalTime;
	private Logger logger;

	/**
	 * Default constructor
	 * it creates and starts timer with "unnamed" name
	 */
	public CodeTimer()
	{
		this("unnamed");
	}
	
	/**
	 * Constructor, it creates and starts timer
	 * @param n timer name
	 */
	public CodeTimer(String n)
	{
		this(n, true);
	}

	/**
	 * Constructor
	 * @param n timer name
	 * @param start flag if we need start timer just after construction
	 */
	public CodeTimer(String n, boolean start)
	{
		logger = Log.getLogger("gov.usgs.util.CodeTimer");
		name = n;
		startTime = -1;
		endTime = -1;
		runTime = -1;
		totalTime = 0;
		if (start)
			start();
	}

	/**
	 * Starts this timer
	 */
	public void start()
	{
		startTime = lastMark = System.nanoTime();
	}

	/**
	 * Dump log message to system console with timer name and time mark. Log time in milliseconds
	 * since previous mark.
	 * @param msg message to log
	 */
	public void mark(String msg)
	{
		long now = System.nanoTime();
		String l = String.format("CodeTimer(%s/%s): %.3f\n", name, msg, (double)(now - lastMark) / 1000000.0);
		logger.finest(l);		
		lastMark = now;
	}

	/**
	 * Stops this timer
	 */
	public void stop()
	{
		endTime = System.nanoTime();
		runTime = endTime - startTime;
		startTime = endTime = -1;
		totalTime += runTime;
	}

	public void stopAndReport() {
		stop();
		logger.finest(this.toString());
	}
	/**
	 * Get nanoseconds since last start
	 * @return run time since previous start, in nanoseconds 
	 */
	public long getRunTime()
	{
		return runTime;
	}
	
	/**
	 * Get milliseconds since last start
	 * @return run time since previous start, in milliseconds
	 */
	public double getRunTimeMillis()
	{
		return runTime / 1000000.0;
	}
	
	/**
	 * Get nanoseconds of total run time
	 * @return overall run time, in nanoseconds
	 */
	public long getTotalTime()
	{
		return totalTime;
	}

	/**
	 * Get milliseconds of total run time
	 * @return overall run time, in milliseconds
	 */
	public double getTotalTimeMillis()
	{
		return totalTime / 1000000.0;
	}

	/**
	 * String representation of timer
	 * @return string representation
	 */
	public String toString()
	{
		if (startTime == -1 && endTime == -1)
		{
			if (runTime == -1)
				return "CodeTimer(" + name + "): no info";
			else
			{
				return String.format("CodeTimer(%s): %.3f", name, getRunTimeMillis());
			}
		}
		else if (startTime != -1 && endTime == -1)
		{
			return "CodeTimer(" + name + "): running";
		}
		return "CodeTimer(" + name + "): no info";
	}
}

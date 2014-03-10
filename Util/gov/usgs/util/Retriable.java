package gov.usgs.util;

/**
 * Basic abstract wildcarded class to retrieve wildcard object
 * with using multiple-attempts logic from remote server.
 * Concrete details about connection methods are provided in
 * classes that inherit from Retriable. 
 * 
 * @author Dan Cervelli
 */
abstract public class Retriable<T>
{
	public static boolean defaultOutput = true;
	public static int defaultMaxAttempts = 3; 
	protected int maxAttempts;
	protected String name;
	protected T result;
	protected boolean output;
	
	/**
	 * Default constructor
	 */
	public Retriable()
	{
		maxAttempts = defaultMaxAttempts;
		output = defaultOutput;
		name = "";
	}
	
	/**
	 * Constructor
	 * @param mr Count of attempts to retrieve
	 */
	public Retriable(int mr)
	{
		this();
		maxAttempts = mr;
	}
	
	/**
	 * Constructor
	 * @param n name
	 * @param mr Count of attempts to retrieve
	 */
	public Retriable(String n, int mr)
	{
		this(mr);
		name = n;
		maxAttempts = mr;
	}
	
	/**
	 * Sets default count of connecting attempts
	 * @param m max attempts
	 */
	public static void setDefaultMaxAttempts(int m)
	{
		defaultMaxAttempts = m;
	}

	/**
	 * Sets default boolean value if we enable logging
	 * @param b default value
	 */
	public static void setDefaultOutput(boolean b)
	{
		defaultOutput = b;
	}
	
	public void setOutput(boolean b)
	{
		output = b;
	}
	
	/**
	 * Getter for name
	 * @return name
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Gets count of connecting attempts
	 * @return max attempts
	 */
	public int getMaxAttempts()
	{
		return maxAttempts;
	}
	
	/**
	 * Check for logging
	 * @return Flag if we enable logging
	 */
	public boolean isOutput()
	{
		return output;
	}
	
	/**
	 * Yield result
	 * @return retrieved result 
	 */
	public T getResult()
	{
		return result;
	}
	
	/**
	 * The inherited classes should clear the state after an unsuccessful
	 * attempt to connect.
     * In this class, attemptFix() does nothing. 
	 */
	public void attemptFix() {}
	
	/**
	 * The inherited classes' method to be called after the last unsuccessful connection attempt, should clear state.
     * In this class it does nothing. 
	 */
	public void giveUp() {}
	
	/**
	 * Abstract method to make connection, retrieve result and assign it to 'result' internal variable.
	 * @return flag if retrieving was successful
	 * @throws initialized UtilException if retrieving itself was successful but result carries error flag
	 */
	abstract public boolean attempt() throws UtilException;

	/**
	 * Connects with multiple-attempts logic, gets results
	 * @return retrieved result
	 * @throws initialized UtilException if retrieving itself was successful but result carries error flag
	 */
	public T go() throws UtilException
	{
		int attempts = 0;
		boolean success = false;
		while (!success && attempts < getMaxAttempts())
		{
			attempts++;
			success = attempt();
			if (!success)
			{
				if (isOutput())
					Log.getLogger("gov.usgs.util").fine(getName() + ": failed on attempt " + attempts + ".");
				attemptFix();
			}
		}
		if (!success)
		{
//			if (isOutput())
			Log.getLogger("gov.usgs.util").warning(getName() + ": giving up.");
			giveUp();
		}
		
		return getResult();
	}
}

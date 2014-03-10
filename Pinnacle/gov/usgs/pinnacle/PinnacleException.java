package gov.usgs.pinnacle;

/**
 * <p>Special exception to indicate errors inside the pinnacle package during communication with a device.</p>
 * $Log: not supported by cvs2svn $
 * @author Dan Cervelli
 */
public class PinnacleException extends Exception
{
	private static final long serialVersionUID = 1L;
	
	/**
	 * Constructor
	 * @param m Error message
	 */
	public PinnacleException(String m)
	{
		super(m);
	}
}

package gov.usgs.winston.in.ew;
import gov.usgs.earthworm.message.TraceBuf;

/**
 * 
 * @author Tom Parker
 */
public class JumboTracebufFilter extends TraceBufFilter 
{
	public JumboTracebufFilter()
	{
		super();
	}
	
	// todo: reimplement
	protected boolean match(TraceBuf tb, Options options)
	{
		return (false);
	}

	public String toString()
	{
		return "JumboTracebufFilter";
	}
}

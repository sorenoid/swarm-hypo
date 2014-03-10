package gov.usgs.util.xml;

import java.util.Map;

/**
 * XML event handler for {@link SimpleXMLParser}. 
 * User should extend this interface with concrete methods processing
 * xml actions, and pass it to parser. Parser will call appropriate 
 * method while event happens.
 * 
 * Reformatted and Java 1.5-ized from article:
 * http://www.javaworld.com/javatips/jw-javatip128_p.html
 * 
 * $Log: not supported by cvs2svn $
 * @author Steven R. Brandt, Dan Cervelli
 * @version $Id: XMLDocHandler.java,v 1.1 2007-05-11 05:15:42 dcervelli Exp $
 */
public interface XMLDocHandler
{
	/**
	 * Element started
	 * @param tag Tag name
	 * @param h map of tag attributes and values
	 * @throws Exception
	 */
	public void startElement(String tag, Map<String, String> h) throws Exception;

	/**
	 * Element ended
	 * @param tag Tag name
	 * @throws Exception
	 */
	public void endElement(String tag) throws Exception;

	/**
	 * Document started
	 * @throws Exception
	 */
	public void startDocument() throws Exception;

	/**
	 * Document ended
	 * @throws Exception
	 */	
	public void endDocument() throws Exception;

	/**
	 * Text or CDATA found
	 * @param str
	 * @throws Exception
	 */
	public void text(String str) throws Exception;
}

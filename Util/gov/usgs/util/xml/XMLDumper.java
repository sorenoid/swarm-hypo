package gov.usgs.util.xml;

import java.io.FileReader;
import java.util.Map;

/**
 * Implementation of {@link XMLDocHandler} to parse disk
 * xml files and dump parsing messages about content to System.out
 * 
 * Reformatted and Java 1.5-ized from article:
 * http://www.javaworld.com/javatips/jw-javatip128_p.html
 * 
 * $Log: not supported by cvs2svn $
 * @author Steven R. Brandt, Dan Cervelli
 * @version $Id: XMLDumper.java,v 1.1 2007-05-11 05:15:42 dcervelli Exp $
 */
public class XMLDumper implements XMLDocHandler
{
	// Implementation of DocHandler is below this line
	
	/** Called at start of document */
	public void startDocument()
	{
		System.out.println("  start document");
	}

	/** Called at end of document */
	public void endDocument()
	{
		System.out.println("  end document");
	}

	/** Called at start of element elem w/ attributes h
	 * @param elem tag
	 * @param h mapping of attributes to their values
	 */
	public void startElement(String elem, Map<String, String> h)
	{
		System.out.println("    start elem: " + elem);
		for (String key : h.keySet())
		{
			String val = h.get(key);
			System.out.println("      " + key + " = " + val);
		}
	}

	/** Called at end of element elem
	 * @param elem tag
	 */
	public void endElement(String elem)
	{
		System.out.println("    end elem: " + elem);
	}

	/** Called at Text node text */
	public void text(String text)
	{
		System.out.println("        text: " + text);
	}

	// implementation of DocHandler is above this line
	
	/**
	 * Parse one disk xml file
	 * @param file file name
	 * @throws Exception
	 */
	public static void reportOnFile(String file) throws Exception
	{
		System.out.println("===============================");
		System.out.println("file: " + file);

		// This is all the code we need to parse
		// a document with our DocHandler.
		FileReader fr = new FileReader(file);
		SimpleXMLParser.parse(new XMLDumper(), fr);

		fr.close();
	}
	
	/** 
	 * Main method, parses set of xml files
	 * 
	 * Usage: java XMLDumper [xml file(s)] 
	 */
	public static void main(String[] args) throws Exception
	{
		for (int i = 0; i < args.length; i++)
			reportOnFile(args[0]);
	}
}

package gov.usgs.util.xml;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A simple XML to map conversion.  Note that this is only useful for single
 * instances of tags.  I.e. the last tag of a certain type is the one recorded,
 * example:
 * <tag>A1</tag>
 * <tag>A2</tag>
 * 
 * text.get("tag") == "A2"
 * 
 * $Log: not supported by cvs2svn $
 * @author Dan Cervelli
 * @version $Id: XMLToMap.java,v 1.1 2007-05-11 05:15:42 dcervelli Exp $
 */
public class XMLToMap implements XMLDocHandler
{
	private List<String> path;
	private String current;
	private boolean ended; 
	
	public Map<String, String> text;
	public Map<String, Map<String, String>> attributes;

	/**
	 * Default constructor
	 */
	public XMLToMap()
	{
		path = new ArrayList<String>();
		text = new HashMap<String, String>();
		attributes = new HashMap<String, Map<String,String>>();
	}
	
	//--- XMLDocHandler implementation begin ---

	/** Called at start of document 
	 * @throws Exception
	 */
	public void startDocument() throws Exception
	{}
	
	/** Called at end of document 
	 * @throws Exception
	 */
	public void endDocument() throws Exception
	{}

	/** Called at start of element elem w/ attributes h
	 * @param tag
	 * @param h mapping of attributes to their values
	 * @throws Exception
	 */
	public void startElement(String tag, Map<String, String> h) throws Exception
	{
		StringBuilder sb = new StringBuilder(256);
		for (String p : path)
		{
			sb.append(p);
			sb.append('/');
		}
		path.add(tag == null ? "" : tag);
		sb.append(path.get(path.size() - 1));
		current = sb.toString();
		attributes.put(current, h);
		ended = false;
	}
	
	/** Called at end of element elem
	 * @param tag
	 */
	public void endElement(String tag) throws Exception
	{
		ended = true;
		path.remove(path.size() - 1);
	}

	/** Called at Text node text 
	 * @param str content of Text node
	 */
	public void text(String str) throws Exception
	{
		if (!ended)
			text.put(current, str);
	}

	//--- XMLDocHandler implementation end ---

	/**
	 * Parse one disk xml file
	 * @param file file name
	 * @throws Exception
	 */
	public static void reportOnFile(String file) throws Exception
	{
		// This is all the code we need to parse
		// a document with our DocHandler.
		FileReader fr = new FileReader(file);
		XMLToMap map = new XMLToMap();
		SimpleXMLParser.parse(map, fr);

		System.out.println("Text:");
		for (String key : map.text.keySet())
			System.out.println(key + " = " + map.text.get(key));
		
		System.out.println("Attributes:");
		for (String key : map.attributes.keySet())
		{
			Map<String, String> attr = map.attributes.get(key);
			if (attr != null && attr.size() > 0)
			{
				System.out.println(key + ":");
				for (String akey : attr.keySet())
					System.out.println("\t" + akey + " = " + attr.get(akey));
			}
		}

		fr.close();
	}
	
	/** 
	 * Main method, parses set of xml files
	 * 
	 * Usage: java XMLToMap [xml file(s)] 
	 * @param args command line args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception
	{
		for (int i = 0; i < args.length; i++)
			reportOnFile(args[0]);
	}
}

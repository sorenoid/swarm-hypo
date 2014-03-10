package gov.usgs.net;

import java.util.HashMap;
import java.util.Map;

/**
 * A class for dealing with HTTP requests.
 * 
 * $Log: not supported by cvs2svn $
 * @author Dan Cervelli
 */
public class HttpRequest
{
	private String fullRequest;
	private String method;
	private String resource;
	private String version;
	private String lines[];
//	private Map headers;

	private String file;
	private String argumentString;
	private Map<String, String> arguments;
	
	private boolean valid;
	
	public HttpRequest(String req)
	{
		fullRequest = req;
//		headers = new HashMap();
		arguments = new HashMap<String, String>();
		parseRequest();
	}
	
	protected void parseRequest()
	{
		lines = fullRequest.split("\n");
		String[] line0 = lines[0].split(" ");
		valid = false;
		if (line0.length == 3)
		{
			method = line0[0].trim();
			resource = line0[1].trim();
			version = line0[2].trim();
			valid = true;
		}
		if (valid)
			parseResource();
	}
	
	protected void parseResource()
	{
		int index = resource.indexOf('?');
		if (index == -1)
		{
			file = resource;
			argumentString = "";
		}
		else
		{
			file = resource.substring(0, index);
			argumentString = resource.substring(index + 1);
		}
		parseArguments();
	}
	
	protected void parseArguments()
	{
		if (argumentString == null || argumentString.length() == 0)
			return;
		
		String[] args = argumentString.split("&");
		for (int i = 0; i < args.length; i++)
		{
			String[] arg = args[i].split("=");
			if (arg.length != 2)
				continue;
			arguments.put(arg[0], arg[1]);
		}
	}
	
	public boolean isValid()
	{
		return valid;
	}
	
	public String getMethod()
	{
		return method;
	}
	
	public String getResource()
	{
		return resource;
	}
	
	public String getVersion()
	{
		return version;
	}
	
	public Map<String, String> getArguments()
	{
		return arguments;
	}
	
	public String getFile()
	{
		return file;
	}
	
}

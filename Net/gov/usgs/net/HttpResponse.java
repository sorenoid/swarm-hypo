package gov.usgs.net;

import java.util.HashMap;
import java.util.Map;

/**
 * A class for dealing with HTTP responses.
 * 
 * @author Dan Cervelli
 */
public class HttpResponse
{
	private String version = "HTTP/1.1";
	private String code = "200";
	private String message = "OK";
	private Map<String, String> headers;
	
	public HttpResponse()
	{
		headers = new HashMap<String, String>();
	}
	
	public HttpResponse(String content)
	{
		this();
		setHeader("Content-Type:", content);
	}
	
	public void setCode(String c)
	{
		code = c;
	}
	
	public void setMessage(String m)
	{
		message = m;
	}
	
	public void setVersion(String v)
	{
		version = v;
	}
	
	public void setHeader(String key, String value)
	{
		headers.put(key, value);
	}
	
	public void setLength(int i)
	{
		setHeader("Content-Length:", Integer.toString(i));
	}
	
	public String getHeaderString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append(version + " " + code + " " + message + "\n");
		
		for (Map.Entry<String, String> entry : headers.entrySet())
		{
			sb.append(entry.getKey() + " " + entry.getValue() + "\n");
		}
		
		sb.append("\n");
		return sb.toString();		
	}
}

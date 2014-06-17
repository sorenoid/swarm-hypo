package gov.usgs.winston.server;

import gov.usgs.earthworm.WaveServer;
import gov.usgs.net.ReadListener;
import gov.usgs.util.Arguments;
import gov.usgs.util.Log;
import gov.usgs.util.Retriable;
import gov.usgs.util.Util;
import gov.usgs.util.UtilException;
import gov.usgs.vdx.data.heli.HelicorderData;
import gov.usgs.vdx.data.rsam.RSAMData;
import gov.usgs.vdx.data.wave.SAC;
import gov.usgs.vdx.data.wave.Wave;
import gov.usgs.winston.Channel;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A class that extends the Earthworm Wave Server to include a get helicorder
 * function for WWS.
 *
 * @author Dan Cervelli
 */
public class WWSClient extends WaveServer
{
	protected ReadListener readListener;
	protected final static Logger logger = Log.getLogger("gov.usgs.winston.server.WaveServer");
	
    public WWSClient(String h, int p)
    {
        super(h, p);
        setTimeout(60000);
    }
    
    public void setReadListener(ReadListener rl)
    {
    	readListener = rl;
    }
    
    public int getProtocolVersion()
    {
    	int version = 1;
    	try
    	{
    		if (!connected())
    			connect();

    		socket.setSoTimeout(1000);
    		writeString("VERSION\n");
    		String result = readString();
    		version = Integer.parseInt(result.split(" ")[1]);
    	}
    	catch (Exception e)
    	{}
    	finally
    	{
    		try { socket.setSoTimeout(timeout); } catch (Exception e) {}
    	}
    	return version;
    }
    
    protected byte[] getData(final String req, final boolean compressed)
    {
    	byte[] ret = null;
     		Retriable<byte[]> rt = new Retriable<byte[]>("WWSClient.getData()", maxRetries)
				{
					public void attemptFix()
					{
						close();
					}

					public boolean attempt() throws UtilException
					{
						try
						{
							if (!connected())
								connect();
							
							writeString(req);
							String info = readString();
							
							if (info.startsWith("ERROR"))
							{
								logger.warning("Sent: " + req);
								logger.warning("Got: " + info);
								return false;
							}
							
							String[] ss = info.split(" ");
							int bytes = Integer.parseInt(ss[1]);
							if (bytes == 0)
							    return true;
							byte[] buf = readBinary(bytes, readListener);
							
							if (compressed)
								buf = Util.decompress(buf);
							
							result = buf;
							return true;
						}
						catch (SocketTimeoutException e)
						{
							logger.warning("WWSClient.getData() timeout.");
						}
						catch (IOException e)
						{
							logger.warning("WWSClient.getData() IOException: " + e.getMessage());
						}
						catch (NumberFormatException e)
						{
							logger.warning("WWSClent.getData() couldn't parse server response. Is remote server a Winston Wave Server?");
						}
						return false;
					}
				};
		try{
			ret = rt.go();
    	}
    	catch(UtilException e){
			//Do nothing
		}
    	return ret;
    }
    
    public List<Channel> getChannels()
    {
        return getChannels(false);
    }
    
    public List<Channel> getChannels(final boolean meta)
    {
    	String[] result = null;
     	Retriable<String[]> rt = new Retriable<String[]>("WWSClient.getChannels()", maxRetries)
				{
					public void attemptFix()
					{
						close();
					}

					public boolean attempt() throws UtilException
					{
						try
						{
							if (!connected())
								connect();
							
							String cmd = "GETCHANNELS: GC";
							if (meta)
								cmd += " METADATA";
							writeString(cmd + "\n");
							String info = readString();
							String[] ss = info.split(" ");
							int lines = Integer.parseInt(ss[1]);
							if (lines == 0)
							    return true;
							ss = new String[lines];
							for (int i = 0; i < ss.length; i++)
								ss[i] = readString();
							
							result = ss;
							return true;
						}
						catch (SocketTimeoutException e)
						{
							logger.warning("WWSClient.getChannels() timeout.");
						}
						catch (IOException e)
						{
							logger.warning("WWSClient.getChannels() IOException: " + e.getMessage());
						}
						return false;
					}
				};
    	try{
    		result = rt.go();
    	}
    	catch(UtilException e){
    		//Do nothing 
		}
    	if (result == null)
    		return null;
    	List<Channel> chs = new ArrayList<Channel>(result.length);
    	for (String s : result)
    	{
    		Channel ch = new Channel(s);
    		chs.add(ch);
    	}
    	
    	return chs;
    }
    
    public Wave getWave(String station, String comp, String network, String location, double start,	double end, boolean compress)
    {
    	String req = String.format(Locale.US, "GETWAVERAW: GS %s %s %s %s %f %f %s\n", 
    			station, comp, network, (location == null ? "--" : location),
    			start, end, (compress ? "1" : "0"));
    	byte[] buf = getData(req, compress);
    	if (buf == null)
    		return null;
    	
    	return new Wave(ByteBuffer.wrap(buf));
    }
    
    public HelicorderData getHelicorder(String station, String comp, String network, String location, double start,	double end, boolean compress)
    {
    	String req = String.format(Locale.US, "GETSCNLHELIRAW: GS %s %s %s %s %f %f %s\n", 
    			station, comp, network, location,
    			start, end, (compress ? "1" : "0"));
    	byte[] buf = getData(req, compress);
    	if (buf == null)
    		return null;
    	
    	return new HelicorderData(ByteBuffer.wrap(buf));
    }

    public String[] getStatus() throws UtilException 
    {
    	return getStatus(0d);
    }
    
    public String[] getStatus(Double d) throws UtilException
    {
    	final double ageThreshold = d;
     	Retriable<String[]> rt = new Retriable<String[]>("WWSClient.getStatus()", maxRetries)
		{
			public void attemptFix()
			{
				close();
			}

			public boolean attempt()
			{
				try
				{
					if (!connected())
						connect();

					String cmd = "STATUS: GC " + ageThreshold;
					writeString(cmd + "\n");
					
					String info = readString();
					String[] ss = info.split(": ");
					int lines = Integer.parseInt(ss[1]);
					if (lines == 0)
					    return true;
					
					ss = new String[lines];
					for (int i = 0; i < ss.length; i++)
						ss[i] = readString();
					
					result = ss;
					return true;
				}
				catch (SocketTimeoutException e)
				{
					logger.warning("WWSClient.getStatus() timeout.");
				}
				catch (IOException e)
				{
					logger.warning("WWSClient.getChannels() IOException: " + e.getMessage());
				}
				return false;
			}
		};

		return rt.go();
    }
    
    public RSAMData getRSAMData(String station, String comp, String network, String location, double start,	double end, double period, boolean compress)
    {
    	String req = String.format(Locale.US, "GETSCNLRSAMRAW: GS %s %s %s %s %f %f %f %s\n", 
    			station, comp, network, location,
    			start, end, period, (compress ? "1" : "0"));
    	byte[] buf = getData(req, compress);
    	if (buf == null)
    		return null;
    	
    	return new RSAMData(ByteBuffer.wrap(buf));
    }
    
    public static void outputSac(String s, int p, Double st, Double et, String c) 
    {
		DateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
        String date = df.format(Util.j2KToDate(st)) + "-" + df.format(Util.j2KToDate(et));

    	outputSac(s, p, st, et, c, c.replace('$', '_') + "_" + date + ".sac");
    }
        
    public static void outputSac(String s, int p, Double st, Double et, String c, String fn) 
    {
    	WWSClient winston = new WWSClient(s, p);
    	winston.connect();
    	
    	String[] chan = c.split("\\$");
    	String sta = chan[0];
    	String comp = chan[1];
    	String net = chan[2];
    	String loc = chan.length == 4 ? chan[3] : null;
    	
    	Wave wave = winston.getWave(sta, comp, net, loc, st, et, false);
    	
        if (wave != null)
        {
        	wave = wave.subset(st, et);
		            
            SAC sac = wave.toSAC();
		    sac.kstnm = sta;
		    sac.kcmpnm = comp;
		    sac.knetwk = net;
		    if (loc != null)
		    	sac.khole = loc;

            try
            {
            	sac.write(fn);
            }
            catch (Exception e)
            {
            	e.printStackTrace();
            }
        }
        else
        {
        	System.out.println("Wave not found");
        }
    }
    
    public static void outputText(String s, int p, Double st, Double et, String c) 
    {
    	System.out.println("dumping samples as text\n");
    	WWSClient winston = new WWSClient(s, p);
    	winston.connect();
    	
    	String[] chan = c.split("\\$");
    	String sta = chan[0];
    	String comp = chan[1];
    	String net = chan[2];
    	String loc = chan.length == 4 ? chan[3] : null;
    	
    	Wave wave = winston.getWave(sta, comp, net, loc, st, et, false);
    	
        if (wave != null)
        {
        	wave = wave.subset(st, et);
        	for (int i : wave.buffer)
        		System.out.println(i);

        }
        else
        {
        	System.out.println("Wave not found");
        }
    }
    public static void outputSac(String s, int p, Double st, Double et, String c, String fn, double cs, double wt) 
    {
    	WWSClient winston = new WWSClient(s, p);
    	winston.connect();
    	
    	String[] chan = c.split("\\$");
    	String sta = chan[0];
    	String comp = chan[1];
    	String net = chan[2];
    	String loc = chan.length == 4 ? chan[3] : null;
    	
    	List<Wave> waves = new ArrayList<Wave>();

    	double duration = et - st;
    	int N = (int)Math.ceil(duration / cs) - 1;
    	double t1 = st;
    	double t2 = 0;
    	Wave wavelet;
    	System.out.printf("Gulp size: %f (s), Gulp delay: %d (ms), Number of gulps: %d\n",cs,(long)(wt*1000),N+1);
    	for (int i = 0; i < N; i++){
    		t2 = t1 + cs;
    		System.out.printf("Gulp #%d starting ... ", i+1); 
    		wavelet = winston.getWave(sta, comp, net, loc, t1, t2, false);
    		System.out.printf("done.\n"); 
    		if (wavelet != null)
    			waves.add(wavelet);
        	t1 = t2;
        	if (wt != 0)
	        	try {
	        		System.out.printf("Waiting ... ");
					Thread.sleep((long)(wt*1000));
	        		System.out.printf("done.\n\n");
	
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

    	}
    	t2 = et;
		System.out.printf("Gulp #%d starting ... ", N+1); 
		wavelet = winston.getWave(sta, comp, net, loc, t1, t2, false);
		System.out.printf("done.\n",N+1);
		
		if (wavelet != null)
			waves.add(wavelet);

    	Wave wave = Wave.join(waves, true);
    	
        if (wave != null)
        {
        	wave = wave.subset(st, et);
		            
            SAC sac = wave.toSAC();
		    sac.kstnm = sta;
		    sac.kcmpnm = comp;
		    sac.knetwk = net;
		    if (loc != null)
		    	sac.khole = loc;
		    
            try
            {
            	sac.write(fn);
            }
            catch (Exception e)
            {
            	e.printStackTrace();
            }
        }
        else
        {
        	System.out.println("Wave not found");
        }
    }

	public static void main(String[] as)
	{
		Logger 	logger = Log.getLogger("gov.usgs.winston");
		logger.setLevel(Level.INFO);
		DateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
		df.setTimeZone(TimeZone.getTimeZone("UTC"));

		Double st = null;
		Double et = null;
		String s;
		int p;
		String c;
		
		Set<String> keys = new HashSet<String>();
		Set<String> flags = new HashSet<String>();

		flags.add("--help");
		flags.add("-sac");
		flags.add("-txt");
		keys.add("-s");
		keys.add("-p");
		keys.add("-st");
		keys.add("-et");
		keys.add("-c");
		
		Arguments args = new Arguments(as, flags, keys);
		
		if (args.flagged("--help") | as.length==0)
		{
			System.err.println("java gov.usgs.winston.server.WWSClient [OPTIONS]\n");
			System.out.println("-s [server]\t\tWinston server");
			System.out.println("-p [port]\t\tport");
			System.out.println("-st [yyyymmddHHmmss]\tstart time");
			System.out.println("-et [yyyymmddHHmmss]\tend time");
			System.out.println("-c [s$c$n$l]\t\tchannel");
			System.out.println("-sac\t\t\toutput sac file");
			System.out.println("-txt\t\t\toutput text");
			System.exit(-1);
		}

		s = args.get("-s");
		p = Integer.parseInt(args.get("-p"));

		
		try {
			st = Util.dateToJ2K(df.parse(args.get("-st")));
			et = Util.dateToJ2K(df.parse(args.get("-et")));
		} catch (ParseException e) {
			e.printStackTrace();
		}
		c = args.get("-c");
				
		if (args.flagged("-sac")) {
			System.out.println(s + ":" + p + ":" + st + ":" + et + ":" + c);
			outputSac(s, p, st, et, c);
		}
		
		if (args.flagged("-txt"))
			outputText(s, p, st, et, c);
		
	}

}

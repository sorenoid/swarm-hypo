package gov.usgs.winston.monitor.collector;

import static org.rrd4j.ConsolFun.AVERAGE;
import static org.rrd4j.ConsolFun.LAST;
import static org.rrd4j.ConsolFun.MAX;
import static org.rrd4j.ConsolFun.MIN;
import gov.usgs.earthworm.Menu;
import gov.usgs.earthworm.MenuItem;
import gov.usgs.net.HttpRequest;
import gov.usgs.util.ConfigFile;
import gov.usgs.util.Time;
import gov.usgs.util.Util;
import gov.usgs.util.UtilException;
import gov.usgs.winston.monitor.Collector;
import gov.usgs.winston.server.WWSClient;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import org.rrd4j.DsType;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDef;
import org.rrd4j.core.Sample;
import org.rrd4j.graph.RrdGraph;
import org.rrd4j.graph.RrdGraphDef;


public class WWSCollector extends Collector{

	
	public enum WWSStat {
		CONNECTIONS("connectionCount", "Connection Count", "Connections", "%.0f%s"), 
		CHANNELS("channelCount", "Channel Count", "Channels", "%.0f%s"), 
		DATA_AGE("medianDataAge", "Median Data Age", "Seconds", "%.3f%s");
		
		private String string;
		private String title;
		private String unit;
		private String format;

		private WWSStat(String s, String t, String u, String f)
		{
			string = s;
			title = t;
			unit = u;
			format = f;
		}
		
		public String toString()
		{
			return string;
		}
		
		public String toTitle()
		{
			return title;
		}
		
		public String toUnit()
		{
			return unit;
		}
		
		public static WWSStat fromString(String s)
		{
			if (s == null)
				return null;
			
			if (s.equals("connectionCount"))
				return CONNECTIONS;
			else if (s.equals("channelCount"))
				return CHANNELS;
			else if (s.equals("medianDataAge"))
				return DATA_AGE;
			else
				return null;
		}
	}

	private static final int DEFAULT_INSPECTION_INTERVAL = 5;
	private static final int DEFAULT_PORT=16022;
	private static final String DEFAULT_SERVER="localhost";
	private static final String DEFAULT_AGE_THRESHOLD="-1w";
	private static final String DEFAULT_SPAN="-1d";
	private String server;
	private int port;
	private double ageThreshold;
	private WWSClient winston;
	
	public WWSCollector()
	{
		super();

	}
	
	public void configure(String n, ConfigFile cc, ConfigFile wc)
	{
		setName(n);
		quit = false;
		rrdBase = "rrds";
		winstonConfig(wc);
		inspectionInterval = Util.stringToInt(cc.getString("instpectionInterval"), DEFAULT_INSPECTION_INTERVAL);
		port = Util.stringToInt(cc.getString("port"), DEFAULT_PORT);
		server = Util.stringToString(cc.getString("server"), DEFAULT_SERVER);
		ageThreshold = Time.getRelativeTime(Util.stringToString(cc.getString("ageThreshold"), DEFAULT_AGE_THRESHOLD));
		winston = new WWSClient(server, port);
    	winston.connect();
	}
	

	protected void poll()
	{
		int connectionCount = 0;
		int channelCount = 0;
		double medianDataAge = 0;
		
		if (!winston.connected())
			winston.connect();
    	
    	String[] status = null;
		try {
			status = winston.getStatus(ageThreshold);
		} catch (UtilException e) {
			e.printStackTrace();
		}
    	if (status == null)
    		return;
    	
    	for (String s : status)
    	{
    		if (s.startsWith("Connection count:"))
    			connectionCount = Integer.parseInt(s.split(": ")[1]);
    		else if (s.startsWith("Channel count:"))
    			channelCount = Integer.parseInt(s.split(": ")[1]);
    		else if (s.startsWith("Median data age:"))
    			medianDataAge = Double.parseDouble(s.split(": ")[1]);
    	}
    	
		String rrdPath =  rrdBase + File.separator + getName() + ".rrd";

		// update database
		if (!(new File(rrdPath)).exists())
			createRrd(rrdPath, (long)System.currentTimeMillis()/1000);
		
		updateRrd(rrdPath, connectionCount, channelCount, medianDataAge);

	}


private void createRrd(String rrdPath, long startTime)
{
	int interval = 300;
	System.out.println("== Creating RRD file " + rrdPath);
	RrdDef rrdDef = new RrdDef(rrdPath, startTime-interval, interval);
	for (WWSStat t : WWSStat.values())
		rrdDef.addDatasource(t.toString(), DsType.GAUGE, 600, 0, Double.NaN);
	
	rrdDef.addArchive(LAST, 0.5, 1, 600);
	rrdDef.addArchive(AVERAGE, 0.5, 6, 700);
	rrdDef.addArchive(AVERAGE, 0.5, 24, 775);
	rrdDef.addArchive(AVERAGE, 0.5, 288, 797);
	rrdDef.addArchive(MAX, 0.5, 6, 700);
	rrdDef.addArchive(MAX, 0.5, 24, 775);
	rrdDef.addArchive(MAX, 0.5, 288, 797);
	rrdDef.addArchive(MIN, 0.5, 6, 700);
	rrdDef.addArchive(MIN, 0.5, 24, 775);
	rrdDef.addArchive(MIN, 0.5, 288, 797);
	
	try
	{
		RrdDb rrdDb = new RrdDb(rrdDef);
		System.out.println("== RRD file created.");
		if (rrdDb.getRrdDef().equals(rrdDef)) {
			System.out.println("Checking RRD file structure... OK");
		}
		else {
			System.out.println("Invalid RRD file created. This is a serious bug, bailing out");
			return;
		}
		rrdDb.close();
		System.out.println("== RRD file closed.");
	}
	catch (IOException e)
	{
		System.out.println("Can't create RRD " + rrdPath + ": " + e.getMessage());
	}
}

private void updateRrd(String rrdPath, int connectionCount, int channelCount, double medianDataAge)
{
	Sample sample = null;
	try
	{
		RrdDb rrdDb = new RrdDb(rrdPath);
		 sample = rrdDb.createSample();
		sample.setTime((long)(System.currentTimeMillis()/1000));
		sample.setValue("connectionCount", connectionCount);
		sample.setValue("channelCount", channelCount);
		sample.setValue("medianDataAge", medianDataAge);
		
		System.out.println(sample.dump());
		try
		{
			sample.update();
		}
		catch (IllegalArgumentException e)
		{
			System.out.println("Duplicate rrd update: " + e.getMessage());
		}
			rrdDb.close();
	}
	catch (IOException e)
	{
		System.out.println("Can't update RRD " + rrdPath + ": " + e.getMessage());
	}
}




public byte[] createPlot(HttpRequest req) throws IOException{
	String rrd = rrdBase + File.separator + getName() + ".rrd";
	long start = System.currentTimeMillis()/1000 - (long)Time.getRelativeTime(Util.stringToString(req.getArguments().get(getName() + "_timeSpan"), DEFAULT_SPAN));
	WWSStat stat = WWSStat.fromString(req.getArguments().get(getName() + "_status"));

	RrdGraphDef gDef = getGDef();
	gDef.setStartTime(start);
//	gDef.setNoLegend(true);

	gDef.setTitle(getName() + " " + stat.toTitle());
	gDef.setVerticalLabel(stat.toUnit());
	gDef.datasource(stat.toString()+"_MAX", rrd, stat.toString(), MAX);
	gDef.datasource(stat.toString()+"_MIN", rrd, stat.toString(), MIN);
	gDef.datasource(stat.toString()+"_AVG", rrd, stat.toString(), AVERAGE);
	gDef.area(stat.toString()+"_MAX", maxColor, "");
	gDef.area(stat.toString()+"_MIN", minColor, "");
	gDef.line(stat.toString()+"_AVG", avgColor, "",2f);
	gDef.gprint(stat.toString()+"_AVG", MAX, "Max = " + stat.format);
	gDef.gprint(stat.toString()+"_AVG", MIN, "Min = " + stat.format);
	gDef.gprint(stat.toString()+"_AVG", AVERAGE, "Average = " + stat.format + "\\r");

	RrdGraph graph = new RrdGraph(gDef);
	return graph.getRrdGraphInfo().getBytes();
	}

	public String getMenu() {
		StringBuilder output = new StringBuilder();
		output.append("<INPUT TYPE=\"radio\" NAME=\"" + getName() + "_type\" VALUE=\"plot\" CHECKED>");
		output.append("Time Span: <SELECT NAME=\"" + getName() + "_timeSpan\">");
		output.append("<OPTION VALUE=\"-1d\">1 day</OPTION>");
		output.append("<OPTION VALUE=\"-1w\" SELECTED>1 week</OPTION>");
		output.append("<OPTION VALUE=\"-4w\">4 weeks</OPTION>");
		output.append("<OPTION VALUE=\"-1y\">1 year</OPTION>");
		output.append("</SELECT>");
		output.append("<BR><INPUT TYPE=\"radio\" NAME=\"" + getName() + "_type\" VALUE=\"status\">Status");
		output.append("<BR><INPUT TYPE=\"radio\" NAME=\"" + getName() + "_type\" VALUE=\"menu\">Menu");	
	return output.toString();
	}

	public String createPlotPane(HttpRequest req) throws IOException {
		String c = req.getArguments().get("collector");
		String ts = req.getArguments().get(c+"_timeSpan");
		String action = req.getArguments().get(c+"_type");
		if (action.equals("plot"))
			return buildPlotsPane(c, ts);
		else if (action.equals("status"))
			return buildStatsPane();
		else if (action.equals("menu"))
			return buildWWSMenu(req);
		else
			return "Do know how to " + action;
	}
	
	
	private String buildPlotsPane(String c, String ts)
	{
		StringBuilder output = new StringBuilder();
		for (WWSStat w : WWSStat.values())
		{
			output.append(String.format("<IMAGE SRC=\"/img?collector=%s&%s_status=%s&%s_timeSpan=%s\">", 
					c, c, w.string, c, ts));
			output.append("<P>");
		}
		return output.toString();
	}
	
	private String buildWWSMenu(HttpRequest req)
	{
		StringBuilder output = new StringBuilder();
		if (!winston.connected())
			winston.connect();
		
//		String c = req.getArguments().get("collector");
//		String sortOrder = req.getArguments().get(c+"_sortOrder");
	
		output.append("<DIV style=\"float: left;width: 100%; padding: 0%; \"><DIV style=\"width: 100%; border: solid #2c5700 thin;text-align: center; background: #cbdfe9; padding: .5em;\">" + server + ":" + port + "</DIV><BR>");
		output.append("<TABLE>");
		
		output.append("<TR><TD>Station</TD><TD>Start</TD><TD>End</TD></TR>");
		for (MenuItem i : winston.getMenu().getItems())
			output.append(String.format("<TR><TD>%s</TD><TD>%s&nbsp;&nbsp;</TD><TD>%s</TD></TR>", 
					i.location == null ? i.getSCN("_") : i.getSCNL("_"), 
					Time.toDateString(Util.ewToJ2K(i.startTime)), 
					Time.toDateString(Util.ewToJ2K(i.endTime))));
		output.append("</TABLE>");
		for (String key : req.getArguments().keySet())
			output.append(key + " =  " + req.getArguments().get(key) + "<BR>");
		
		return(output.toString());
	}
	
	private String buildStatsPane()
	{
		StringBuilder output = new StringBuilder();
		if (!winston.connected())
			winston.connect();
    	
		ArrayList<Integer> ages = new ArrayList<Integer>();
    	Menu menu = winston.getMenu();
    	MenuItem mostRecentItem = null;
    	int now = (int)(System.currentTimeMillis()/1000);
    	int lessThanOneMin = 0;
    	int lessThanFiveMin = 0;
    	int lessThanOneHour = 0;
    	int lessThanOneDay = 0;
    	int lessThanFourWeeks = 0;
    	int moreThanFourWeeks = 0;
    	
    	for (MenuItem i : winston.getMenu().getItems())
    	{
    		int age = (int)(now - i.endTime);
    		ages.add(age);
    		if (mostRecentItem == null || mostRecentItem.endTime < i.endTime)
    			mostRecentItem = i;
    		
    		if (age < 60)
    			++lessThanOneMin;
    		else if (age < 60 * 5)
    			++lessThanFiveMin;
    		else if (age < 60 * 60)
    			++lessThanOneHour;
    		else if (age < 60 * 60 * 24)
    			++lessThanOneDay;
    		else if (age < 60 * 60 * 24 * 7 * 4)
    			++lessThanFourWeeks;
    		else
    			++moreThanFourWeeks;
    	}

    	
    	Collections.sort(ages);
    	String mostRecent = mostRecentItem.location == null ? mostRecentItem.getSCN("_") : mostRecentItem.getSCNL("_");
    		
    	output.append("<DIV style=\"float: left;width: 45%; padding: 0%; \"><DIV style=\"width: 100%; border: solid #2c5700 thin;text-align: center; background: #cbdfe9; padding: .5em;\">" + server + ":" + port + "</DIV><BR>");
    	output.append("Channel count: " + menu.numItems() + "<BR>");
    	output.append("Median data age: " + ages.get(ages.size()/2) + " seconds<BR>");
    	output.append("Most recent insert: " + mostRecent + "<BR>");
    	output.append("<P><DIV style=\"width: 100%; border: solid #2c5700 thin;text-align: center; background: #cbdfe9; padding: .5em;\">Data Freshness</DIV><BR>");
    	output.append(String.format("%d < 1 minute <BR>", lessThanOneMin));
    	output.append(String.format("%d < 5 minutes <BR>", lessThanFiveMin));
    	output.append(String.format("%d < 1 hour <BR>", lessThanOneHour));
    	output.append(String.format("%d < 1 day <BR>", lessThanOneDay));
    	output.append(String.format("%d < 4 weeks <BR>", lessThanFourWeeks));
    	output.append(String.format("%d>= 4 weeks <BR>", moreThanFourWeeks));
    	output.append("</DIV>");
    	output.append("<DIV style=\"float: right;width: 45%;padding: 0%; height: 100%;\"><DIV style=\"width: 100%; border: solid #2c5700 thin;text-align: center; background: #cbdfe9; padding: .5em;\">" + (lessThanOneHour + lessThanOneDay) + " channels between 5 minutes and 24 hours old</DIV><BR>");

    	for (MenuItem i : winston.getMenu().getSortedItems())
    		if (now - i.endTime > 60*5 && now - i.endTime < 60*60*24)
    			output.append((i.location == null ? i.getSCN("_") : i.getSCNL("_")) + "<BR>");
    	
    	return output.toString();
	}
}

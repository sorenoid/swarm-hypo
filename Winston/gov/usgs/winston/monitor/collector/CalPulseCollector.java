package gov.usgs.winston.monitor.collector;

import static org.rrd4j.ConsolFun.AVERAGE;
import static org.rrd4j.ConsolFun.MAX;
import static org.rrd4j.ConsolFun.MIN;
import gov.usgs.net.HttpRequest;
import gov.usgs.util.ConfigFile;
import gov.usgs.util.Time;
import gov.usgs.util.Util;
import gov.usgs.util.UtilException;
import gov.usgs.vdx.data.wave.Wave;
import gov.usgs.winston.db.Data;
import gov.usgs.winston.monitor.Collector;
import gov.usgs.winston.monitor.McVcoCalPulse;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import org.rrd4j.DsType;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDef;
import org.rrd4j.core.Sample;
import org.rrd4j.graph.RrdGraph;
import org.rrd4j.graph.RrdGraphDef;

public class CalPulseCollector extends Collector{

	private static final int DEFAULT_INSPECTION_WINDOW = 90;
	private static final int DEFAULT_INSPECTION_INTERVAL = 60;
	private static final String DEFAULT_SPAN="-1d";

	private int inspectionWindow;
	
	private Map<String, Double> lastCalPulse;
	
	public CalPulseCollector()
	{
		super();
	}
	
	public void configure(String n, ConfigFile cc, ConfigFile wc)
	{
		setName(n);
		quit = false;
		lastCalPulse = new HashMap<String, Double>();
		rrdBase = "rrds";
		
		winstonConfig(wc);
		
		inspectionWindow = Util.stringToInt(cc.getString("inspectionWindow"), DEFAULT_INSPECTION_WINDOW);
		logger.info("config: inspectionWindow=" + inspectionWindow);
		
		inspectionInterval = Util.stringToInt(cc.getString("instpectionInterval"), DEFAULT_INSPECTION_INTERVAL);
		
	}

	protected void poll()
	{
		String sql = "SELECT code, value from channels, channelmetadata WHERE channels.sid=channelmetadata.sid";
		Statement s = null;
		
		if (!winston.useRootDatabase())
			fatalError("Can't use root database");
		 
		try {
			s = winston.getNewStatement();
			ResultSet rs = s.executeQuery(sql);
			while (rs.next())
			{
				String code = rs.getString("code");
				double j2kTime = rs.getDouble("value");
				if (!lastCalPulse.containsKey(code) || j2kTime > lastCalPulse.get(code))
				{
					lastCalPulse.put(code, j2kTime);
					
					// get wave and clean it up
					Data d = new Data(winston);
					Wave w = null;
					try {
						w = d.getWave(code, j2kTime, j2kTime+inspectionWindow, Integer.MAX_VALUE);
						w = w.subset(j2kTime, j2kTime+inspectionWindow);
					} catch (UtilException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					McVcoCalPulse cp = new McVcoCalPulse(w);
					insertCalPulse(code, cp);
				}
			}
		} catch (SQLException e1) {
			e1.printStackTrace();
		} finally {
			try {
				s.close();
			} catch (SQLException e) {}
		}
	}


	private void createRrd(String rrdPath, long startTime)
	{
		int step = 21600;
		int heartbeat = step * 4;
		// creation
		RrdDef rrdDef = new RrdDef(rrdPath, startTime-step, step);
		rrdDef.addDatasource("batteryVoltage", DsType.GAUGE, heartbeat, 0, Double.NaN);
		rrdDef.addArchive(AVERAGE, 0.5, 1, 168);
		rrdDef.addArchive(AVERAGE, 0.5, 2, 504);
		rrdDef.addArchive(AVERAGE, 0.5, 4, 365);
		rrdDef.addArchive(AVERAGE, 0.5, 8, 730);
		rrdDef.addArchive(MAX, 0.5, 1, 168);
		rrdDef.addArchive(MAX, 0.5, 2, 504);
		rrdDef.addArchive(MAX, 0.5, 4, 365);
		rrdDef.addArchive(MAX, 0.5, 8, 730);
		rrdDef.addArchive(MIN, 0.5, 1, 42);
		rrdDef.addArchive(MIN, 0.5, 2, 504);
		rrdDef.addArchive(MIN, 0.5, 4, 365);
		rrdDef.addArchive(MIN, 0.5, 8, 7);
		
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

	private void updateRrd(String rrdPath, McVcoCalPulse cp)
	{

		
		try
		{
			RrdDb rrdDb = new RrdDb(rrdPath);
			Sample sample = rrdDb.createSample();
			sample.setTime((long)Util.j2KToEW(cp.getStartTime()));
			sample.setValue("batteryVoltage", cp.getVoltage());
			System.out.println(sample.dump());
			try
			{
				sample.update();
			}
			catch (IllegalArgumentException e)
			{
				logger.finest("Duplicate rrd update: " + e.getMessage());
			}
			rrdDb.close();
		}
		catch (IOException e)
		{
			System.out.println("Can't update RRD " + rrdPath + ": " + e.getMessage());
		}

	}

	private void insertCalPulse(String code, McVcoCalPulse cp) 
	{
		if (!cp.dataFound())
		{
			System.out.println(String.format("Fake cal pulse found: %s at %s", code, Util.j2KToDateString(cp.getStartTime())));
			return;
		}
		
		System.out.println(String.format("Cal pulse found at %s for %s: gain=%d id=%d voltage=%f", Util.j2KToDateString(cp.getStartTime()), code, cp.getGain(), cp.getUnitId(), cp.getVoltage()));

		String rrdPath =  rrdBase + File.separator + getName() + "_" + code + ".rrd";

		// update database
		if (!(new File(rrdPath)).exists())
			createRrd(rrdPath, (long)Util.j2KToEW(cp.getStartTime()));
		
		updateRrd(rrdPath, cp);
	}
	
	public byte[] createPlot(HttpRequest req) throws IOException{
		String code = req.getArguments().get(getName() + "_code");
		String rrd = rrdBase + File.separator + getName() + "_" + code.replace('+', '$') + ".rrd";
		long start = System.currentTimeMillis()/1000 - (long)Time.getRelativeTime(Util.stringToString(req.getArguments().get(getName() + "_timeSpan"), DEFAULT_SPAN));
		
		// create graph
		RrdGraphDef gDef = getGDef();
		gDef.setStartTime(start);
		gDef.setTitle(code.replace('+', ' ') + " Battery Voltage");
		gDef.setVerticalLabel("Volts");
		
		gDef.datasource("batteryVoltageAvg", rrd, "batteryVoltage", AVERAGE);
		gDef.datasource("batteryVoltageMax", rrd, "batteryVoltage", MAX);
		gDef.datasource("batteryVoltageMin", rrd, "batteryVoltage", MIN);
		gDef.setMaxValue(20);
		gDef.setMinValue(10);
		gDef.setAltAutoscale(false);
		gDef.area("batteryVoltageMax", maxColor, "");
		gDef.area("batteryVoltageMin", minColor, "");
		gDef.line("batteryVoltageAvg", avgColor, "",2f);
		gDef.gprint("batteryVoltageMax", MAX, "Max = %.3f%s");
		gDef.gprint("batteryVoltageMin", MIN, "Min = %.3f%s");
		gDef.gprint("batteryVoltageAvg", AVERAGE, "Average = %.3f%S\\r");

		RrdGraph graph = new RrdGraph(gDef);
		return graph.getRrdGraphInfo().getBytes();
	}

	public String getMenu() {
		StringBuilder output = new StringBuilder();
			output.append("Station: <SELECT NAME=\"" + getName() + "_code\">");
			for (String s : getStations())
				output.append(String.format("<OPTION NAME=\"%s\">%s</OPTION>", s, s));
			output.append("</SELECT>");

		return output.toString();
	}
	
	public String createPlotPane(HttpRequest req) throws IOException {
		String c = req.getArguments().get("collector");
		String code = req.getArguments().get(c+"_code");
		StringBuilder output = new StringBuilder();
		output.append(String.format("<IMAGE SRC=\"/img?collector=%s&%s_code=%s&%s_timeSpan=-1w&%s_height=134\">", c, c, code, c, c));
		output.append("<P>");
		output.append(String.format("<IMAGE SRC=\"/img?collector=%s&%s_code=%s&%s_timeSpan=-4w&%s_height=134\">", c, c, code, c, c));
		output.append("<P>");
		output.append(String.format("<IMAGE SRC=\"/img?collector=%s&%s_code=%s&%s_timeSpan=-1y&%s_height=134\">", c, c, code, c, c));
		
		return output.toString();
	}

}

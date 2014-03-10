package gov.usgs.winston.server.cmd.http;

import gov.usgs.net.HttpResponse;
import gov.usgs.net.NetTools;
import gov.usgs.plot.DefaultFrameDecorator;
import gov.usgs.plot.DefaultFrameDecorator.Location;
import gov.usgs.plot.MatrixRenderer;
import gov.usgs.plot.Plot;
import gov.usgs.plot.PlotException;
import gov.usgs.util.Util;
import gov.usgs.util.UtilException;
import gov.usgs.vdx.client.VDXClient.DownsamplingType;
import gov.usgs.vdx.data.GenericDataMatrix;
import gov.usgs.vdx.data.rsam.RSAMData;
import gov.usgs.winston.db.WinstonDatabase;
import gov.usgs.winston.server.WWS;

import java.awt.Color;
import java.nio.ByteBuffer;

/**
 * Return a rsam plot.
 * 
 * @author Tom Parker
 * 
 */
public final class HttpRsamCommand extends AbstractHttpCommand {
	private static final int DEFAULT_HEIGHT = 300;
	private static final int DEFAULT_WIDTH = 900;
	private static final boolean DEFAULT_DESPIKE = false;
	private static final boolean DEFAULT_DETREND = false;
	
	private String tz;
	private Double endTime;
	private Double startTime;
	private int width;
	private int height;
	private boolean detrend;
	private boolean despike;
	private double despikePeriod;
	private boolean runningMedian;
	private double runningMedianPeriod;
	private double plotMax;
	private double plotMin;
	private String errorString = "";
	private String code;
	private RSAMData rsamData;

	public HttpRsamCommand(NetTools nt, WinstonDatabase db, WWS wws) {
		super(nt, db, wws);
	}

	
	private String parseArguments() {
		code = arguments.get("code");
		if (code == null)
			errorString += "Error: you must specify a channel (code).<br>";
		else {
			code = code.replace('_', '$');
			if (code.indexOf(";") != -1)
				errorString += "Error: illegal characters in channel (code).<br>";
		}

		tz = Util.stringToString(arguments.get("tz"), "UTC");

		endTime = getEndTime(arguments.get("t2"));
		if (endTime == Double.NaN)
			errorString += "Error: could not parse end time (t2). Should be " + DATE_FORMAT + ".<br>";

		startTime = getStartTime(arguments.get("t1"), endTime, ONE_DAY);
		if (startTime == Double.NaN)
			errorString += "Error: could not parse start time (t1). Should be " + DATE_FORMAT + " or -HH.M<br>";

		width = Util.stringToInt(arguments.get("w"), DEFAULT_WIDTH);
		height = Util.stringToInt(arguments.get("h"), DEFAULT_HEIGHT);

		if (height * width <= 0 || height * width > wws.httpMaxSize())
			errorString += "Error: product of width (w) and height (h) must be between 1 and " + wws.httpMaxSize()
					+ ".<br>";

		detrend = Util.stringToBoolean(arguments.get("dt"), DEFAULT_DETREND);

		despike = Util.stringToBoolean(arguments.get("ds"), DEFAULT_DESPIKE);
		despikePeriod = Util.stringToDouble(arguments.get("dsp"), 0);

		runningMedian = Util.stringToBoolean(arguments.get("rm"), false);
		runningMedianPeriod = Util.stringToDouble(arguments.get("rmp"), 300);

		plotMax = Util.stringToDouble(arguments.get("max"), Double.MAX_VALUE);
		plotMin = Util.stringToDouble(arguments.get("min"), Double.MIN_VALUE);

		return errorString;
	}

	
	private void getData() {
		rsamData = null;
		try {
			rsamData = data.getRSAMData(code, startTime, endTime, 0, DownsamplingType.NONE, 0);
		} catch (UtilException e) {
			writeSimpleHTML("Error: could not get RSAM data, check channel (code). e = " + e.toString());
		}
		if (rsamData == null || rsamData.rows() <= 0)
			writeSimpleHTML("Error: could not get RSAM data, check channel (code). Empty result.");
	}

	
	private void sendPlot() {
		HttpResponse response = new HttpResponse("image/png");
		response.setVersion(request.getVersion());
		if (wws.httpRefreshInterval() > 0)
			response.setHeader("Refresh:", wws.httpRefreshInterval() + "; url=" + request.getResource());

		byte[] png;
		GenericDataMatrix gdm = new GenericDataMatrix(rsamData.getData());

		if (despike)
			gdm.despike(1, despikePeriod);

		if (detrend)
			gdm.detrend(1);

		if (runningMedian)
			gdm.set2median(1, runningMedianPeriod);

		Plot plot = new Plot();
		plot.setSize(width, height);
		plot.setBackgroundColor(new Color(0.97f, 0.97f, 0.97f));

		MatrixRenderer mr = new MatrixRenderer(gdm.getData(), false);
		double max = Math.min(plotMax, gdm.max(1) + gdm.max(1) * .1);
		double min = Math.max(plotMin, gdm.min(1) - gdm.max(1) * .1);
		mr.setExtents(startTime, endTime, min, max);
		mr.setLocation(70, 35, width - 140, height - 70);
		mr.createDefaultAxis();
		mr.setXAxisToTime(8, true, true);
		mr.getAxis().setBottomLabelAsText(
				tz + " Time (" + Util.j2KToDateString(startTime, DATE_FORMAT) + " to "
						+ Util.j2KToDateString(endTime, DATE_FORMAT) + ")");
		mr.getAxis().setLeftLabelAsText("RSAM");
		DefaultFrameDecorator.addLabel(mr, code.replace('$', ' '), Location.LEFT);
		mr.createDefaultLineRenderers(Color.blue);
		// mr.setExtents(startTime, endTime, gdm.min(1), gdm.max(1));
		plot.addRenderer(mr);
		try {
			png = plot.getPNGBytes();
			response.setLength(png.length);
			netTools.writeString(response.getHeaderString(), channel);
			netTools.writeByteBuffer(ByteBuffer.wrap(png), channel);
		} catch (PlotException e) {
			e.printStackTrace();
			errorString = errorString + "Error: Can not create plot.";
		}

	}

	protected void sendResponse() {
		errorString = parseArguments();

		if (errorString.length() > 0) {
			writeSimpleHTML(errorString);
		} else {
			getData();
			sendPlot();
		}
	}

	public String getUsage() {
		StringBuilder output = new StringBuilder();
		output.append("You can construct urls that return graphical RSAM plots as follows:");
		output.append("<br><br><code>http://&lt;hostname&gt;:&lt;port&gt;/rsam?code=AUI_EHZ_AV&w=900&h=300&t1=-7&t2=now&tz=UTC<br><br>"
				+ "</code>This url will return a helicorder for the channel <code>AUL EHZ AV</code> depicting the last 24 hours of data."
				+ "The options (separated by the & character, all optional except for <code>code</code> ) are defined as follows:<br><br>"
				+ "<code>w</code>: <b>Width</b> in pixels of the returned image (default = "
				+ DEFAULT_WIDTH
				+ ").<br><br>"
				+ "<code>h</code>: <b>Height</b> in pixels of the returned image (default = "
				+ DEFAULT_HEIGHT
				+ ").<br><br>"
				+ ""
				+ "<code>t1</code>: <b>Start Time</b> The start time (local) of the helicorder as given by the number of days before present or a specific time in the format YYYYMMDDHHMM.  Note that, in the first case, this is a negative number (default = -12).<br><br>"
				+ ""
				+ "<code>t2</code>: <b>End Time</b> The end time (local) of the helicorder as given by the format YYYYMMDDHHMM or 'now' (default = 'now').<br><br>"
				+ ""
				+ "<code>dt</code>: <b>Detrend</b> Whether to detrend (linear) the plot, 1 is yes, 0 is no (default = 0).<br><br>"
				+ "<code>ds</code>: <b>Despike</b> Whether to despike (mean) the plot, 1 is yes, 0 is no (default = 0).<br><br>"
				+ "<code>dsp</code>: <b>Despike Period</b> Period to use for despike (default = 0).<br><br>"
				+ "<code>max</code>: <b>Plot Max</b> Largest value to plot<BR><BR>"
				+ "<code>min</code>: <b>Plot Min</b> Smallest value to plot<BR><BR>"
				+ "<code>rm</code>: <b>Running Median</b>Whether to apply a running median filter<br><br>"
				+ "<code>rmp</code>: <b>Running Median Period</b> Period to use for running medial, in seconds (defualt = 300)<br><br>"
				+ ""
				+ "<code>tz</code>: <b>Time Zone</b> The time zone, a complete list of time zones that WWS understands is shown below.<br><br>");
		return output.toString();
	}

	public String getAnchor() {
		return "rsam";
	}

	public String getTitle() {
		return "RSAM Plots";
	}

	public String getCommand() {
		return "/rsam";
	}
}

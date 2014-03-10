package gov.usgs.winston.server.cmd.http;

import gov.usgs.net.HttpResponse;
import gov.usgs.net.NetTools;
import gov.usgs.net.Server;
import gov.usgs.plot.PlotException;
import gov.usgs.util.CodeTimer;
import gov.usgs.util.Util;
import gov.usgs.util.UtilException;
import gov.usgs.vdx.data.heli.HelicorderData;
import gov.usgs.vdx.data.heli.plot.HelicorderSettings;
import gov.usgs.winston.db.WinstonDatabase;
import gov.usgs.winston.server.WWS;

import java.nio.ByteBuffer;
import java.util.TimeZone;
import java.util.logging.Level;

/**
 * Return a Helicorder plot.
 * 
 * @author Tom Parker
 * 
 */
public final class HttpHeliCommand extends AbstractHttpCommand {
	private static final int MAX_HOURS = 144;
	private static final int MIN_HOURS = 1;
	private static final int DEFAULT_HEIGHT = 800;
	private static final int DEFAULT_WIDTH = 800;
	private static final String DEFAULT_TIME_ZONE = "UTC";
	private static final int DEFAULT_TIME_CHUNK = 30;
	private static final double MAX_TIME_CHUNK = 21600;
	private static final boolean DEFAULT_SHOW_CLIP = true;
	private static final boolean DEFAULT_FORCE_CENTER = false;

	public HttpHeliCommand(NetTools nt, WinstonDatabase db, WWS wws) {
		super(nt, db, wws);
	}

	protected void sendResponse() {

		CodeTimer ct = new CodeTimer("HttpHeliCommand");

		String error = "";
		HelicorderSettings settings = new HelicorderSettings();

		settings.channel = arguments.get("code");
		if (settings.channel == null)
			error = "Error: you must specify a channel (code).";
		else {
			settings.channel = settings.channel.replace('_', '$');
			if (settings.channel.indexOf(";") != -1)
				error = "Error: illegal characters in channel (code).";
		}

		String tz = Util.stringToString(arguments.get("tz"), DEFAULT_TIME_ZONE);
		settings.timeZone = TimeZone.getTimeZone(tz);

		settings.endTime = getEndTime(arguments.get("t2"));
		if (settings.endTime == Double.NaN)
			error = error + "Error: could not parse end time (t2). Should be " + DATE_FORMAT + ".";

		settings.startTime = getStartTime(arguments.get("t1"), settings.endTime, ONE_HOUR);
		if (settings.startTime == Double.NaN)
			error += "Error: cannot parse start time. Should be " + DATE_FORMAT + " or -HH. I received "
					+ arguments.get("t1");

		if (settings.endTime - settings.startTime > MAX_HOURS * ONE_HOUR)
			error += "Error: Plot must not be more that " + MAX_HOURS + " hours long";
		else if (settings.endTime - settings.startTime < MIN_HOURS * ONE_HOUR)
			error += "Error: Plot cannot be less than " + MIN_HOURS + " hour long";

		settings.timeChunk = Util.stringToDouble(arguments.get("tc"), DEFAULT_TIME_CHUNK) * ONE_MINUTE;
		if (settings.timeChunk <= 0 || settings.timeChunk > MAX_TIME_CHUNK)
			error = error + "Error: time chunk (tc) must be greater than 0 and less than " + MAX_TIME_CHUNK + ".";

		int width = Util.stringToInt(arguments.get("w"), DEFAULT_WIDTH);
		int height = Util.stringToInt(arguments.get("h"), DEFAULT_HEIGHT);
		settings.setSizeFromPlotSize(width, height);

		if (settings.height * settings.width <= 0 || settings.height * settings.width > wws.httpMaxSize())
			error = error + "Error: product of width (w) and height (h) must be between 1 and " + wws.httpMaxSize()
					+ ".";

		settings.showClip = Util.stringToBoolean(arguments.get("sc"), DEFAULT_SHOW_CLIP);
		settings.forceCenter = Util.stringToBoolean(arguments.get("fc"), DEFAULT_FORCE_CENTER);
		settings.barRange = Util.stringToInt(arguments.get("br"), -1);
		settings.clipValue = Util.stringToInt(arguments.get("cv"), -1);

		settings.largeChannelDisplay = Util.stringToBoolean(arguments.get("lb"));

		settings.minimumAxis = Util.stringToBoolean(arguments.get("min"));
		if (settings.minimumAxis)
			settings.setMinimumSizes();

		if (error.length() > 0) {
			ct.stop();
			writeSimpleHTML(error);
		} else {
			HelicorderData heliData = null;
			try {
				heliData = data.getHelicorderData(settings.channel, settings.startTime, settings.endTime, 0);
			} catch (UtilException e) {
			}
			ct.stop();

			// Did it take too long to gather the data?
			if (wws.getSlowCommandTime() > 0 && ct.getRunTimeMillis() > wws.getSlowCommandTime() * .75)
				wws.log(Level.INFO,
						String.format("slow db query (%1.2f ms) http/heli " + settings.channel + " "
								+ settings.startTime + " -> " + settings.endTime + " ("
								+ decimalFormat.format(settings.endTime - settings.startTime) + ") ", ct.getRunTimeMillis()), channel);

			if (heliData == null || heliData.rows() <= 0)
				writeSimpleHTML("Error: could not get helicorder data, check channel (code).");
			else {
				ct.start();
				HttpResponse response = new HttpResponse("image/png");
				response.setVersion(request.getVersion());
				if (wws.httpRefreshInterval() > 0)
					response.setHeader("Refresh:", wws.httpRefreshInterval() + "; url=" + request.getResource());

				byte[] png;
				try {
					png = settings.createPlot(heliData).getPNGBytes();
					response.setLength(png.length);
					netTools.writeString(response.getHeaderString(), channel);
					netTools.writeByteBuffer(ByteBuffer.wrap(png), channel);
				} catch (PlotException e) {
					e.printStackTrace();
					error = error + "Error: Can not create plot.";
				}

				ct.stop();
				// Did it take too long to deliver the data?
				if (wws.getSlowCommandTime() > 0 && ct.getRunTimeMillis() > wws.getSlowCommandTime() * .75)
					wws.log(Level.INFO,
							String.format("slow network (%1.2f ms) http/heli? " + settings.channel + " "
									+ settings.startTime + " -> " + settings.endTime + " ("
									+ decimalFormat.format(settings.endTime - settings.startTime) + ") ", ct.getRunTimeMillis()), channel);

			}
		}
	}

	public String getUsage() {
		StringBuilder output = new StringBuilder();
		output.append("You can construct urls that return graphical helicorders as follows:");
		output.append("<br><br><code>http://&lt;hostname&gt;:&lt;port&gt;/heli?code=AUI_EHZ_AV&w=1280&h=1024&tc=30&t1=-24&t2=now&tz=America/Anchorage<br><br>"
				+ "</code>This url will return a helicorder for the channel <code>AUL EHZ AV</code> depicting the last 24 hours of data."
				+ "The options (separated by the & character, all optional except for <code>code</code> ) are defined as follows:<br><br>"
				+ "<code>w</code>: <b>Width</b> in pixels of the returned image (default = "
				+ DEFAULT_WIDTH
				+ ").<br><br>"
				+ "<code>h</code>: <b>Height</b> in pixels of the returned image (default = "
				+ DEFAULT_HEIGHT
				+ ").<br><br>"
				+ "<code>tc</code>: <b>Time Chunk</b> length of x axis in minutes (default = "
				+ DEFAULT_TIME_CHUNK
				+ ").<br><br>"
				+ ""
				+ "<code>t1</code>: <b>Start Time</b> The start time (local) of the helicorder as given by the number of hours before present or a specific time in the format YYYYMMDDHHMM.  Note that, in the first case, this is a negative number (default = -12).<br><br>"
				+ ""
				+ "<code>t2</code>: <b>End Time</b> The end time (local) of the helicorder as given by the format YYYYMMDDHHMM or 'now' (default = 'now').<br><br>"
				+ ""
				+ "<code>tz</code>: <b>Time Zone</b> The time zone, a complete list of time zones that WWS understands is shown below.<br><br>"
				+ "<code>sc</code>: <b>Show Clip</b> Whether to show a clipped value as red, 1 is yes, 0 is no (default = "
				+ boolToInt(DEFAULT_SHOW_CLIP)
				+ ").<br><br>"
				+ "<code>fc</code>: <b>Force Center</b> Whether to center traces, 1 is yes, 0 is no (default = "
				+ boolToInt(DEFAULT_FORCE_CENTER)
				+ ").<br><br>"
				+ "<code>br</code>: <b>Bar Range</b> Controls the size of helicorder lines (default = auto).<br><br>"
				+ "<code>cv</code>: <b>Clip Value</b> Sets the number of counts above which to clip (default = auto).<br><br>"
				+ "<code>lb</code>: <b>Label</b> Whether to display a large label, 1 is yes, 0 is no (default = 0).<br><br>"
				+ "The WWS does basic argument checking to prevent malignant attacks (like SQL injection) or just silly requests (like a 10000 x 10000 pixel graph).");

		return output.toString();
	}

	public String getAnchor() {
		return "heli";
	}

	public String getTitle() {
		return "Helicorder Plots";
	}

	public String getCommand() {
		return "/heli";
	}
}
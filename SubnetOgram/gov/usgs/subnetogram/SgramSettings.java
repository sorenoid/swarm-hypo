package gov.usgs.subnetogram;

import gov.usgs.util.ConfigFile;
import gov.usgs.util.Time;
import gov.usgs.util.Util;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import org.thymeleaf.context.Context;

/**
 * A class to hold settings common to all types of output.
 * 
 * * Adding a setting? Yes, it's a bit tedious. However it's all here and it
 * makes everything else easy.
 * 
 * 1. Set default if appropriate
 * 
 * 2. Create public final instance variable
 * 
 * 3. Apply default setting in applyDefaults()
 * 
 * 4. Set value in constructor
 * 
 * 5. Add variable to toString()
 * 
 * 6. Add variable to applySettings()
 * 
 * 7. Add variable to getConfigFile()
 * 
 * TODO: stop exposing mutable objects
 * 
 * @author Tom Parker
 * 
 */
public class SgramSettings {

	// Totally sane defaults
	public static final String DEFAULT_DURATION = "10";
	public static final String DEFAULT_PATH_ROOT = "./";
	public static final String DEFAULT_TIME_ZONE_NAME = "UTC";
	public static final String DEFAULT_FILE_NAME_DATE_FORMAT = "_yyyyMMdd-HHmm";
	public static final String DEFAULT_FILE_PATH_DATE_FORMAT = "yyyy/DDD/";
	public static final String DEFAULT_SUBNET_NAME = "unamedSubnet";
	public static final String DEFAULT_ON_ERROR = "exit";
	public static final String DEFAULT_EMBARGO = "0";
	public static final String DEFAULT_ON_MARK = "true";
	public static final String DEFAULT_WRITE_HTML = "true";

	/** My logger */
	protected static final Logger logger = Logger.getLogger("gov.usgs.subnetogram");

	/** Timezone to display on pages. App uses UTC. */
	public final TimeZone timeZone;

	/** Timezone name. Only used to create the timeZone object */
	public final String timeZoneName;

	/** Time zone offset in milliseconds */
	public final int timeZoneOffset;

	/** Time of earliest data point */
	public final Date startTime;

	/** Time of last data point */
	public final Date endTime;

	/** Time of next image endTime + period */
	public final Date nextTime;

	/** Time of previous image endTime - period */
	public final Date previousTime;

	/** Duration of spectrogram. endTime - startTime */
	public final int duration;

	/** generation period, in seconds. How frequently should this element be generated? */
	public final int period;

	/** Where to put output. */
	public final String fileNameDateFormat;

	/** How to append the date to file names */
	public final String filePathDateFormat;

	/** The name of the grouping of stations */
	public final String subnetName;

	/** The name of the grouping of subnets */
	public final String networkName;
	public final List<String> networks;
	
	/** Text to be added after the date, if present, and before the file extension. */
	public final String fileSuffix;

	/** what to do when an exception is thrown? values exit and continue are known. Maybe this should be an enum? */
	public final String onError;

	/** Try to work this far in the past. Useful for high-latency data */
	public final int embargo;

	/** If true images on an even period interval */
	public final boolean onMark;

	/** Filesystem root of output. Images will be placed in a directory of the same name as the subnet. This along with all other paths must end with a seperator. */
	public final String pathRoot;

	/** application launch in miliseconds. Indended to be set by creating class with System.currentTimeMillis() */
	public final long applicationLaunch;

	/** I might need this later */
	public final ConfigFile configFile;

	/** create HTML pages? */
	public final boolean writeHtml;

	/**
	 * Class constructor
	 * 
	 * @param cf
	 *            configfile to retrieve settings from
	 */
	public SgramSettings(ConfigFile cf) {

		setDefaults(cf);

		String s = cf.getString("applicationLaunch");
		if (s == null)
			fatalError("application did not set applicationLaunch");
		applicationLaunch = Long.parseLong(cf.getString("applicationLaunch"));

		// Not mine. Clone it just to keep things on the up-and-up.
		configFile = cf.clone();

		pathRoot = cf.getString("pathRoot");
		timeZoneName = cf.getString("timeZoneName");
		timeZone = TimeZone.getTimeZone(timeZoneName);
		duration = Integer.parseInt(cf.getString("duration"));
		period = Integer.parseInt(cf.getString("period"));
		embargo = Integer.parseInt(cf.getString("embargo"));
		onMark = Util.stringToBoolean(cf.getString("onMark"));

		// startTime is optional, but has rules. Lots of rules.
		// TODO: figure this stuff out
		Calendar cal = Calendar.getInstance();
		cal.setTime(parseStartTime(cf.getString("startTime")));
		cal.setTimeZone(timeZone);
		cal.add(Calendar.MINUTE, -embargo);

		// always reference zero milliseconds
		cal.add(Calendar.MILLISECOND, -1
				* ((int) (cal.getTimeInMillis() % 1000)));

		if (onMark)
			cal.add(Calendar.SECOND, -1
					* ((int) (cal.getTimeInMillis() / 1000) % (period * 60)));

		startTime = cal.getTime();

		cal.add(Calendar.SECOND, duration * 60);
		endTime = cal.getTime();

		timeZoneOffset = timeZone.getOffset(endTime.getTime());

		cal.add(Calendar.MINUTE, duration);
		nextTime = cal.getTime();

		cal.add(Calendar.MINUTE, -2 * duration);
		previousTime = cal.getTime();

		fileNameDateFormat = cf.getString("fileNameDateFormat");
		filePathDateFormat = cf.getString("filePathDateFormat");
		subnetName = cf.getString("subnetName");
		fileSuffix = cf.getString("fileSuffix");
		onError = cf.getString("onError");
		networkName = cf.getString("networkName");
		networks = cf.getList("network");
		writeHtml = Util.stringToBoolean(cf.getString("writeHtml"));
	}

	protected void setDefaults(ConfigFile cf) {
		cf.put("pathRoot", Util.stringToString(cf.getString("pathRoot"),
				DEFAULT_PATH_ROOT), false);
		cf.put("timeZoneName", Util.stringToString(cf.getString("timeZoneName"),
				DEFAULT_TIME_ZONE_NAME), false);
		cf.put("duration",
				Util.stringToString(cf.getString("duration"), DEFAULT_DURATION),
				false);
		cf.put("period",
				Util.stringToString(cf.getString("period"),
						"" + cf.getString("duration")));
		cf.put("embargo",
				Util.stringToString(cf.getString("embargo"), DEFAULT_EMBARGO),
				false);
		cf.put("onMark",
				Util.stringToString(cf.getString("onMark"), DEFAULT_ON_MARK),
				false);
		cf.put("fileNameDateFormat", Util.stringToString(
				cf.getString("fileNameDateFormat"),
				DEFAULT_FILE_NAME_DATE_FORMAT), false);
		cf.put("filePathDateFormat", Util.stringToString(
				cf.getString("filePathDateFormat"),
				DEFAULT_FILE_PATH_DATE_FORMAT), false);
		cf.put("subnetName", Util.stringToString(cf.getString("subnetName"),
				DEFAULT_SUBNET_NAME), false);
		cf.put("fileSuffix",
				Util.stringToString(cf.getString("fileSuffix"), ""), false);
		cf.put("onError",
				Util.stringToString(cf.getString("onError"), DEFAULT_ON_ERROR),
				false);
		cf.put("writeHtml", Util.stringToString(cf.getString("writeHtml"), DEFAULT_WRITE_HTML), false);
	}

	/**
	 * Try to catch and log errors before an exception causes execution to halt.
	 * 
	 * @param msg
	 *            The string to log.
	 */
	public void fatalError(String msg) {
		logger.severe(msg);
		if (onError.equals("exit"))
			System.exit(1);
	}

	/**
	 * Parse a startTime
	 * 
	 * @param startTime
	 * @return The startTime
	 */
	public Date parseStartTime(String startTime) {

		if (startTime == null)
			startTime = "-" + duration + "i";

		if (startTime.charAt(0) == '-') {
			return new Date(System.currentTimeMillis()
					- ((long) Time.getRelativeTime(startTime) * 1000));
		} else {
			SimpleDateFormat format = new SimpleDateFormat(
					Time.INPUT_TIME_FORMAT);
			format.setTimeZone(timeZone);

			Date d = null;
			try {
				d = format.parse(startTime);
			} catch (ParseException e) {
				fatalError("Can't parse startTime (" + startTime + "): " + e);
			}
			return d;
		}
	}

	/**
	 * Generate a file path by applying a SimpleDateFormat
	 * 
	 * @return generated file path
	 */
	public String generateFilePath(Date time, String dateFormat, String subnetName) {
		StringBuilder sb = new StringBuilder();
		sb.append(pathRoot );
		if (networkName != null)
			sb.append(networkName + '/');
		sb.append(subnetName + '/');
		if (dateFormat != null) 
			sb.append(Time.format(dateFormat, time));
		
		return sb.toString();
	}

		
	/**
	 * Generate a file name, without extension, by applying a SimpleDateFormat
	 * 
	 * @return generated file name
	 */
	public String generateFileName(Date time, String dateFormat, String subnetName) {
		StringBuilder sb = new StringBuilder();
		sb.append(subnetName);
		
		if (dateFormat != null)
			sb.append(Time.format(dateFormat, time));
		
		sb.append(fileSuffix);
		return sb.toString();
	}

	/**
	 * Is this worth the trouble? Why not just always call the replaceAll()?
	 * 
	 * @param path
	 *            filesystem path to convert
	 * @return The path suitable for use in a URL
	 */
	public String pathToPath(String path) {
		if (File.separator.equals("/"))
			return path;
		else
			return path.replaceAll("/", Matcher.quoteReplacement(File.separator));
	}

	/**
	 * return settings with all fields decrimented by duration
	 */
	protected ConfigFile getConfigFileOffset(int offset) {

		ConfigFile cf = new ConfigFile();

		Calendar cal = Calendar.getInstance();
		cal.setTime(startTime);
		cal.setTimeZone(timeZone);
		cal.add(Calendar.MINUTE, offset);
		
		// startTime is in localtime.
		cal.add(Calendar.MILLISECOND, timeZone.getOffset(startTime.getTime()));

		cf.put("startTime", Time.format(Time.INPUT_TIME_FORMAT, cal.getTime()));
		cf.put("fileNmeDateFormat", fileNameDateFormat);
		cf.put("filePathDateFormat", filePathDateFormat);
		cf.put("subnetName", subnetName);
		cf.put("pathRoot", pathRoot);
		cf.put("applicationLaunch", "" + applicationLaunch);
		cf.put("duration", "" + duration);
		cf.put("period", "" + period);
		cf.put("fileSuffix", fileSuffix);
		cf.put("timeZoneName", timeZoneName);
		cf.put("textTimeOffset", "" + timeZoneOffset);
		cf.put("onError", onError);
		cf.put("embargo", "" + embargo);
		cf.put("networkName", networkName);
		cf.putList("network", networks);
		cf.put("writeHtml", "" + writeHtml);

		return cf;
	}

	/**
	 * log settings
	 */
	public String toString() {

		StringBuilder settings = new StringBuilder();

		settings.append("startTime = " + startTime + "\n");
		settings.append("endTime = " + endTime + "\n");
		settings.append("fileNameDateFormat = " + fileNameDateFormat + "\n");
		settings.append("filePathDateFormat = " + filePathDateFormat + "\n");
		settings.append("subnetName = " + subnetName + "\n");
		settings.append("pathRoot = " + pathRoot + "\n");
		settings.append("applicationLaunch = " + applicationLaunch + "\n");
		settings.append("duration = " + duration + "\n");
		settings.append("period = " + period + "\n");
		settings.append("fileSuffix = " + fileSuffix + "\n");
		settings.append("timeZoneName = " + timeZoneName + "\n");
		settings.append("timeZoneOffset = " + timeZoneOffset + "\n");
		settings.append("onError = " + onError + "\n");
		settings.append("embargo = " + embargo + "\n");
		settings.append("networkName = " + networkName + "\n");
		settings.append("writeHtml = " + writeHtml + "\n");
		settings.append("network = " + networks.toString());

		return settings.toString();
	}

	/**
	 * Apply settings to a Thymeleaf Context. Convert path seperator if needed.
	 * 
	 * @param ctx
	 */
	protected void applySettings(Context ctx) {
		ctx.setVariable("startTime", startTime);
		ctx.setVariable("endTime", endTime);
		ctx.setVariable("duration", duration);
		ctx.setVariable("period", period);
		ctx.setVariable("fileNameDateFormat", fileNameDateFormat);
		ctx.setVariable("filePathDateFormat", filePathDateFormat);
		ctx.setVariable("subnetName", subnetName);
		ctx.setVariable("subnetDisplayName", subnetName.replace('_', ' '));
		ctx.setVariable("pathRoot", pathRoot);
		ctx.setVariable("applicationLaunch", applicationLaunch);
		ctx.setVariable("fileSuffix", fileSuffix);
		ctx.setVariable("timeZoneName", timeZoneName);
		ctx.setVariable("timeZoneOffset", timeZoneOffset);
		ctx.setVariable("onError", onError);
		ctx.setVariable("embargo", embargo);
		ctx.setVariable("configFile", configFile);
		ctx.setVariable("settings", this);
		ctx.setVariable("networkName", networkName);
		ctx.setVariable("filePathDateFormat", filePathDateFormat);
		ctx.setVariable("writeHtml", writeHtml);
	}
}
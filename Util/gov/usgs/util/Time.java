package gov.usgs.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

/**
 * A class for dealing with time, especially formatting and J2Ks.
 * 
 * @author Dan Cervelli
 */
public class Time
{
	public static final String INPUT_TIME_FORMAT = "yyyyMMddHHmmss";
	public static final String STANDARD_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
	public static final String STANDARD_TIME_FORMAT_MS = "yyyy-MM-dd HH:mm:ss.SSS";
	public static final String ISO_8601_TIME_FORMAT = "yyyyMMdd'T'HHmmss.SSSS'Z'";
	
	public static final double SECONDSPERYEAR = 31557600;
	public static final double UNIXZERO = -946728000;
	
	private static Map<String, SimpleDateFormat> formats;
	
	static
	{
		formats = new HashMap<String, SimpleDateFormat>();
	}
	
	/**
	 * Formats long date by format name
	 * @param fs Name of format to search
	 * @param t long date to format
	 * @return Formatted date as string
	 */
	public synchronized static String format(String fs, long t)
	{
		return format(fs, (((double)t / (double)1000) + UNIXZERO));
	}
	
	/**
	 * Formats J2K date by format name
	 * @param fs Name of format to search
	 * @param j2k date to format
	 * @return Formatted date as string
	 */
	public synchronized static String format(String fs, double j2k)
	{
		return format(fs, Util.j2KToDate(j2k));
	}
	
	/**
	 * Formats {@link Date} by format name
	 * @param fs Name of format to search
	 * @param d date to format
	 * @return Formatted date as string
	 */
	public synchronized static String format(String fs, Date d)
	{
		return getFormat(fs).format(d);
	}
	
	/**
	 * Formats {@link Date} as "yyyy-MM-dd HH:mm:ss"
	 * @param d date
	 * @return formatted date
	 */
	public static String toDateString(Date d)
	{
		return format(STANDARD_TIME_FORMAT, d);
	}

	/**
	 * Formats {@link Date} as "yyyyMMddHHmmss"
	 * @param d date
	 * @return formatted date
	 */
	public static String toShortString(Date d)
	{
		return format(INPUT_TIME_FORMAT, d);
	}

	/**
	 * Formats J2K date as "yyyy-MM-dd HH:mm:ss"
	 * @param j2k date
	 * @return formatted date
	 */
	public static String toDateString(double j2k)
	{
		return format(STANDARD_TIME_FORMAT, j2k);
	}

	/**
	 * Formats long date as "yyyy-MM-dd HH:mm:ss"
	 * @param t Date
	 * @return formatted date 
	 */
	public static String toDateString(long t)
	{
		return format(STANDARD_TIME_FORMAT, t);
	}

	/**
	 * Searches in internal formats list 
	 * @param fs Format name to search
	 * @return Found SimpleDateFormat or, if absent, initialized with fs string one
	 */
	protected static SimpleDateFormat getFormat(String fs)
	{
		SimpleDateFormat format = formats.get(fs);
		if (format == null)
		{
			format = new SimpleDateFormat(fs);
			format.setTimeZone(TimeZone.getTimeZone("GMT"));
			formats.put(fs, format);
		}
		return format;
	}
	

	/** 
	 * Parse string into J2K date, log errors
	 * @param fs Format name to parse
	 * @param ds date string 
	 * @return parsed J2K date or 0 if exception occurred
	 */
	public synchronized static double parse(String fs, String ds)
	{
		try
		{
			return Util.dateToJ2K(getFormat(fs).parse(ds));
		}
		catch (Exception e)
		{
			System.out.println("Failed to parse time: " + ds + ", format: " + fs);
			e.printStackTrace();
			return 0;
		}
	}
	
	/**
	 * The same as parse(String, String) but throws exception in the case of problems
	 * @param fs
	 * @param ds
	 * @return parsed J2K date or 0 if exception occured
	 * @throws ParseException
	 */
	public synchronized static double parseEx(String fs, String ds) throws ParseException
	{
		return Util.dateToJ2K(getFormat(fs).parse(ds));
	}		
	
	/**
	 * Converts a Valve style relative time to a number of seconds.  First
	 * character must be '-', then a number, then one of these characters:
	 * s, i, h, d, w, y.
	 * 
	 * @param t the time string
	 * @return the number of seconds this represents (or NaN on error)
	 */
	public static double getRelativeTime(String t)
	{
		if (t == null || t.charAt(0) != '-')
			return Double.NaN;
		
		int number = Integer.MIN_VALUE;
		try
		{
			number = Integer.parseInt(t.substring(1, t.length() - 1));
		}
		catch (Exception e)
		{
			return Double.NaN;
		}
		char unit = t.charAt(t.length() - 1);
		double unitSize = 0;
		switch (unit)
		{
			case 's':
				unitSize = 1;
				break;
			case 'i':
				unitSize = 60;
				break;
			case 'h':
				unitSize = 60 * 60;
				break;
			case 'd':
				unitSize = 60 * 60 * 24;
				break;
			case 'w':
				unitSize = 60 * 60 * 24 * 7;
				break;
			case 'm':
				unitSize = 60 * 60 * 24 * 30;
				break;
			case 'y':
				unitSize = 60 * 60 * 24 * 365;
				break;
			default:
				return Double.NaN;
		}
		
		return number * unitSize;
	}
	
	/**
	 * @param timeRange two string dates in "yyyyMMddHHmmss" format or relative time, divided by comma
	 * @return array of two doubles - start and end J2K dates
	 * @throws ParseException
	 */
	public synchronized static double[] parseTimeRange(String timeRange) throws ParseException
	{
		if (timeRange == null || timeRange.equals(""))
			throw new ParseException("Time range is null.", -1);
		
		double[] result = new double[2];
		String[] ss = timeRange.split(",");
		 
		result[1] = CurrentTime.getInstance().nowJ2K();
		result[0] = 0;
		if (ss.length == 2)
		{
			if (ss[1].charAt(0) == '-')
			{
				double rt = Time.getRelativeTime(ss[1]);
				if (Double.isNaN(rt))
					throw new ParseException("Unparsable relative end time.", -1);
				result[1] = result[1] - rt;
			}
			else
				result[1] = Util.dateToJ2K(getFormat(INPUT_TIME_FORMAT).parse(ss[1]));
		}
		if (ss[0].charAt(0) == '-')
		{
			result[0] = Time.getRelativeTime(ss[0]);
			if (Double.isNaN(result[0]))
				throw new ParseException("Unparsable relative start time.", -1);
			result[0] = result[1] - result[0];
		}
		else
			result[0] = Util.dateToJ2K(getFormat(INPUT_TIME_FORMAT).parse(ss[0]));
		
		return result;
	}

	/**
	 * The same as parseTimeRange(String), but doesn't throw ParseException - return 
	 * null in case of problems
	 * @param timeRange
	 * @return array of start and end dates
	 */
	public synchronized static double[] parseTimeRangeQuiet(String timeRange)
	{
		try
		{
			return parseTimeRange(timeRange);
		}
		catch (Exception e)
		{
			return null;
		}
	}

	/**
	 * Gets the time zone offset, determines DST from now.
	 * 
	 * @param tz
	 * @return time zone offset
	 */
	public static double getTimeZoneOffset(TimeZone tz)
	{
		return getTimeZoneOffset(tz, Util.nowJ2K());
	}

	/**
	 * Gets the time zone offset
	 * @param tz
	 * @param dst
	 * @return time zone offset
	 */
	public static double getTimeZoneOffset(TimeZone tz, double dst)
	{
		int dstOffset = 0;
		if (tz.inDaylightTime(Util.j2KToDate(dst)))
			dstOffset = tz.getDSTSavings();

		return (double)(tz.getRawOffset() + dstOffset) / 1000.0;
	}

	/**
	 * Gets the time zone offset
	 * 
	 * @param tz time zone
	 * @param dst flag if we take into account daylight saving time
	 * @return time zone offset
	 */
	public static double getTimeZoneOffset(TimeZone tz, boolean dst)
	{
		int dstOffset = 0;
		if (dst)
			dstOffset = tz.getDSTSavings();

		return (double)(tz.getRawOffset() + dstOffset) / 1000.0;
	}
	
	/**
	 * Main method
	 * @param args command line args
	 */
	public static void main(String[] args)
	{
		if (args.length == 0)
		{
			System.out.println("Args:");
			System.out.println("-d		display default and available ids");
			System.exit(1);
		}

		String cmd = "";
		if (args.length == 1)
			cmd = args[0].toLowerCase();
		
		if (cmd.equals("getzones"))
		{
			System.out.println("Default time zone is " + TimeZone.getDefault().getID());
			System.out.println("Available time zones:");
			String[] zones = TimeZone.getAvailableIDs();
			Arrays.sort(zones);
			for (String s : zones)
				System.out.println("\t" + s);
		}	
	}
}

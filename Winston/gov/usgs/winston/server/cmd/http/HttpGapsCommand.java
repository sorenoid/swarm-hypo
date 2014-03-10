package gov.usgs.winston.server.cmd.http;

import gov.usgs.net.HttpResponse;
import gov.usgs.net.NetTools;
import gov.usgs.util.Util;
import gov.usgs.winston.db.WinstonDatabase;
import gov.usgs.winston.server.WWS;

import java.text.DecimalFormat;
import java.util.List;
import java.util.TimeZone;

/**
 * Returns data gaps, gap duration, and gap analysis.
 * 
 * @author Austin Sedita
 * 
 */

public final class HttpGapsCommand extends AbstractHttpCommand {

	DecimalFormat formatter = new DecimalFormat("#.###");
	double now = Util.ewToJ2K(System.currentTimeMillis() / 1000);

	String error = "";
	String code;
	Double endTime;
	Double startTime;
	String timeZone;
	TimeZone zone;
	Double minGapDuration;
	int writeComputer;

	String startTimeS;
	String endTimeE;
	double totalTime;

	List<double[]> gaps;
	double[] gap;
	int gapCount;

	//double dataStart;
	//double dataEnd;
	//double dataLength;

	String[] color = { "#ffeeee;", "#eeffee;", "#eeeeff;" };
	int colorCount;

	double gapLength;
	double totalGapLength;
	double averageGapLength;
	double dataPercentage;

	public HttpGapsCommand(NetTools nt, WinstonDatabase db, WWS wws) {
		super(nt, db, wws);
	}

	protected void sendResponse() {

		// Station
		code = arguments.get("code");
		if (code == null) {
			error = "Error: you must specify a channel (code).";
		} else {
			code = code.replace('_', '$');
			if (code.indexOf(";") != -1) {
				error = "Error: illegal characters in channel (code).";
			}
		}

		// End Time
		endTime = getEndTime(arguments.get("t2"));
		if (endTime == Double.NaN) {
			error = error + "Error: could not parse end time (t2). Should be "
					+ DATE_FORMAT + ".";
		}

		// Start Time
		startTime = getStartTime(arguments.get("t1"), endTime, ONE_HOUR);
		if (startTime == Double.NaN) {
			error = error
					+ "Error: could not parse start time (t1). Should be "
					+ DATE_FORMAT + " or -HH.";
		}

		// TimeZone
		timeZone = Util.stringToString(arguments.get("tz"), "UTC");
		zone = TimeZone.getTimeZone(timeZone);

		// Minimum Gap Duration
		minGapDuration = Util.stringToDouble(arguments.get("mgd"), 5);

		// Write Computer
		writeComputer = Util.stringToInt(arguments.get("wc"), 0);

		// If Error
		if (error.length() > 0) {
			writeSimpleHTML(error);

		} else {

			// Finds Gaps
			gaps = data.findGaps(code, startTime, endTime);
			code = code.replace('$', '_');
			startTimeS = Util.j2KToDateString(startTime, zone);
			endTimeE = Util.j2KToDateString(endTime, zone);
			totalTime = endTime - startTime;

			if (writeComputer == 1) {
				writeComputer();
			} else {
				writeHuman();
			}
		}
	}

	// Write for Humans
	void writeHuman() {

		StringBuilder header = new StringBuilder();
		StringBuilder outputSettings_Analysis = new StringBuilder();
		//StringBuilder outputData = new StringBuilder();
		StringBuilder outputDataGaps = new StringBuilder();
		StringBuilder footer = new StringBuilder();

		// Write Human Header
		header.append("<HTML><TITLE>Winston Gaps</TITLE>");
		header.append("<BODY><TABLE CELLSPACING=5 CELLPADDING=5 STYLE=\"border-width: 2; border-style: solid;\">"
				+ "<THEAD><TD ALIGN=center COLSPAN=3><BIG><B><U>Winston Gaps</U></B></BIG></TD></THEAD><TBODY><TR>");

		// Write Human Settings
		outputSettings_Analysis
				.append("<TD VALIGN=top><TABLE CELLSPACING=0 CELLPADDING=5 STYLE=\"border-width: 2; border-style: solid;\" WIDTH=400>"
						+ "<THEAD><TR><TH ALIGN=center COLSPAN=2><B>Settings</B></TH></TR></THEAD>");
		outputSettings_Analysis.append("<TBODY><TR><TD>Start Time:</TD><TD>"
				+ startTimeS + "</TD></TR>");
		outputSettings_Analysis.append("<TR><TD>End Time:</TD><TD>" + endTimeE
				+ "</TD></TR>");
		outputSettings_Analysis.append("<TR><TD>Duration:</TD><TD>" + totalTime
				+ " seconds</TD></TR>");
		outputSettings_Analysis.append("<TR><TD>Time Zone:</TD><TD>" + timeZone
				+ "</TD></TR>");
		outputSettings_Analysis.append("<TR><TD>Station Name:</TD><TD>" + code
				+ "</TD></TR>");
		outputSettings_Analysis.append("<TR><TD>Minimum Gap Duration:</TD><TD>"
				+ minGapDuration + " seconds</TD></TR></TBODY></TABLE>");

//		// Write Human Data
//		outputData
//				.append("<TD VALIGN=top><TABLE CELLSPACING=0 CELLPADDING=5 STYLE=\"border-width: 2; border-style: solid;\" WIDTH=500>"
//						+ "<THEAD><TR><TH ALIGN=center COLSPAN=3><B>Data</B></TH></TR></THEAD>");
//		outputData
//				.append("<TR><TH>Data Start Time:</TH><TH>Data End Time:</TD><TH>Data Duration:</TH></TR>");
//		outputData.append("<TR STYLE=\"background: #ffeeee;\"><TD>"
//				+ startTimeS + "</TD>");
//		colorCount = 1;
//		dataStart = startTime;
//
//		// Listing of Data
//		for (double[] gap : gaps) {
//			gapLength = gap[1] - gap[0];
//			dataEnd = gap[0];
//
//			dataLength = dataEnd - dataStart;
//			if (gapLength < minGapDuration) {
//				continue;
//			} else {
//				outputData.append("<TD>" + Util.j2KToDateString(gap[0], zone)
//						+ "</TD>");
//				outputData.append("</TD><TD ALIGN=right>"
//						+ formatter.format(dataLength) + " seconds</TD>");
//				outputData.append("<TR STYLE=\"background: "
//						+ color[colorCount++ % 3] + "\"><TD>"
//						+ Util.j2KToDateString(gap[1], zone));
//			}
//			dataStart = gap[1];
//		}
//		dataLength = endTime - dataStart;
//		outputData.append("</TD><TD>" + endTimeE + "</TD>"
//				+ "</TD><TD ALIGN=right>" + formatter.format(dataLength)
//				+ " seconds</TD>" + "</TR></TABLE></TD>");

		// Write Human Data Gaps
		outputDataGaps
				.append("<TD VALIGN=top><TABLE CELLSPACING=0 CELLPADDING=5 STYLE=\"border-width: 2; border-style: solid;\" WIDTH=500>"
						+ "<THEAD><TR><TH ALIGN=center COLSPAN=3><B>Data Gaps</B></TH></TR></THEAD>");
		outputDataGaps
				.append("<TR><TH>Gap Start Time</TH><TH>Gap End Time</TH><TH>Gap Duration</TH></TR>");
		gapCount = 0;
		colorCount = 0;
		totalGapLength = 0;

		// Listing of Gaps
		for (double[] gap : gaps) {
			gapLength = gap[1] - gap[0];
			if (gapLength < minGapDuration) {
				continue;
			} else {
				outputDataGaps.append("<TR STYLE=\"background: "
						+ color[colorCount++ % 3] + "\"><TD>"
						+ Util.j2KToDateString(gap[0], zone) + "</TD><TD>"
						+ Util.j2KToDateString(gap[1], zone)
						+ "</TD><TD ALIGN=right>" + formatter.format(gapLength)
						+ " seconds</TD></TR>");
				gapCount++;
				totalGapLength = totalGapLength + gapLength;
			}
		}

		averageGapLength = totalGapLength / gapCount;
		dataPercentage = (totalTime - totalGapLength) / totalTime * 100;
		outputDataGaps.append("</TABLE>");

		// Write Human Analysis
		outputSettings_Analysis
				.append("<TABLE CELLSPACING=0 CELLPADDING=5 STYLE=\"border-width: 2; border-style: solid;\" WIDTH=400>"
						+ "<THEAD><TR><TH ALIGN=center COLSPAN=2 ><B>Analysis</B></TH></TR></THEAD>");
		outputSettings_Analysis.append("<TR><TD>Number of Gaps:</TD><TD>"
				+ gapCount + "</TD></TR>");
		outputSettings_Analysis.append("<TR><TD>Total Gap Length:</TD><TD>"
				+ formatter.format(totalGapLength) + "seconds</TD></TR>");
		outputSettings_Analysis.append("<TR><TD>Average Gap Length:</TD><TD>"
				+ formatter.format(averageGapLength) + "seconds</TD></TR>");
		outputSettings_Analysis
				.append("<TR><TD>Data percentage:</TD><TD>"
						+ formatter.format(dataPercentage)
						+ "%</TD></TR></TABLE></TD>");

		// Write Human Footer
		footer.append("</TD></TR></TBODY><TFOOT></TFOOT</TABLE></BODY></HTML>");

		// All or No Data
		if (gapCount == 0) {
			writeSimpleHTML("No Gaps for selected time frame.");
		} else if (totalGapLength == totalTime) {
			writeSimpleHTML("No Data for selected time frame.");
		}

		// Website Creation
		String html = header.toString() + outputSettings_Analysis.toString()
				+ /*outputData.toString() + */ outputDataGaps.toString()
				+ footer.toString();
		HttpResponse response = new HttpResponse("text/html");
		response.setLength(html.length());
		netTools.writeString(response.getHeaderString(), channel);
		netTools.writeString(html, channel);
	}

	// Write for Computers
	void writeComputer() {

		StringBuilder header = new StringBuilder();
		StringBuilder output = new StringBuilder();
		output.append("# Gap Start\t\tGap End\t\t\tDuration\n");

		// Write Computer Data Gaps
		gapCount = 0;
		totalGapLength = 0;

		for (double[] gap : gaps) {
			gapLength = gap[1] - gap[0];
			if (gapLength < minGapDuration) {
				continue;
			} else {
				output.append(gap[0] + "\t" + gap[1] + "\t" + gapLength + "\n");
				gapCount++;
				totalGapLength = totalGapLength + gapLength;
			}
		}

		// Write Computer Winston Gaps
		header.append("# Start Time: " + startTimeS + " (" + startTime
				+ ")\n# End Time: " + endTimeE + " (" + endTime
				+ ")\n# Total Time: " + totalTime + "\n# Time Zone: "
				+ timeZone + "\n# Station: " + code
				+ "\n# Minumum Gap Duration: " + minGapDuration
				+ "\n# GapCount: " + gapCount + "\n");

		writeSimpleText(header.toString() + output.toString());
	}

	// Winston Wave Server Interface
	public String getUsage() {
		StringBuilder output = new StringBuilder();
		output.append("You can construct urls that return data, data gaps, and analysis as follows: <br><br>"
				+ "<code>http://&lt;hostname&gt;:&lt;port&gt;/gaps?code=PS4A_EHZ_AV&t1=-24&t2=now&tz=US/Alaska&mgd=30&wc=0</code><br><br>"
				+ "This url will return data, data gaps, and analysis for the channel PS4A EHZ AV, for the last 24 hours, in Alaskan time, with a minimum gap duration of 30 seconds, written for humans.<br>"
				+ "The options (separated by the & character, all optional except for code ) are defined as follows<br><br>"
				+ "code: <b>Station Name</b> The name of the Station desired<br><br>"
				+ "t1: <b>Start Time</b> The start time (local) of the gap analysis as given by the number of hours before present or a specific time in the format YYYYMMDDHHMM. "
				+ "Note that, in the first case, this is a negative number (default = -12).<br><br>"
				+ "t2: <b>End Time</b> The end time (local) of the gap analysis as given by the format YYYYMMDDHHMM or 'now' (default = 'now').<br><br>"
				+ "tz: <b>Time Zone</b> The time zone, a complete list of time zones that WWS understands is shown below (default = UTC).<br><br>"
				+ "mgd: <b>Minimumm Gap Duration</b> The minimum gap duration desired in seconds (default = 5)<br><br>"
				+ "wc: <b>Write Computer</b> Whether to show data gaps as the computer sees, 1 is yes, 0 is no (default = 0)");
		return output.toString();
	}

	public String getAnchor() {
		return "gaps";
	}

	public String getTitle() {
		return "WWS Gaps";
	}

	public String getCommand() {
		return "/gaps";
	}
}

package gov.usgs.winston.server.cmd.http;

import gov.usgs.net.ConnectionStatistics;
import gov.usgs.net.HttpResponse;
import gov.usgs.net.NetTools;
import gov.usgs.util.Util;
import gov.usgs.winston.Channel;
import gov.usgs.winston.db.Channels;
import gov.usgs.winston.db.WinstonDatabase;
import gov.usgs.winston.server.WWS;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Return server status. Mostly intended to give an overview of data freshness.
 * 
 * @author Tom Parker
 * 
 */
public final class HttpStatusCommand extends AbstractHttpCommand {

	public HttpStatusCommand(NetTools nt, WinstonDatabase db, WWS wws) {
		super(nt, db, wws);
	}

	protected void sendResponse() {
		DecimalFormat formatter = new DecimalFormat("#.##");
		double now = Util.ewToJ2K(System.currentTimeMillis() / 1000);

		Channels channels = new Channels(winston);
		List<Channel> sts = channels.getChannelsByLastInsert();
		for (int i = 0; i < sts.size(); i++) {
			Channel chan = sts.get(i);
			if (wws.getMaxDays() > 0
					&& chan.getMaxTime() < now - (wws.getMaxDays() * (60 * 60 * 24))) {
				sts.remove(i);
				i--;
			}
		}

		Collection<ConnectionStatistics> css = wws.getConnectionStats();

		double medianDataAge = now - sts.get(sts.size() / 2).getMaxTime();

		ArrayList<String> oneMinChannels = new ArrayList<String>();
		ArrayList<String> fiveMinChannels = new ArrayList<String>();
		ArrayList<String> oneHourChannels = new ArrayList<String>();
		ArrayList<String> oneDayChannels = new ArrayList<String>();
		ArrayList<String> oneMonthChannels = new ArrayList<String>();
		ArrayList<String> ancientChannels = new ArrayList<String>();

		for (Channel chan : sts) {
			double age = now - chan.getMaxTime();
			String code = chan.getCode().replace('$', '_');
			if (age < 60)
				oneMinChannels.add(code);
			else if (age <= 60 * 5)
				fiveMinChannels.add(code);
			else if (age <= 60 * 60)
				oneHourChannels.add(code);
			else if (age <= 60 * 60 * 24)
				oneDayChannels.add(code);
			else if (age <= 60 * 60 * 24 * 7 * 4)
				oneMonthChannels.add(code);
			else
				ancientChannels.add(code);
		}

		StringBuilder output = new StringBuilder();
		output.append("<HTML><HEAD><TITLE>Winston Status</TITLE>\n"
				+ "<script language=\"javascript\" type=\"text/javascript\">\n"
				+ "<!-- \n function popup(title, stations) { \n"
				+ "newwindow=window.open('','','width=300,height=500,scrollbars=1,resizable=1');\n"
				+ "var tmp = newwindow.document;\n"
				+ "tmp.write('<html><head><title>title</title></head>');\n"
				+ "tmp.write('<body><B>' + title + '</B><pre>');\n"
				+ "for (var i=0; i <stations.length; i++)\n"
				+ "tmp.write(stations[i] + \"\\n\");\n" + "tmp.write('</pre></body></html>');\n"
				+ "tmp.close();\n }\n" + "// -->" + "</script></HEAD><BODY>\n");

		output.append("<SCRIPT type=\"text/javascript\">");
		output.append("var count1Title = \"&le; 1 minute old\";");
		output.append("var count1 = new Array(");
		for (String s : oneMinChannels)
			output.append("\"" + s + "\",");
		output.append("\"\");");

		output.append("var countFreshTitle = \"&gt; 1 minute &and; &le; 5 minutes old\";");
		output.append("var countFresh = new Array(");
		for (String s : fiveMinChannels)
			output.append("\"" + s + "\",");
		output.append("\"\");");

		output.append("var count5Title = \"&gt; 5 minutes &and; &le; 1 hour old\";");
		output.append("var count5 = new Array(");
		for (String s : oneHourChannels)
			output.append("\"" + s + "\",");
		output.append("\"\");");

		output.append("var countHourTitle = \"&gt; 1 hour &and; &le; 1 day old\";");
		output.append("var countHour = new Array(");
		for (String s : oneDayChannels)
			output.append("\"" + s + "\",");
		output.append("\"\");");

		output.append("var countDayTitle = \"> 1 day &and; &le; 4 weeks old\";");
		output.append("var countDay = new Array(");
		for (String s : oneMonthChannels)
			output.append("\"" + s + "\",");
		output.append("\"\");");

		output.append("var countMonthTitle = \"> 4 weeks old\";");
		output.append("var countMonth = new Array(");
		for (String s : ancientChannels)
			output.append("\"" + s + "\",");
		output.append("\"\");");

		output.append("</SCRIPT>");

		int count;
		output.append("<TABLE><TR><TD VALIGN=top>");
		output.append("<TABLE CELLSPACING=0 CELLPADDING=5 STYLE=\"border-width: 2; border-style: solid;\"><TR STYLE=\"background: #eeffee;\"><TD ALIGN=center COLSPAN=2><B>Winston Status</B></TD></TR>");
		output.append("<TR STYLE=\"background: #eeeeff;\"><TD>channel count</TD><TD><A HREF=\"/menu\">"
				+ sts.size() + "</A></TD></TR>");
		output.append("<TR><TD>connection count</TD><TD>" + css.size() + "</TD></TR>");
		output.append("<TR STYLE=\"background: #eeeeff;\"><TD>median data age</TD><TD>"
				+ formatter.format(medianDataAge) + " seconds</TD></TR>");
		Channel chan = sts.get(0);
		output.append("<TR><TD>most recent</TD><TD>" + chan.getCode().replace('$', '_') + " "
				+ formatter.format(now - chan.getMaxTime()) + " seconds ago</TD></TR>");
		output.append("<TR STYLE=\"background: #eeffee;\"><TD ALIGN=center COLSPAN=2><B>Data Freshness</B></TD></TR>");
		count = oneMinChannels.size();
		output.append("<TR STYLE=\"background: #eeeeff;\"><TD>&le; 1 minute</TD><TD><A HREF=\"javascript:popup(count1Title,count1);\">"
				+ count
				+ " channels</A> ("
				+ formatter.format((double) 100 * count / sts.size())
				+ "%)</TD></TR>");
		count = fiveMinChannels.size();
		output.append("<TR><TD>&le; 5 minutes</TD><TD><A HREF=\"javascript:popup(countFreshTitle,countFresh);\">"
				+ count
				+ " channels</A> ("
				+ formatter.format((double) 100 * count / sts.size())
				+ "%)</TD></TR>");
		count = oneHourChannels.size();
		output.append("<TR STYLE=\"background: #eeeeff;\"><TD>&le; 1 hour</TD><TD><A HREF=\"javascript:popup(count5Title,count5);\">"
				+ count
				+ " channels</A> ("
				+ formatter.format((double) 100 * count / sts.size())
				+ "%)</TD></TR>");
		count = oneDayChannels.size();
		output.append("<TR><TD>&le; 1 day</TD><TD><A HREF=\"javascript:popup(countHourTitle,countHour);\">"
				+ count
				+ " channels</A> ("
				+ formatter.format((double) 100 * count / sts.size())
				+ "%)</TD></TR>");
		count = oneMonthChannels.size();
		output.append("<TR STYLE=\"background: #eeeeff;\"><TD>&le; 4 weeks</TD><TD><A HREF=\"javascript:popup(countDayTitle,countDay);\">"
				+ count
				+ " channels</A> ("
				+ formatter.format((double) 100 * count / sts.size())
				+ "%)</TD></TR>");
		count = ancientChannels.size();
		output.append("<TR><TD>&gt; 4 weeks</TD><TD><A HREF=\"javascript:popup(countMonthTitle,countMonth);\">"
				+ count
				+ " channels</A> ("
				+ formatter.format((double) 100 * count / sts.size())
				+ "%)</TD></TR>");
		output.append("</TABLE></TD><TD VALIGN=top>");
		output.append("<TABLE CELLSPACING=0 CELLPADDING=5 STYLE=\"border-width: 2; border-style: solid;\"><TR STYLE=\"background: #eeffee;\"><TD ALIGN=center COLSPAN=2><B>"
				+ (oneDayChannels.size() + oneHourChannels.size())
				+ " channels between <BR>5 minutes and 24 hours old</B></TD></TR>");

		int i = 0;
		for (Channel chan1 : sts) {
			double a = now - chan1.getMaxTime();
			if (a > (5 * 60) && a < (60 * 60 * 24)) {
				String bg = i++ % 2 == 0 ? "#eeeeff;" : "#ffffff;";
				output.append("<TR STYLE=\"background: " + bg + "\"><TD ALIGN=right>"
						+ chan1.getCode().replace('$', '_') + "</TD><TD>" + ((int) a / 60)
						+ " minutes</TD></TR>");
			}
		}

		output.append("</TR></TABLE></TD></TR></TABLE>");
		output.append("</BODY></HTML>");

		String html = output.toString();
		HttpResponse response = new HttpResponse("text/html");
		response.setLength(html.length());
		netTools.writeString(response.getHeaderString(), channel);
		netTools.writeString(html, channel);

	}

	public String getUsage() {
		StringBuilder output = new StringBuilder();
		output.append("You can construct urls that return the server status as follows:");
		output.append("<br><br><code>http://&lt;hostname&gt;:&lt;port&gt;/status</code><br><br>");
		output.append("</code>This url will return the status of this wave server.");
		return output.toString();
	}

	public String getAnchor() {
		return "status";
	}

	public String getTitle() {
		return "WWS Status";
	}

	public String getCommand() {
		return "/status";
	}
}

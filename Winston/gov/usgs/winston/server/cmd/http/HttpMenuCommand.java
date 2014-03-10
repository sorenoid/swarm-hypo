package gov.usgs.winston.server.cmd.http;

import gov.usgs.net.NetTools;
import gov.usgs.util.Util;
import gov.usgs.winston.db.WinstonDatabase;
import gov.usgs.winston.server.WWS;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.TimeZone;

/**
 * Return the wave server menu. Similar to earthworm getmenu command.
 * 
 * @author Tom Parker
 * 
 */
public final class HttpMenuCommand extends AbstractHttpCommand {

	public HttpMenuCommand(NetTools nt, WinstonDatabase db, WWS wws) {
		super(nt, db, wws);
	}

	protected void sendResponse() {
		// validate input. Write error and return if bad.
		int sortCol = Util.stringToInt(arguments.get("ob"), 1);
		if (sortCol < 1 || sortCol > 8) {
			writeSimpleHTML("Error: could not parse ob = " + arguments.get("ob"));
			return;
		}

		String o = Util.stringToString(arguments.get("so"), "a");
		char order = o.charAt(0);
		if (order != 'a' && order != 'd') {
			writeSimpleHTML("Error: could not parse so = " + arguments.get("so"));
			return;
		}

		String tz = Util.stringToString(arguments.get("tz"), "UTC");
		TimeZone timeZone = TimeZone.getTimeZone(tz);

		// write header
		String[] colTitle = { null, "Pin", "S", "C", "N", "L", "Earliest", "Most Recent", "Type" };
		colTitle[sortCol] += order == 'a' ? " &#9652;" : " &#9662;";

		char[] colOrd = new char[colTitle.length];
		Arrays.fill(colOrd, 'a');
		if (order == 'a')
			colOrd[sortCol] = 'd';

		StringBuilder output = new StringBuilder();

		output.append("<table CELLPADDING=\"5\"><tr>");
		for (int i = 1; i < colTitle.length; i++)
			output.append("<th><a href=\"?ob=" + i + "&so=" + colOrd[i] + "\">" + colTitle[i]
					+ "</a></th>");

		output.append("</tr>");

		// get and sort menu
		List<String> list = emulator.getWaveServerMenu(true, 0, 0, maxDays);
		String[][] menu = new String[list.size()][8];
		int i = 0;
		for (String s : list)
			menu[i++] = s.split("\\s");

		Arrays.sort(menu, getMenuComparator(sortCol, order));

		// display menu items
		for (String[] line : menu) {
			if (line.length < 8) {
				output.append("can't parse line, skipping. " + line);
				continue;
			}

			double start = Double.parseDouble(line[6]);
			double end = Double.parseDouble(line[7]);

			output.append("<tr>");
			output.append("<td>" + line[1] + "</td>");
			output.append("<td>" + line[2] + "</td>");
			output.append("<td>" + line[3] + "</td>");
			output.append("<td>" + line[4] + "</td>");
			output.append("<td>" + line[5] + "</td>");
			output.append("<td>" + Util.j2KToDateString(Util.ewToJ2K(start), timeZone) + "</td>");
			output.append("<td>" + Util.j2KToDateString(Util.ewToJ2K(end), timeZone) + "</td>");
			output.append("<td>" + line[8] + "</td>");
			output.append("</tr>\n");
		}

		output.append("</table>");
		writeSimpleHTML(output.toString());
	}

	private Comparator<String[]> getMenuComparator(final int sortCol, final char order) {
		return new Comparator<String[]>() {
			public int compare(final String[] e1, final String[] e2) {

				// numeric columns
				if (sortCol == 1 || sortCol == 6 || sortCol == 7) {
					double d1 = Double.parseDouble(e1[sortCol]);
					double d2 = Double.parseDouble(e2[sortCol]);
					if (order == 'a')
						return (int) (d1 - d2);
					else
						return (int) (d2 - d1);
				}
				// textual columns
				else {
					if (order == 'a')
						return e1[sortCol].compareTo(e2[sortCol]);
					else
						return e2[sortCol].compareTo(e1[sortCol]);
				}
			}
		};
	}

	public String getUsage() {
		StringBuilder output = new StringBuilder();
		output.append("You can construct urls that return the server menu as follows:");
		output.append("<br><br><code>http://&lt;hostname&gt;:&lt;port&gt;/menu</code><br><br>");
		output.append("</code>This url will return the menu for this wave server."
				+ "The options (separated by the & character, all optional) are defined as follows:<br><br>"
				+ "<code>ob</code>: <b>Order By</b> The column number used to order the menu(default = 1).<br><br>"
				+ "<code>so</code>: <b>Sort Order</b> How to order the menu, a is ascending, d is decending (default = a).<br><br>"
				+ ""
				+ "<code>tz</code>: <b>Time Zone</b> The time zone, a complete list of time zones that WWS understands is shown below.<br><br>");
		return output.toString();
	}

	public String getAnchor() {
		return "menu";
	}

	public String getTitle() {
		return "Wave Server Menu";
	}

	public String getCommand() {
		return "/menu";
	}
}

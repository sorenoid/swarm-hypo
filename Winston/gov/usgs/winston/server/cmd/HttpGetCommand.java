package gov.usgs.winston.server.cmd;

import gov.usgs.net.CommandHandler;
import gov.usgs.net.HttpRequest;
import gov.usgs.net.HttpResponse;
import gov.usgs.net.NetTools;
import gov.usgs.winston.db.WinstonDatabase;
import gov.usgs.winston.server.WWS;
import gov.usgs.winston.server.cmd.http.AbstractHttpCommand;
import gov.usgs.winston.server.cmd.http.HttpGapsCommand;
import gov.usgs.winston.server.cmd.http.HttpHeliCommand;
import gov.usgs.winston.server.cmd.http.HttpMenuCommand;
import gov.usgs.winston.server.cmd.http.HttpRsamCommand;
import gov.usgs.winston.server.cmd.http.HttpStatusCommand;

import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.TimeZone;
import java.util.logging.Level;

/**
 * Parse http get request and pass to correct sub-command
 * 
 * @author Dan Cervelli
 */
public class HttpGetCommand extends BaseCommand {

	private CommandHandler commandHandler;
	private LinkedHashMap<String, AbstractHttpCommand> httpCommands;

	public HttpGetCommand(NetTools nt, WinstonDatabase db, WWS wws, CommandHandler ch) {
		super(nt, db, wws);
		commandHandler = ch;
		httpCommands = new LinkedHashMap<String, AbstractHttpCommand>();

		addCommand(new HttpMenuCommand(nt, db, wws));
		addCommand(new HttpHeliCommand(nt, db, wws));
		addCommand(new HttpRsamCommand(nt, db, wws));
		addCommand(new HttpStatusCommand(nt, db, wws));
		addCommand(new HttpGapsCommand(nt, db, wws));
	}

	private void addCommand(AbstractHttpCommand cmd) {
		httpCommands.put(cmd.getCommand(), cmd);
	}

	public void doCommand(Object info, SocketChannel channel) {
		String cmd = (String) info;
		HttpRequest request = new HttpRequest(cmd);

		wws.log(Level.FINER, cmd, channel);

		AbstractHttpCommand command = httpCommands.get(request.getFile());
		if (command != null)
			command.respond(cmd, channel, request);
		else
			sendUsage(channel);

		if (channel.isOpen())
			commandHandler.closeConnection();
	}

	private void sendUsage(SocketChannel channel) {
		StringBuilder output = new StringBuilder(64000);
		output.append("<html><body>");
		output.append("This is a Winston Wave Server.<br><br>I can create:");
		output.append("<ul>");
		for (AbstractHttpCommand cmd : httpCommands.values())
			output.append("<li><a href=\"#" + cmd.getAnchor() + "\">" + cmd.getTitle()
					+ "</a></li>");

		output.append("</ul>");
		output.append("The <a href=\"#timeZones\">time zones</a> I know are at the bottom of this page.");
		output.append("<P><HR>");
		for (AbstractHttpCommand cmd : httpCommands.values()) {
			output.append("<h2><a id=\"" + cmd.getAnchor() + "\">" + cmd.getTitle() + "</a></h2>");
			output.append(cmd.getUsage() + "<P><HR>");
		}

		output.append("<h2><a id=\"timeZones\">Time Zones</a></h2>");
		String[] tzs = TimeZone.getAvailableIDs();
		Arrays.sort(tzs);
		for (String tz : tzs) {
			output.append(tz);
			output.append("<br>");
		}
		output.append("<p><hr><b>" + WWS.getVersion() + "</b>");
		output.append("</body></html>");
		
		String html = output.toString();
		HttpResponse response = new HttpResponse("text/html");
		response.setLength(html.length());
		netTools.writeString(response.getHeaderString(), channel);
		netTools.writeString(html, channel);
	}
}
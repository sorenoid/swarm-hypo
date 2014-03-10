package gov.usgs.winston.monitor.server;

import gov.usgs.net.CommandHandler;
import gov.usgs.net.HttpRequest;
import gov.usgs.net.HttpResponse;
import gov.usgs.net.NetTools;
import gov.usgs.winston.monitor.Collector;
import gov.usgs.winston.monitor.MonitorServer;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;

/**
 * 
 * @author Tom Parker
 */
public class HttpGetCommand extends BaseCommand
{
		private CommandHandler commandHandler;
		private String defaultWWS;
		private String title;
		
		public HttpGetCommand(NetTools nt, MonitorServer mon, CommandHandler cm)
		{
			super(nt, mon);
			commandHandler = cm;
			defaultWWS = mon.defaultWWS;
			title = mon.title;
		}
		
		public void writeSimpleHTML(String msg, SocketChannel channel)
		{
			String html = "<html><body>" + msg + "</body></html>";
			HttpResponse response = new HttpResponse("text/html");
			response.setLength(html.length());
			netTools.writeString(response.getHeaderString(), channel);
			netTools.writeString(html, channel);
		}
		
		private void sendDashboard(SocketChannel channel)
		{
			StringBuilder output = new StringBuilder(64000);
			output.append("<html><head>");
			
			output.append("<script type=\"text/javascript\">");
			output.append("function showMenu() { \n");
			output.append("var collector=document.getElementById(\"collectorBox\");\n");
			output.append("for (var i=0; i<collector.length; i++) {\n document.getElementById(collector.options[i].value).style.display=\"none\";}\n");
			output.append("document.getElementById(collector.options[collector.selectedIndex].value).style.display=\"block\";\n}");
			output.append("</script></head><body onLoad=\"showMenu();\" style=\"height: 100%;\">");
					
			output.append("<DIV style=\"border: solid #2c5700 thin;text-align: center; background: #75d781; padding: .5em;\">" + title + "</DIV><P>\n");
			output.append("<DIV style=\"float: left;width: 25%;height: 100%\">\n");
			output.append("<FORM ACTION=\"/plotPane\" METHOD=\"GET\" target=\"plotFrameN\">\n");
			output.append("Collector: <SELECT id=\"collectorBox\" name=\"collector\" onChange=showMenu()>\n");
			for (Collector c : mon.collectors)
				output.append(String.format("<OPTION NAME=\"%s\" VALUE=\"%s\" %s>%s</OPTION>\n", 
				c.getName(),
				c.getName(),
				c.getName().equals(defaultWWS) ? "SELECTED" : "",
				c.getName()));

			output.append("</SELECT>\n");
			output.append("<DIV style=\"border: solid #2c5700 thin;padding: .5em; margin: 1em 1em 1em 0em;\">\n");
			for (Collector c : mon.collectors)
				output.append("<DIV style=\"display: none;\" id=\"" + c.getName() + "\">" + c.getMenu() + "</DIV>\n");
			
			output.append("</DIV>\n");
			output.append("<INPUT TYPE=\"submit\" VALUE=\"create plot\">");
			output.append("</FORM>\n");
			output.append("</DIV>\n");
			output.append("<DIV style=\"float: right;width: 75%;height: 100%;border: none\">\n");
			output.append("<IFRAME name=\"plotFrameN\" id=\"plotFrame\" frameBorder=0 style=\"width: 100%; height: 100%; border: none;padding: 0px; margin: 0px;\" SRC=\"/plotPane?collector=" + defaultWWS + "&" + defaultWWS + "_timeSpan=-1w&" + defaultWWS + "_type=plot\">");
			output.append("</DIV>\n");
			output.append("</body></html>");
			
			HttpResponse response = new HttpResponse("text/html");
			response.setLength(output.length());
			netTools.writeString(response.getHeaderString(), channel);
			netTools.writeString(output.toString(), channel);
		}

		
		private void sendPlot(HttpRequest request, SocketChannel channel)
		{
			Collector c = mon.getCollector(request.getArguments().get("collector"));
			try
			{
				byte[] png = c.createPlot(request);
				HttpResponse response = new HttpResponse("image/png");
				response.setVersion(request.getVersion());
				response.setLength(png.length);
				netTools.writeString(response.getHeaderString(), channel);
				netTools.writeByteBuffer(ByteBuffer.wrap(png), channel);
			}
			catch (Exception e)
			{
				writeSimpleHTML("Error: " + e.getMessage(), channel);
			}
		}

		private void sendPlotPane(HttpRequest request, SocketChannel channel)
		{
			Collector c = mon.getCollector(request.getArguments().get("collector"));
			try
			{
				HttpResponse response = new HttpResponse("text/html");
				response.setVersion(request.getVersion());
				
				StringBuilder output = new StringBuilder();
				output.append("<HTML><HEAD>");
				output.append("<script type=\"text/javascript\">");
				output.append("function resizeIframe() {\n");
				output.append("var iframe = window.parent.document.getElementById(\"plotFrame\");");
				output.append("var container = document.getElementById(\"plotPane\");");
				output.append("iframe.style.height = (container.offsetHeight+40) + \"px\";");
				output.append("}</script></HEAD><BODY onload=\"resizeIframe();\">\n");
				output.append("<DIV id=\"plotPane\" style=\"margin: 0; padding: 0;height: 100%; width:100%;\">");
				output.append(c.createPlotPane(request));
				output.append("</DIV></BODY></HTML>");
				
				response.setLength(output.length());
				netTools.writeString(response.getHeaderString(), channel);
				netTools.writeString(output.toString(), channel);
			}
			catch (Exception e)
			{
				writeSimpleHTML("Error: " + e.getMessage(), channel);
			}
		}

		public void doCommand(Object info, SocketChannel channel)
		{
			try
			{
				String cmd = (String)info;
				mon.log(Level.FINER, cmd, channel);
				HttpRequest request = new HttpRequest(cmd);
				if (request == null || request.getFile() == null)
					mon.log(Level.INFO, "malformed HTTP request", channel);
				else if (request.getFile().equals("/"))
					sendDashboard(channel);
				else if (request.getFile().equals("/img"))
					sendPlot(request, channel);
				else if (request.getFile().equals("/plotPane"))
					sendPlotPane(request, channel);
			}
			finally
			{
				if (channel.isOpen())
					commandHandler.closeConnection();
			}
		}
	}

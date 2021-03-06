package gov.usgs.vdx.server;

import gov.usgs.net.NetTools;
import gov.usgs.util.CodeTimer;
import gov.usgs.vdx.ExportConfig;
import gov.usgs.vdx.data.DataSource;
import gov.usgs.vdx.data.DataSourceDescriptor;
import gov.usgs.vdx.data.DataSourceHandler;

import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import java.util.ArrayList;

/**
 * Comand to retrieve data. 
 * Contains 'source' parameter do determine data source,
 * and parameters set to determine requested data.
 *  
 * 
 * $Log: not supported by cvs2svn $
 * Revision 1.2  2005/08/28 19:11:44  dcervelli
 * Fixes for new CodeTimer.
 *
 * Revision 1.1  2005/08/26 20:39:00  dcervelli
 * Initial avosouth commit.
 *
 * @author Dan Cervelli
 */
public class GetDataCommand extends BaseCommand
{
	/**
	 * Constructor
	 * @param sh server handler
	 * @param nt net tools
	 */
	public GetDataCommand(ServerHandler sh, NetTools nt)
	{
		super(sh, nt);
	}
	
	/**
	 * Perform command actions, write result to channel
	 * @param info params
	 * @param channel where to write to
	 */
	public void doCommand(Object info, SocketChannel channel)
	{
		CodeTimer ct = new CodeTimer("send");
		parseParams((String)info);
		String source = inParams.get("source");
		if (source == null)
		{
			sendError("source not specified", "getdata", channel);
			return;
		}
		DataSourceHandler dsh = handler.getDataSourceHandler();
		String resultType;
		RequestResult result;
		String action = inParams.get("action");
		if ( action!=null && action.equals("exportinfo") ) {
			ExportConfig ec = dsh.getExportConfig( source );
			resultType = action;
			if ( ec == null || !ec.isClosed() ) {
				int ncl = Integer.parseInt( inParams.get("numCommentLines") );
				ArrayList<String> args = new ArrayList<String>(ncl+4);
				args.add( inParams.get("exportable") );
				args.add( inParams.get("width.0") );
				args.add( inParams.get("width.1") );
				//args.add( ""+ncl );
				for ( int i = 1; i <= ncl; i++ )
					args.add( inParams.get("cmt."+i) );
				ExportConfig new_ec = new ExportConfig( args );
				if ( ec == null ) {
					ec = new_ec;
					dsh.putExportConfig( source, ec );
				} else
					ec.underride( new_ec );
				ec.setClosed();
			}
			result = new TextResult( ec.toStringList() );
		} else {
			DataSourceDescriptor dsd = dsh.getDataSourceDescriptor(inParams.get("source"));
			DataSource ds = dsd.getDataSource();
			result = ds.getData(inParams);
			dsd.putDataSource();
			resultType = ds.getType();
		}
		if (result != null)
		{
			result.set("type", resultType);
			result.prepare();
			result.writeHeader(netTools, channel);
			result.writeBody(netTools, channel);
			ct.stop();
			handler.log(Level.FINE, String.format("%s (%1.2f ms): [%s]", inParams.get("source"), ct.getRunTimeMillis(), info), channel);
		}
		else
		{
			netTools.writeString("error: no data\n", channel);
			handler.log(Level.FINE, "[getdata] returned nothing", channel);
		}
	}
}

package gov.usgs.winston.in;

import gov.usgs.util.Arguments;
import gov.usgs.util.Util;
import gov.usgs.vdx.data.wave.Seed;
import gov.usgs.vdx.data.wave.Wave;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * $Log: not supported by cvs2svn $
 * Revision 1.2  2005/09/23 19:20:07  dcervelli
 * Changes for list of filenames instead of array of arguments.
 *
 * Revision 1.1  2005/08/26 20:37:08  dcervelli
 * Initial avosouth commit.
 *
 * Revision 1.2  2005/05/11 22:17:07  cervelli
 * Now extends StaticImporter.
 *
 * Revision 1.1  2005/05/10 06:14:54  cervelli
 * Initial commit.
 *
 * @author Dan Cervelli
 */
public class ImportSeed extends StaticImporter
{	
	private String channel;
	
	public Map<String, List<Wave>> readFile(String fn)
	{
		Map<String, List<Wave>> map;
		if (channel == null)
			map = Seed.readSeed(fn);
		else 
		{
			map = new HashMap<String, List<Wave>>();
			map.put(channel, Seed.readMiniSeed(fn));
		}
		
		return map;
	}
	
	protected Set<String> getArgumentSet()
	{
		Set<String> kvs = new HashSet<String>();
		kvs.add("-rd");
		kvs.add("-rl");
		kvs.add("-c");
		return kvs;
	}
	
	public void processArguments(Arguments args)
	{
		String rd = args.get("-rd");
		rsamDelta = Util.stringToInt(rd, 10);
		String rl = args.get("-rl");
		rsamDuration = Util.stringToInt(rl, 60);
		channel = args.get("-c");
		System.out.printf("RSAM parameters: delta=%d, duration=%d.\n", rsamDelta, rsamDuration);
	}
	
	public static void main(String[] as)
	{
		instructions.append("Winston ImportSeed\n\n");
		instructions.append("This program imports data from SEED volumes into a Winston database.\n");
		instructions.append("Information about connecting to the Winston database must be present\n");
		instructions.append("in Winston.config in the current directory.\n\n");
		instructions.append("Usage:\n");
		instructions.append("  java gov.usgs.winston.in.ImportSeed [files]\n");
		
		ImportSeed is = new ImportSeed();
		
		Set<String> kvs = is.getArgumentSet();
		kvs.add("-c");
		Set<String> flags = new HashSet<String>();
		flags.add("-f");
		
		Arguments args = new Arguments(as, null, is.getArgumentSet());
		is.processArguments(args);
		List<String> files = args.unused();
		process(files, is);
		
	}
}

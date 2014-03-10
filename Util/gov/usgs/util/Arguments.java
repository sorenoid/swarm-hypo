package gov.usgs.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A utility class for more easily dealing with command line arguments.
 * We use following terminology here: argument - part of command line without spaces
 * or dividers, flag - command line option without value, 
 * keyed value - command line option with value.
 *
 * @author Dan Cervelli
 */
public class Arguments
{
	/**
	 * arguments array, from main() 
	 */
	private String[] args;
	/**
	 * parsed args
	 */
	private Set<String> allArgs;
	/**
	 * Map of arguments from args found in kvs constructor argument and it's values
	 */
	private Map<String, String> mappedArgs;
	/**
	 * Set of arguments from args, found in flags constructor argument
	 */
	private Set<String> flaggedArgs;
	/**
	 * Other arguments from args, not included in flaggedArgs or mappedArgs
	 */
	private List<String> unusedArgs;

	/**
	 * Constructor
	 * @param a Arguments array, from main() 
	 * @param flags List of possible flags in the command line
	 * @param kvs List of possible keyed values in the command line
	 */
	public Arguments(String[] a, Set<String> flags, Set<String> kvs)
	{
		args = a;
		if (args == null)
			return;
		mappedArgs = new HashMap<String, String>();
		allArgs = new HashSet<String>();
		flaggedArgs = new HashSet<String>();
		unusedArgs = new ArrayList<String>();
		
		for (int i = 0; i < args.length; i++)
		{
			allArgs.add(args[i]);
			
			if (flags != null && flags.contains(args[i]))
				flaggedArgs.add(args[i]);
			else if (kvs != null && kvs.contains(args[i]))
			{
				String key = args[i];
				String val = "";
				if (args.length > (i + 1))
				{
					val = args[i + 1];
					i++;
				}
				mappedArgs.put(key, val);
			}
			else
				unusedArgs.add(args[i]);
		}
	}
	
	/**
	 * Getter for size
	 * @return size of arguments array
	 */
	public int size()
	{
		if (args == null)
			return 0;
		else
			return args.length;
	}
	
	/**
	 * Getter for value of option named key
	 * @param key keyed value name
	 * @return Value of given option, or null if absent
	 */
	public String get(String key)
	{
		return mappedArgs.get(key);
	}
	
	/**
	 * Getter for value of argument number i
	 * @param i argument number
	 * @return String argument by serial number in the main() command line
	 */
	public String get(int i)
	{
		return args[i];
	}
	
	/** 
	 * See if name is a valid flag
	 * @param arg Flag name 
	 * @return boolean value if this option was parsed as flag and found in the command line
	 */
	public boolean flagged(String arg)
	{
		return flaggedArgs.contains(arg);
	}

	/** 
	 * See if arg is a valid argument
	 * @param arg string argument 
	 * @return boolean value if this argument was found in the command line
	 */
	public boolean contains(String arg)
	{
		return allArgs.contains(arg);
	}
	
	/**
	 * Yield list of unused options
	 * @return List of options that were not parsed as a flag or a keyed value 
	 */
	public List<String> unused()
	{
		return unusedArgs;
	}
	
	/**
	 * Yield last argument
	 * @return last argument
	 */
	public String last()
	{
		return args[args.length - 1];
	}

	/**
	 * Yield last n arguments
	 * @param n arguments count
	 * @return array of n last arguments
	 */
	public String[] lastN(int n)
	{
		String[] result = new String[n];
		for (int i = args.length - n, j = 0; i < args.length; i++, j++)
			result[j] = args[i];
		return result;
	}
	
	/**
	 * Main method
	 * @param args command line arguments
	 */
	public static void main(String[] args)
	{
		System.out.println(System.getProperties());
	}
}

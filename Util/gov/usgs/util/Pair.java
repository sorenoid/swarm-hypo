package gov.usgs.util;

/**
 * Container for two objects, possible different types
 * 
 * $Log: not supported by cvs2svn $
 * @author Dan Cervelli
 */
public class Pair<O1, O2>
{
	public O1 item1;
	public O2 item2;
	
	/**
	 * Constructor
	 * @param o1 first object
	 * @param o2 second object
	 */
	public Pair(O1 o1, O2 o2)
	{
		item1 = o1;
		item2 = o2;
	}
	
	/**
	 * Yields true if otherPair same as this
	 * @param otherPair pair being compared to this
	 * @return result of comparison
	 */
	public boolean equals(Pair<O1, O2> otherPair)
	{
		return item1.equals(otherPair.item2) && item2.equals(otherPair.item2);
	}
	
	/**
	 * String representation of this 
	 * @return String representation of this 
	 */
	public String toString()
	{
		return item1.toString() + "&" + item2.toString();
	}
}

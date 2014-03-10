package gov.usgs.winston;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A class representing one row of the channels table.
 * 
 * $Log: not supported by cvs2svn $
 * Revision 1.4  2006/08/03 19:00:45  cervelli
 * New metadata stuff.
 *
 * Revision 1.3  2006/08/02 23:37:28  cervelli
 * Instrument changes.
 *
 * Revision 1.2  2005/09/02 22:29:17  dcervelli
 * New toString()s and constructor.
 *
 * Revision 1.1  2005/08/26 20:37:08  dcervelli
 * Initial avosouth commit.
 *
 * @author Dan Cervelli
 */
public class Channel
{
	private int sid;
	private Instrument instrument;
	
	private String code;
	private double minTime;
	private double maxTime;
	
	private double linearA;
	private double linearB;
	private String alias;
	private String unit;
	
	private List<String> groups;
	
	private Map<String, String> metadata;
	
	/**
	 * Default constructor
	 */
	public Channel()
	{
		sid = -1;
		code = null;
		instrument = Instrument.NULL;
		minTime = Double.NaN;
		maxTime = Double.NaN;
	}
	
	/**
	 * Constructor from minimal info
	 * @param s sid
	 * @param c code
	 * @param min minTime
	 * @param max maxTime
	 */
	public Channel(int s, String c, double min, double max)
	{
		sid = s;
		code = c;
		minTime = min;
		maxTime = max;
	}
	
	/**
	 * Constructor from a ResultSet
	 * @param rs ResultSet
	 * @throws SQLException
	 */
	public Channel(ResultSet rs) throws SQLException
	{
		sid = rs.getInt("sid");
		code = rs.getString("code");
		minTime = rs.getDouble("st");
		maxTime = rs.getDouble("et");
		instrument = new Instrument(rs);
		linearA = rs.getDouble("linearA");
		if (linearA == 1e300)
			linearA = Double.NaN;
		linearB = rs.getDouble("linearB");
		if (linearB == 1e300)
			linearB = Double.NaN;
		unit = rs.getString("unit");
		if (unit == null)
			unit = "";
		alias = rs.getString("alias");
		if (alias == null)
			alias = "";
	}
	
	/**
	 * Setter for metadata
	 * @param map Mapping of metadata keys to values
	 */
	public void setMetadata(Map<String, String> map)
	{
		metadata = map;
	}
	
	/**
	 * Getter for metadata
	 * @return mapping of metadata keys to values
	 */
	public Map<String, String> getMetadata()
	{
		return metadata;
	}
	
	/**
	 * Add g to list of groups
	 * @param g group to add
	 */
	public void addGroup(String g)
	{
		if (groups == null)
			groups = new ArrayList<String>(2);
		groups.add(g);
	}
	
	/**
	 * Constructor from a String
	 * @param s colon-separated string of values defining a Channel
	 */
	public Channel(String s)
	{
		String[] ss = s.split(":");
		sid = Integer.parseInt(ss[0]);
		code = ss[1];
		minTime = Double.parseDouble(ss[2]);
		maxTime = Double.parseDouble(ss[3]);
		instrument = new Instrument();
		instrument.setLongitude(Double.parseDouble(ss[4]));
		instrument.setLatitude(Double.parseDouble(ss[5]));
		if (ss.length == 12) // metadata present
		{
			if (ss[6].length() >= 1)
				instrument.setTimeZone(ss[6]);
			if (ss[7].length() >= 1)
				alias = ss[7];
			if (ss[8].length() >= 1)
				unit = ss[8];
			linearA = Double.parseDouble(ss[9]);
			linearB = Double.parseDouble(ss[10]);
			if (!ss[11].equals("~"))
			{
				String[] gs = ss[11].split("\\|");
				for (String g : gs)
					addGroup(g);
			}
		}
	}
	
	/**
	 * Getter for sid
	 * @return sid
	 */
	public int getSID()
	{
		return sid;	
	}
	
	/**
	 * Getter for code
	 * @return code
	 */
	public String getCode()
	{
		return code;	
	}
	
	/**
	 * Getter for instrument
	 * @return instrument
	 */
	public Instrument getInstrument()
	{
		return instrument;
	}
	
	/**
	 * Getter for min time
	 * @return min time
	 */
	public double getMinTime()
	{
		return minTime;
	}
	
	/**
	 * Getter for max time
	 * @return max time
	 */
	public double getMaxTime()
	{
		return maxTime;
	}
	
	/**
	 * Getter for code
	 * @return code
	 */
	public String toString()
	{
		return code;	
	}
	
	/**
	 * Getter for groups as a |-separated string
	 * @return groups as a string
	 */
	public String getGroupString()
	{
		if (groups == null)
			return "~";
		
		String gs = "";
		for (int i = 0; i < groups.size() - 1; i++)
		{
			gs += groups.get(i) + "|";
		}
		gs += groups.get(groups.size() - 1);
		return gs;
	}
	
	/**
	 * Getter for List of groups
	 * @return List of groups
	 */
	public List<String> getGroups()
	{
		return groups;
	}
	
	/**
	 * Getter for metadata as a :-separated string
	 * @return metadata as a string
	 */
	public String toMetadataString()
	{
		return String.format("%s:%s:%s:%s:%f:%f:%s:", toPV2String(), instrument.getTimeZone(), alias, unit, linearA, linearB, getGroupString());
	}
	
	/**
	 * Getter for PV2 as a :-separated string
	 * @return PV2 as a string
	 */
	public String toPV2String()
	{
		return String.format("%d:%s:%f:%f:%f:%f", sid, code, minTime, maxTime, instrument.getLongitude(), instrument.getLatitude());
	}
	
		/**
		 * Getter for VDX as a :-separated string
		 * @return VDX as a string
		 */
        public String toVDXString()
        {
        	// this contains the new output for what VDX is expecting
            String stripped = code.replace('$', ' ');
            // return String.format("%s:%f:%f:%s:%s", code, instrument.getLongitude(), instrument.getLatitude(), stripped, stripped);
             return String.format("%d:%s:%s:%f:%f:%f:%s", sid, code, stripped,
                                instrument.getLongitude(), instrument.getLatitude(), instrument.getHeight(), "0");
        }

	/**
	 * Getter for linearA
	 * @return linearA
	 */
	public double getLinearA()
	{
		return linearA;
	}

	/**
	 * Setter for linearA
	 * @param linearA
	 */
	public void setLinearA(double linearA)
	{
		this.linearA = linearA;
	}

	/**
	 * Getter for linearB
	 * @return linearB
	 */
	public double getLinearB()
	{
		return linearB;
	}

	/**
	 * Setter for linearB
	 * @param linearB
	 */
	public void setLinearB(double linearB)
	{
		this.linearB = linearB;
	}

	/**
	 * Getter for alias
	 * @return alias
	 */
	public String getAlias()
	{
		if (alias == null || alias.length() == 0)
			return null;
		
		return alias;
	}

	/**
	 * Setter for alias
	 * @param alias
	 */
	public void setAlias(String alias)
	{
		this.alias = alias;
	}

	/**
	 * Getter for unit
	 * @return unit
	 */
	public String getUnit()
	{
		if (unit == null || unit.length() == 0)
			return null;
		
		return unit;
	}

	/**
	 * Setter for unit
	 * @param unit
	 */
	public void setUnit(String unit)
	{
		this.unit = unit;
	}
}

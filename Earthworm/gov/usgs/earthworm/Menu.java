package gov.usgs.earthworm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A class that holds the information from an Earthworm Wave Server MENU 
 * request.
 *
 * @author Dan Cervelli
 */
public class Menu
{
	private List<MenuItem> items;
	private boolean isScnl;
	private static final String MENU_DELIMITER = "  ";
	
	public Menu(String menu)
	{
		isScnl = false;
		items = new ArrayList<MenuItem>();
		parseMenu(menu);
	}
	
	public boolean isSCNL()
	{
		return isScnl;
	}
	
	public List<MenuItem> getItems()
	{
		return items;
	}
	
	public List<MenuItem> getSortedItems()
	{
		List<MenuItem> list = new ArrayList<MenuItem>(items);
		Collections.sort(list);
		return list;
	}
	
	public int numItems()
	{
		return items.size();
	}
		
	@Deprecated
	public MenuItem getItem(SCN scn)
	{
		for (int i = 0; i < items.size(); i++)
		{
			MenuItem mi = (MenuItem)items.get(i);
			if (mi.isSCN(scn.station, scn.channel, scn.network))
				return mi;	
		}	
		return null;
	}
	
	public boolean channelExists(String channel)
	{
		for (int i = 0; i < items.size(); i++)
		{
			MenuItem mi = (MenuItem)items.get(i);
			if (mi.getSCNSCNL("$").equals(channel))
				return true;	
		}	
		return false;
	}
	
	private void parseMenu(String menu)
	{
		// ignore starting delimiter
		menu = menu.substring(menu.indexOf(MENU_DELIMITER) + 2);

		String[] entries = menu.split(MENU_DELIMITER);
		for (String entry : entries)
			items.add(new MenuItem(entry));

		if (items.get(0).location != null)
			isScnl = true;
	}
	
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		for (MenuItem mi : items)
			sb.append(mi.toFullString() + "\n");
		
		return sb.toString();
	}
}

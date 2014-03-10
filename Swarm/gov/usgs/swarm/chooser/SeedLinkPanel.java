package gov.usgs.swarm.chooser;

import gov.usgs.swarm.Swarm;
import gov.usgs.swarm.data.SeedLinkSource;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * The SeedLink panel is a data source panel for a SeedLink Server.
 * 
 * @author Kevin Frechette (ISTI)
 */
public class SeedLinkPanel extends DataSourcePanel
{
	/** The SeedLink source code. */
	private static final String codeText = ";"
			+ SeedLinkSource.SEEDLINK_SOURCE_CODE + ":";

	/** The default SeedLink host. */
	private static final String defaultHost = "";

	/** The default SeedLink port. */
	private static final String defaultPort = "18000";

	/** The SeedLink host. */
	private JComboBox slsHost;

	/** The SeedLink port. */
	private JTextField slsPort;

	/**
	 * Create the SeedLink panel.
	 */
	public SeedLinkPanel()
	{
		super(SeedLinkSource.SEEDLINK_SOURCE_CODE, "SeedLink Server");
	}

	/**
	 * Determines if the OK should be allowed.
	 * 
	 * @return true if allowed, false otherwise.
	 */
	public boolean allowOK(boolean edit)
	{
		String host = getHost();
		String message = null;

		if (host == null || host.length() == 0 || host.indexOf(';') != -1
				|| host.indexOf(':') != -1)
			message = "There is an error with the " + getName()
					+ " IP address or host name.";
		int ip = -1;
		try
		{
			ip = Integer.parseInt(slsPort.getText());
		}
		catch (Exception e)
		{
		}
		if (ip < 0 || ip > 65535)
			message = "There is an error with the " + getName() + " port.";

		if (message != null)
		{
			JOptionPane.showMessageDialog(Swarm.getApplication(), message,
					"Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		else
			return true;
	}

	/**
	 * Create fields.
	 */
	protected void createFields()
	{
		final String[] hosts =
		{
				"geofon.gfz-potsdam.de", "rtserve.iris.washington.edu"
		};
		slsHost = new JComboBox(hosts);
		slsHost.setEditable(true);
		slsPort = new JTextField();
		String h = defaultHost;
		String p = defaultPort;
		int index;
		if (source != null && (index = source.indexOf(codeText)) != -1)
		{
			String[] ss = source.substring(index + codeText.length())
					.split(":");
			int ssIndex = 0;
			h = ss[ssIndex++];
			p = ss[ssIndex++];
		}
		setHost(h);
		slsPort.setText(p);
	}

	/**
	 * Create panels.
	 */
	protected void createPanel()
	{
		createFields();
		FormLayout layout = new FormLayout(
				"right:max(20dlu;pref), 3dlu, 40dlu, 0dlu, 126dlu", "");

		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();
		builder.append(new JLabel(
				"Use this data source to connect to a SeedLink Server."), 5);
		builder.nextLine();
		builder.appendSeparator();
		builder.append("IP Address or Host Name:");
		builder.append(slsHost, 3);
		builder.nextLine();
		builder.append("Port:");
		builder.append(slsPort);
		builder.append(" SeedLink default: " + defaultPort);
		builder.nextLine();

		panel = builder.getPanel();
	}

	/**
	 * Get the selected host.
	 * 
	 * @return the selected host.
	 */
	protected String getHost()
	{
		// host is always a String
		return (String) slsHost.getSelectedItem();
	}

	/**
	 * Set the selected host.
	 * 
	 * @param h the selected host.
	 */
	protected void setHost(String h)
	{
		slsHost.setSelectedItem(h);
	}

	/**
	 * Process the OK.
	 */
	public String wasOK()
	{
		String result = String.format(getCode() + ":%s:%s", getHost(),
				slsPort.getText());
		return result;
	}
}

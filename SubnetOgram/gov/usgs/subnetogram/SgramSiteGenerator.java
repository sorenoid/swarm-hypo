package gov.usgs.subnetogram;

import gov.usgs.util.ConfigFile;
import gov.usgs.util.Log;
import gov.usgs.util.Util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * 
 * @author Tom Parker
 * 
 */

public class SgramSiteGenerator implements Runnable {

	public static final String DEFAULT_SUBNET_DELAY = "1000";
	public static final String DEFAULT_FILE_EXTENSION = ".html";
	public static final String URL_PATTERN = "(file|http|https)://";

	private static final Logger LOGGER = Log.getLogger("gov.usgs.subnetogram");
	private ConfigFile configFile;
	private boolean top = false;

	/**
	 * Class constructor
	 * 
	 * @param configFile
	 */
	public SgramSiteGenerator(ConfigFile configFile) {

		this.configFile = configFile;
		configFile.put("applicationLaunch", "" + System.currentTimeMillis());
		Logger.getLogger("global").setLevel(Level.OFF);
	}
	
	public void setTop(boolean top) {
		this.top = top;
	}
	
	private void createNetworks() {
		List<String> nets = configFile.getList("network");
		if (nets == null)
			return;

//		configFile.putList("network", nets);

		List<ConfigFile> networkConfigs = new ArrayList<ConfigFile>();
		LinkedList<String> networks = new LinkedList<String>(nets);
		List<String> networkLinks = new ArrayList<String>();

		String firstNetworkName = networks.peekFirst();
		String previousNetworkName = networks.peekLast();
		String nextNetworkName;
		String networkName = networks.peekLast();

		String indexFile = configFile.getString("pathRoot") + "index.html";

		String indexLink = firstNetworkName + "/index.html";
		System.out.println("writing " + indexFile + " -> " + indexLink);

		writeIndex(indexFile, indexLink);

		// TODO: clean this up
		// create configs
		while (networks.size() > 0) {
			previousNetworkName = networkName;
			networkName = networks.poll();
			nextNetworkName = networks.peek();
			if (nextNetworkName == null)
				nextNetworkName = firstNetworkName;

			String networkLink;

			String configDir = configFile.getString("configDir") + networkName
					+ File.separator;
			ConfigFile networkConfig = configFile.clone();
//			networkConfig.remove("network");

			ConfigFile tempConfig = new ConfigFile(configDir + "sgram.config");
			networkConfig.putConfig(tempConfig, false);

//			networkConfig.putList("network", nets);
			networkConfig.put("nextNetworkName", nextNetworkName, false);
			networkConfig
					.put("previousNetworkName", previousNetworkName, false);
			networkConfig.put("configDir", configDir, false);

			networkConfig.put("networkName", networkName);
			String pathRoot = networkConfig.getString("pathRoot");
			networkConfig.put("pathRoot", pathRoot, false);
			networkConfigs.add(networkConfig);

			networkLink = pathRoot + networkName + "/"
					+ networkConfig.getString("subnet")
					+ networkConfig.getString("fileExtension");
			networkLinks.add(networkLink);
		}

		for (ConfigFile config : networkConfigs) {
			config.putList("networkLink", networkLinks);
			
			if (top) {
				SgramSiteGenerator siteGen = new SgramSiteGenerator(config);
				new Thread(siteGen).start();
			}

			try {
				System.out.println("pausing for "
						+ configFile.getString("subnetDelay"));
				Thread.sleep(Long.parseLong(Util.stringToString(
						configFile.getString("subnetDelay"),
						DEFAULT_SUBNET_DELAY)));
			} catch (InterruptedException e) {
				// Restore the interrupted status
				Thread.currentThread().interrupt();
			}
		}
	}

	public void createSubnets() {

		List<String> subs = configFile.getList("subnet");

		if (subs == null)
			return;

		LinkedList<String> subnets = new LinkedList<String>(subs);

		String firstSubnetName = subnets.peekFirst();
		String previousSubnetName = subnets.peekLast();
		String nextSubnetName;
		String subnetName = subnets.peekLast();

		String indexFile = configFile.getString("pathRoot");
		if (configFile.getString("networkName") != null)
			indexFile += configFile.getString("networkName") + "/";
		indexFile += "index.html";

		String indexLink = firstSubnetName + "/" + firstSubnetName
				+ configFile.getString("mosaicSuffix");

		// TODO: why is this messy?
		String fileExtension = configFile.getString("fileExtension");
		if (fileExtension == null)
			fileExtension = DEFAULT_FILE_EXTENSION;

		indexLink += fileExtension;

		writeIndex(indexFile, indexLink);

		while (subnets.size() > 0) {
			previousSubnetName = subnetName;
			subnetName = subnets.poll();
			nextSubnetName = subnets.peek();
			if (nextSubnetName == null)
				nextSubnetName = firstSubnetName;

			ConfigFile subnetConfig = configFile.clone();
			ConfigFile tempConfig = new ConfigFile(
					configFile.getString("configDir") + subnetName + ".config");
			subnetConfig.putConfig(tempConfig, true);

			subnetConfig.put("nextSubnetName", nextSubnetName);
			subnetConfig.put("previousSubnetName", previousSubnetName);
			SgramPageGenerator pageGen = new SgramPageGenerator(subnetConfig);
			new Thread(pageGen).start();

			try {
				Thread.sleep(Long.parseLong(Util.stringToString(
						configFile.getString("subnetDelay"),
						DEFAULT_SUBNET_DELAY)));
			} catch (InterruptedException e) {
				// Restore the interrupted status
				Thread.currentThread().interrupt();
			}
		}
	}

	private void writeIndex(String indexFile, String indexLink) {
		try {
			new File(indexFile).getParentFile().mkdirs();

			BufferedWriter out = new BufferedWriter(new FileWriter(indexFile));
			out.write("<HTML><HEAD><meta http-equiv=\"refresh\" content=\"0;url="
					+ indexLink + "\">");
			out.close();
		} catch (Exception e) {// Catch exception if any
			System.err.println("Error: " + e.getMessage() + " writing "
					+ indexLink + " to " + indexFile);
		}
	}

	@Override
	public void run() {
		createNetworks();
		createSubnets();
	}

	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		// create the package-level logger so inheritance works
		Log.getLogger("gov.usgs.subnetogram");
		LogManager.getLogManager().getLogger("gov.usgs.subnetogram")
				.setLevel(Level.FINEST);
		
		Log.attachFileLogger(LOGGER, "sgramLog", 100000, 10, true);
		LOGGER.setLevel(Level.INFO);
		LOGGER.finest("creating SgramPageGenerator");

		ConfigFile cf = new ConfigFile("config/sgram.config");
		if (!cf.wasSuccessfullyRead())
			throw new RuntimeException("Error reading config file " + cf);

		cf.put("configDir", "config" + File.separator);

		if (cf.getList("debug") != null) {
			for (String name : cf.getList("debug")) {
				Logger l = Log.getLogger(name);
				l.setLevel(Level.ALL);
				LOGGER.fine("debugging " + name);
			}
		}

		SgramSiteGenerator pageGen = new SgramSiteGenerator(cf);
		pageGen.setTop(true);
		new Thread(pageGen).start();
	}
}
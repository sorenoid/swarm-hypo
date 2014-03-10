package gov.usgs.subnetogram.page;

import gov.usgs.subnetogram.SgramSettings;
import gov.usgs.util.ConfigFile;
import gov.usgs.util.Util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.thymeleaf.context.Context;

/**
 * A class to hold settings for a single SubnetOgram page
 * 
 * Adding a setting? Yes, it's a bit tedious. However it's all here and it makes
 * everything else easy.
 * 
 * 1. Set default, as a String, if appropriate - Make it a String
 * 
 * 2. Create public final instance variable
 * 
 * 3. Apply default setting in applyDefaults()
 * 
 * 4. Set value in constructor
 * 
 * 5. Add variable to toString()
 * 
 * 6. Add variable to applySettings()
 * 
 * 7. Add variable to getConfigFile(), if constructor doesn't generate it
 * 
 * 
 * @author Tom Parker
 * 
 */
public class SgramSinglePageSettings extends SgramSettings {

	public static final String DEFAULT_TEMPLATE = "duff";
	public static final String DEFAULT_FILE_EXTENSION = ".html";
	public static final String DEFAULT_MOSAIC_SUFFIX = "_mosaic";
	public static final String DEFAULT_TEXT_TIME_FORMAT = "HH:mm";
	public static final String DEFAULT_TEXT_DATE_FORMAT = "MMM dd";

	/**
	 * The template to apple
	 */
	public final String template;

	/*
	 * Next subnet
	 */
	public final String nextSubnetName;

	/*
	 * Previous subnet
	 */
	public final String previousSubnetName;

	/**
	 * filename extension of this file
	 */
	public final String fileExtension;

	/**
	 * filesystem path to the root of the output
	 */
//	public final String pathToRoot;

	/**
	 * String to use to format the path to the next file
	 */
	public final String nextFilePathDateFormat;

	/**
	 * string to use to format the name of the next file
	 */
	public final String nextFileNameDateFormat;

	/**
	 * String to use to format the path to the previous file
	 */
	public final String previousFilePathDateFormat;

	/**
	 * String to use to format the name of the previous file
	 */
	public final String previousFileNameDateFormat;

	/**
	 * suffix appended to mosaic pages
	 */
	public final String mosaicSuffix;

//	/**
//	 * 
//	 */
//	public final Map<String, String> networkLinks;

	/**
	 * 
	 */
	public final List<String> networks;

	/**
	 * My logger
	 */
	// private static final Logger logger =
	// Logger.getLogger("gov.usgs.subnetogram");

	/**
	 * The list of subnets in this network
	 */
	public List<String> subnets;

	/**
	 * Format used to display times. Done here since the ThymeLeaf's
	 * #dates.format does not seem to be time zone aware
	 */
	public final SimpleDateFormat textTimeFormat;
	public final SimpleDateFormat textDateFormat;

	/**
	 * Holds the settings required to create a subnetOgramImage
	 * 
	 * @param cf
	 *            The fully parsed config file
	 */
	public SgramSinglePageSettings(ConfigFile cf) {
		super(cf);

		// settings to use and maintain as is
		fileExtension = cf.getString("fileExtension");
		template = cf.getString("template");

		nextFilePathDateFormat = cf.getString("nextFilePathDateFormat");
		previousFilePathDateFormat = cf.getString("previousFilePathDateFormat");

		nextFileNameDateFormat = cf.getString("nextFileNameDateFormat");
		previousFileNameDateFormat = cf.getString("previousFileNameDateFormat");

		nextSubnetName = cf.getString("nextSubnetName");
		previousSubnetName = cf.getString("previousSubnetName");

		mosaicSuffix = cf.getString("mosaicSuffix");

		subnets = cf.getList("subnet");
		networks = cf.getList("network");

//		pathToRoot = getRelativePath(getTimeStampFilePath(), pathRoot);
		textTimeFormat = new SimpleDateFormat(Util.stringToString(
				cf.getString("textTimeFormat"), DEFAULT_TEXT_TIME_FORMAT));
		textTimeFormat.setTimeZone(timeZone);

		textDateFormat = new SimpleDateFormat(Util.stringToString(
				cf.getString("textDateFormat"), DEFAULT_TEXT_DATE_FORMAT));
		textDateFormat.setTimeZone(timeZone);
	}

	/**
	 * 
	 */
	public String getBareFilePath() {
		return generateFilePath(endTime, null, subnetName);
	}

	/**
	 * 
	 */
	public String getTimeStampFilePath() {
		return generateFilePath(endTime, filePathDateFormat, subnetName);
	}

	/**
	 * 
	 */
	public String getBareFileName() {
		return generateFileName(endTime, null, subnetName);
	}

	/**
	 * 
	 */
	public String getTimeStampFileName() {
		return generateFileName(endTime, fileNameDateFormat, subnetName);
	}


	/**
	 * {@inheritDoc}
	 */
	protected void setDefaults(ConfigFile cf) {
		super.setDefaults(cf);

		cf.put("fileExtension", Util.stringToString(
				cf.getString("fileExtension"), DEFAULT_FILE_EXTENSION), false);
		cf.put("template",
				Util.stringToString(cf.getString("template"), DEFAULT_TEMPLATE),
				false);
		cf.put("nextFilePathDateFormat",
				Util.stringToString(cf.getString("nextFilePathDateFormat"),
						cf.getString("filePathDateFormat")), false);
		cf.put("previousFilePathDateFormat", Util.stringToString(
				cf.getString("previousFilePathDateFormat"),
				cf.getString("filePathDateFormat")), false);
		cf.put("nextFileNameDateFormat",
				Util.stringToString(cf.getString("nextFileNameDateFormat"),
						cf.getString("fileNameDateFormat")), false);
		cf.put("previousFileNameDateFormat", Util.stringToString(
				cf.getString("previousFileNameDateFormat"),
				cf.getString("fileNameDateFormat")), false);
		cf.put("mosaicSuffix", Util.stringToString(
				cf.getString("mosaicSuffix"), DEFAULT_MOSAIC_SUFFIX));
	}

	/**
	 * return a relative path from one directory to another directory
	 * 
	 * @param src
	 *            source directory
	 * @param dst
	 *            destination directory
	 * @return relative path from s1
	 */
	public String getRelativePath(String src, String dst) {
		StringBuilder relPath = new StringBuilder();

		// I only deal with paths and paths always end with a seperator. The
		// seperator is always a /

		if (!src.endsWith("/"))
			src = src.substring(0, src.lastIndexOf('/') + 1);

		if (!dst.endsWith("/"))
			dst = dst.substring(0, dst.lastIndexOf('/') + 1);

		LinkedList<String> srcComp = new LinkedList<String>();
		LinkedList<String> dstComp = new LinkedList<String>();
		// populate lists
		for (String s : src.split("/"))
			srcComp.offerLast(s);

		for (String s : dst.split("/"))
			dstComp.offerLast(s);

		// drop common components
		while (!(srcComp.isEmpty() || dstComp.isEmpty())
				&& srcComp.peekFirst().equals(dstComp.peekFirst())) {
			srcComp.pollFirst();
			dstComp.pollFirst();
		}

		// back up
		while (srcComp.pollFirst() != null)
			relPath.append("../");

		// decend
		while (dstComp.peekFirst() != null)
			relPath.append(dstComp.pollFirst() + '/');

		return relPath.toString();
	}

	/**
	 * return settings with all fields decrimented by duration
	 */
	protected ConfigFile getConfigFileOffset(int offset) {
		ConfigFile cf = super.getConfigFileOffset(offset);

		cf.put("fileExtension", fileExtension);
		cf.put("template", template);
		cf.put("nextFilePathDateFormat", nextFilePathDateFormat);
		cf.put("previousFilePathDateFormat", previousFilePathDateFormat);
		cf.put("nextSubnetName", nextSubnetName);
		cf.put("previousSubnetName", previousSubnetName);
		cf.put("mosaicSuffix", mosaicSuffix);
		cf.put("nextFileNameDateFormat", nextFileNameDateFormat);
		cf.put("previousFileNameDateFormat", previousFileNameDateFormat);
		cf.putList("subnet", subnets);
		cf.putList("networks", networks);
		return cf;
	}

	/**
	 * 
	 */
	public ConfigFile getConfigFile() {
		return getConfigFileOffset(0);
	}

	/**
	 * Generate the settings for the next image
	 * 
	 * @return
	 */
	public SgramSinglePageSettings getNext() {
		return new SgramSinglePageSettings(getConfigFileOffset(duration));
	}

	/**
	 * Generate the settings for the previous image
	 * 
	 * @return
	 */
	public SgramSinglePageSettings getPrevious() {
		return new SgramSinglePageSettings(getConfigFileOffset(-duration));
	}

	/**
	 * {@inheritDoc}
	 */
	public String toString() {
		StringBuilder settings = new StringBuilder();

		settings.append(super.toString());

		settings.append("fileExtension = " + fileExtension + "\n");
		settings.append("template = " + template + "\n");
		settings.append("nextFilePathDateFormat = " + nextFilePathDateFormat
				+ "\n");
		settings.append("previousFilePathDateFormat = "
				+ previousFilePathDateFormat + "\n");
		settings.append("nextFileNameDateFormat = " + nextFileNameDateFormat
				+ "\n");
		settings.append("previousFileNameDateFormat = "
				+ previousFileNameDateFormat + "\n");
		settings.append("nextSubnetName = " + nextSubnetName + "\n");
		settings.append("previousSubnetName = " + previousSubnetName + "\n");
		settings.append("mosaicSuffix = " + mosaicSuffix + "\n");
//		settings.append("pathToRoot = " + pathToRoot + "\n");
		settings.append("textTimeFormat = " + textTimeFormat + "\n");
		settings.append("textDateFormat = " + textDateFormat + "\n");
		return settings.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	public void applySettings(Context ctx) {
		super.applySettings(ctx);

		ctx.setVariable("fileExtension", fileExtension);
		ctx.setVariable("template", template);
		ctx.setVariable("nextFilePathDateFormat", nextFilePathDateFormat);
		ctx.setVariable("previousFilePathDateFormat",
				previousFilePathDateFormat);
		ctx.setVariable("nextFileNameDateFormat", nextFileNameDateFormat);
		ctx.setVariable("previousFileNameDateFormat",
				previousFileNameDateFormat);
		ctx.setVariable("nextSubnetName", nextSubnetName);
		ctx.setVariable("previousSubnetName", previousSubnetName);
		ctx.setVariable("mosaicSuffix", mosaicSuffix);
//		ctx.setVariable("pathToRoot", pathToRoot);
		ctx.setVariable("textTimeFormat", textTimeFormat);
		ctx.setVariable("textDateFormat", textDateFormat);
		ctx.setVariable("textStartTime", textTimeFormat.format(startTime));
		ctx.setVariable("textEndTime", textTimeFormat.format(endTime));
		ctx.setVariable("textStartDate", textDateFormat.format(startTime));
	}
}
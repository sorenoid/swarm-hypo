package gov.usgs.subnetogram.page.mosaic;

import gov.usgs.subnetogram.page.SgramSinglePageSettings;
import gov.usgs.util.ConfigFile;
import gov.usgs.util.Util;

import org.thymeleaf.context.Context;

/**
 * A class to hold settings for a single SubnetOgram mosaic page Is this a good
 * idea? Are mosaics really different than other pages?
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
public class SgramMosaicSettings extends SgramSinglePageSettings {
	public static final String DEFAULT_COLS = "1";
	public static final String DEFAULT_DAILY_FILE_NAME_FORAMT = "_yyyyMMdd";
	public static final String DEFAULT_CREATE_DAILY = "true";
	public static final String DEFAULT_DAILY_FILE_SUFFIX = "_daily";

	/**
	 * number of columns on mosaic
	 */
	public final int cols;
	public final String dailyFileNameFormat;
	public final boolean createDaily;
	public final String dailyFileSuffix;

	public SgramMosaicSettings(ConfigFile cf) {
		super(cf);

		cols = Integer.parseInt(cf.getString("cols"));
		createDaily = Util.stringToBoolean(cf.getString("createDaily"));
		dailyFileNameFormat = cf.getString("dailyFileNameFormat");
		dailyFileSuffix = cf.getString("dailyFileSuffix");
	}

	/**
	 * {@inheritDoc}
	 */
	protected void setDefaults(ConfigFile cf) {
		super.setDefaults(cf);

		cf.put("cols", Util.stringToString(cf.getString("cols"), DEFAULT_COLS),
				false);
		cf.put("createDaily", Util.stringToString(cf.getString("createDaily"),
				DEFAULT_CREATE_DAILY));
		cf.put("dailyFileNameFormat", Util.stringToString(
				cf.getString("dailyFileNameFormat"),
				DEFAULT_DAILY_FILE_NAME_FORAMT));
		cf.put("dailyFileSuffix", Util.stringToString(
				cf.getString("dailyFileSuffix"), DEFAULT_DAILY_FILE_SUFFIX));
	}

	/**
	 * return settings with all fields decrimented by duration
	 */
	protected ConfigFile getConfigFileOffset(int offset) {
		ConfigFile cf = super.getConfigFileOffset(offset);

		cf.put("cols", "" + cols);
		cf.put("createDaily", "" + createDaily);
		cf.put("dailyFileNameFormat", dailyFileNameFormat);
		cf.put("dailyFileSuffix", dailyFileSuffix);
		return cf;
	}

	/**
	 * 
	 */
	public ConfigFile getConfigFile() {
		return getConfigFileOffset(0);
	}

	/**
	 * {@inheritDoc}
	 */
	public String toString() {
		StringBuilder settings = new StringBuilder();

		settings.append(super.toString());

		settings.append("cols = " + cols + "\n");
		settings.append("createDaily = " + createDaily + "\n");
		settings.append("dailyFileNameFormat = " + dailyFileNameFormat + "\n");
		settings.append("dailyFileSuffix = " + dailyFileSuffix + "\n");

		return settings.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	public void applySettings(Context ctx) {
		super.applySettings(ctx);

		ctx.setVariable("cols", cols);
		ctx.setVariable("createDaily", createDaily);
		ctx.setVariable("dailyFileNameFormat", dailyFileNameFormat);
		ctx.setVariable("dailyFileSuffix", dailyFileSuffix);
		ctx.setVariable("dailyMosaic", getBareDailyFileName() + fileExtension);
		ctx.setVariable("currentFile", getCurrentDailyMosaic());
		ctx.setVariable("nextMosaicTitle", getNextMosaicTitle());
	}

	public String getTimeStampFileName() {
		return generateFileName(endTime, fileNameDateFormat, subnetName);

	}

	public String getBareFileName() {
		return generateFileName(endTime, null, subnetName);
	}

	public String getTimeStampDailyFileName() {
		return generateFileName(endTime, dailyFileNameFormat, subnetName
				) + dailyFileSuffix;
	}

	public String getBareDailyFileName() {
		return generateFileName(endTime, null, subnetName) + dailyFileSuffix;
	}

	public String getNextMosaicTitle() {
		return "Daily Mosaic";
	}

	public String getNextMosaicPage() {
		return getBareDailyFileName();
	}

	public String getCurrentDailyMosaic() {
		String s = getRelativePath(getTimeStampFilePath(), pathRoot);
		if (networkName != null)
			s += networkName + '/';
		return s + subnetName + '/' + getBareDailyFileName();

	}
}

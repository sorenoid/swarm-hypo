package gov.usgs.subnetogram.image;

import gov.usgs.subnetogram.SgramSettings;
import gov.usgs.swarm.data.SeismicDataSource;
import gov.usgs.util.ConfigFile;
import gov.usgs.util.Util;

import java.util.List;

import org.thymeleaf.context.Context;

/**
 * A class to hold setting specific to a given SubnetOgram image
 * 
 * Adding a setting? Yes, it's a bit tedious. However it's all here and it makes
 * everything else easy.
 * 
 * 1. Set default if appropriate
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
 * 7. Add variable to getConfigFile()
 * 
 * @author Tom Parker
 * 
 */
public class SgramImageSettings extends SgramSettings {

	public static final String DEFAULT_WAVE_RATIO = "30";
	public static final String DEFAULT_OVERLAP = "0.859375";
	public static final String DEFAULT_LOG_POWER = "true";
	public static final String DEFAULT_MIN_FREQ = "0";
	public static final String DEFAULT_MAX_FREQ = "10";
	public static final String DEFAULT_NFFT = "0";
	public static final String DEFAULT_BIN_SIZE = "256";
	public static final String DEFAULT_MAX_POWER = "120";
	public static final String DEFAULT_MIN_POWER = "20";
	public static final String DEFAULT_HEIGHT = "756";
	public static final String DEFAULT_WIDTH = "576";
	public static final String DEFAULT_DECORATE = "true";
	public static final String DEFAULT_DETREND = "true";
	
	/**
	 * The source of the seismic data
	 */
	public final SeismicDataSource dataSource;

	/**
	 * The ratio of the waveform plot to the total wave+spectrogram plot
	 */
	public final double waveRatio;

	/**
	 * The overlap
	 */
	public final double overlap;

	/**
	 * If true use a log scale for power
	 */
	public final boolean logPower;

	/**
	 * Minimum frequency to plot
	 */
	public final double minFreq;

	/**
	 * Maximum frequency to plot
	 */
	public final double maxFreq;

	/**
	 * The nfft
	 */
	public final int nfft;

	/**
	 * The bin size
	 */
	public final int binSize;

	/**
	 * Max power to plot
	 */
	public final double maxPower;

	/**
	 * Min power to plot
	 */
	public final double minPower;

	/**
	 * The list of channels to place of the plot TODO: Don't expose this as a
	 * mutable object
	 */
	public final List<String> channels;

	/**
	 * Total image size, including any decoration
	 */
	public final int height;

	/**
	 * Total image width, including any decoration
	 */
	public final int width;

	/**
	 * Filesystem path to write file to
	 */
//	public final String filePath;

	/**
	 * Filesystem name of plot
	 */
//	public final String fileName;

	/**
	 * add labels?
	 */
	public final boolean decorate;
	
	/**
	 * detrand wave data?
	 */
	public final boolean detrend;
	

	/**
	 * Holds the settings required to create a subnetOgramImage
	 * 
	 * @param cf
	 *            The fully parsed config file
	 */
	public SgramImageSettings(ConfigFile cf) {
		super(cf);

		// data source is required
		SeismicDataSource sds = SeismicDataSource.getDataSource(cf
				.getString("dataSource"));
		sds.setUseCache(false);
		dataSource = sds;
		if (dataSource == null)
			fatalError("required config directive dataSource missing.");

		// channels required
		channels = cf.getList("channel");
		if (channels == null)
			fatalError("No channels to plot.");

		waveRatio = Double.parseDouble(cf.getString("waveRatio"));
		overlap = Double.parseDouble(cf.getString("overlap"));
		logPower = Util.stringToBoolean(cf.getString("logPower"));
		minFreq = Double.parseDouble(cf.getString("minFreq"));
		maxFreq = Double.parseDouble(cf.getString("maxFreq"));
		nfft = Integer.parseInt(cf.getString("nfft"));
		binSize = Integer.parseInt(cf.getString("binSize"));
		maxPower = Double.parseDouble(cf.getString("maxPower"));
		minPower = Double.parseDouble(cf.getString("minPower"));
		height = Integer.parseInt(cf.getString("height"));
		width = Integer.parseInt(cf.getString("width"));
		decorate = Util.stringToBoolean(cf.getString("decorate"));
		detrend = Util.stringToBoolean(cf.getString("detrend"));
	}

	public String getTimeStampFilePath() {
			return generateFilePath(endTime, filePathDateFormat, subnetName);
	}
	
	public String getTimeStampFileName() {
			return generateFileName(endTime, fileNameDateFormat, subnetName);
	}
	
	public String getBareFilePath() {
			return generateFilePath(endTime, null, subnetName);
	}
	
	public String getBareFileName() {
			return generateFileName(endTime, null, subnetName);
	}

	/**
	 * {@inheritDoc}
	 */
	protected void setDefaults(ConfigFile cf) {
		super.setDefaults(cf);

		cf.put("waveRatio", Util.stringToString(cf.getString("waveRatio"),
				DEFAULT_WAVE_RATIO), false);
		cf.put("overlap",
				Util.stringToString(cf.getString("overlap"), DEFAULT_OVERLAP),
				false);
		cf.put("logPower", Util.stringToString(cf.getString("logPower"),
				DEFAULT_LOG_POWER), false);
		cf.put("minFreq",
				Util.stringToString(cf.getString("minFreq"), DEFAULT_MIN_FREQ),
				false);
		cf.put("maxFreq",
				Util.stringToString(cf.getString("maxFreq"), DEFAULT_MAX_FREQ),
				false);
		cf.put("nfft", Util.stringToString(cf.getString("nfft"), DEFAULT_NFFT),
				false);
		cf.put("binSize",
				Util.stringToString(cf.getString("binSize"), DEFAULT_BIN_SIZE),
				false);
		cf.put("maxPower", Util.stringToString(cf.getString("maxPower"),
				DEFAULT_MAX_POWER), false);
		cf.put("minPower", Util.stringToString(cf.getString("minPower"),
				DEFAULT_MIN_POWER), false);
		cf.put("height",
				Util.stringToString(cf.getString("height"), DEFAULT_HEIGHT),
				false);
		cf.put("width",
				Util.stringToString(cf.getString("width"), DEFAULT_WIDTH),
				false);
		cf.put("decorate",
				Util.stringToString(cf.getString("decorate"), DEFAULT_DECORATE));
		cf.put("detrend",
				Util.stringToString(cf.getString("detrend"), DEFAULT_DETREND));
	}

	/**
	 * return settings with all fields decrimented by duration
	 */
	protected ConfigFile getConfigFileOffset(int offset) {
		ConfigFile cf = super.getConfigFileOffset(offset);

		cf.put("dataSource", dataSource.toConfigString());
		cf.put("waveRatio", "" + waveRatio);
		cf.put("overlap", "" + overlap);
		cf.put("logPower", "" + logPower);
		cf.put("minFreq", "" + minFreq);
		cf.put("maxFreq", "" + maxFreq);
		cf.put("nfft", "" + nfft);
		cf.put("binSize", "" + binSize);
		cf.put("maxPower", "" + maxPower);
		cf.put("minPower", "" + minPower);
		cf.put("height", "" + height);
		cf.put("width", "" + width);
		cf.putList("channel", channels);
		cf.put("decorate", "" + decorate);
		cf.put("detrend", "" + detrend);

		return cf;
	}

	public ConfigFile getConfigFile() {
		return getConfigFileOffset(0);
	}

	/**
	 * Generate the settings for the next image
	 * 
	 * @return
	 */
	public SgramImageSettings getNext() {
		return new SgramImageSettings(getConfigFileOffset(duration));
	}

	/**
	 * Generate the settings for the previous image
	 * 
	 * @return
	 */
	public SgramImageSettings getPrevious() {
		return new SgramImageSettings(getConfigFileOffset(-duration));
	}

	/**
	 * {@inheritDoc}
	 */
	public String toString() {
		StringBuilder settings = new StringBuilder();
		settings.append(super.toString());

		settings.append("dataSource = " + dataSource + "\n");
		settings.append("waveRatio = " + waveRatio + "\n");
		settings.append("overlap = " + overlap + "\n");
		settings.append("logPower = " + logPower + "\n");
		settings.append("minFreq = " + minFreq + "\n");
		settings.append("maxFreq = " + maxFreq + "\n");
		settings.append("nfft = " + nfft + "\n");
		settings.append("binSize = " + binSize + "\n");
		settings.append("maxPower = " + maxPower + "\n");
		settings.append("minPower = " + minPower + "\n");
		settings.append("height = " + height + "\n");
		settings.append("width = " + width + "\n");
		settings.append("channels = " + channels + "\n");
		settings.append("decorate = " + decorate + "\n");
		settings.append("detrend = " + detrend + "\n");

		return settings.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	public void applySettings(Context ctx) {
		super.applySettings(ctx);

		ctx.setVariable("dataSource", dataSource);
		ctx.setVariable("waveRatio", waveRatio);
		ctx.setVariable("overlap", overlap);
		ctx.setVariable("logPower", logPower);
		ctx.setVariable("minFreq", minFreq);
		ctx.setVariable("maxFreq", maxFreq);
		ctx.setVariable("nfft", nfft);
		ctx.setVariable("binSize", binSize);
		ctx.setVariable("maxPower", maxPower);
		ctx.setVariable("minPower", minPower);
		ctx.setVariable("height", height);
		ctx.setVariable("width", width);
		ctx.setVariable("channels", channels);
		ctx.setVariable("decorate", decorate);
		ctx.setVariable("detrend", detrend);
	}
}
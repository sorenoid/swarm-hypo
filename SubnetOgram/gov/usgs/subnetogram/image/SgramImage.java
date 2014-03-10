package gov.usgs.subnetogram.image;

import gov.usgs.plot.BasicFrameRenderer;
import gov.usgs.plot.Plot;
import gov.usgs.plot.PlotException;
import gov.usgs.plot.TextRenderer;
import gov.usgs.util.Util;
import gov.usgs.vdx.data.wave.SliceWave;
import gov.usgs.vdx.data.wave.Wave;
import gov.usgs.vdx.data.wave.plot.MinuteMarkingWaveRenderer;
import gov.usgs.vdx.data.wave.plot.SliceWaveRenderer;
import gov.usgs.vdx.data.wave.plot.SpectrogramRenderer;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Creates a stacked imaged with full decoration.
 * 
 * @author Tom Parker
 * 
 */
public class SgramImage {

	public static final int LABEL_HEIGHT = 35;
	public static final int LABEL_WIDTH = 30;
	public static final Color NO_DATA_COLOR = new Color(160, 41, 41);
	public static final Font NO_DATA_FONT = Font.decode("dialog-PLAIN-36");
	public static final Font NO_DATA_THUMBNAIL_FONT = Font
			.decode("dialog-PLAIN-12");
	public static final int IMAGE_HEIGHT_FONT_THREASHOLD = 200;

	private static final Logger logger = Logger
			.getLogger("gov.usgs.subnetogram");
	protected SgramImageSettings settings;
	protected Map<String, SliceWave> waves;

	/**
	 * Class constructor
	 * 
	 * @param settings
	 *            The settings used to create plot.
	 */
	public SgramImage(SgramImageSettings settings) {
		this.settings = settings;
	}

	/**
	 * 
	 */
	protected SliceWaveRenderer getSliceWaveRenderer(SliceWave sliceWave) {

		SliceWaveRenderer wr = new MinuteMarkingWaveRenderer();

		wr.xTickMarks = true;
		wr.xTickValues = false;
		wr.xUnits = false;
		wr.xLabel = false;
		wr.yTickMarks = false;
		wr.yTickValues = false;
		wr.setMinY(sliceWave.min());
		wr.setMaxY(sliceWave.max());
		wr.setColor(Color.BLACK);
		wr.setWave(sliceWave);
		double t1 = Util.dateToJ2K(settings.startTime);
		double t2 = t1+(settings.duration * 60);
		wr.setViewTimes(t1, t2, "UTC");

		return wr;
	}

	/**
     * 
     */
	protected SpectrogramRenderer getSpectrogramRenderer(int index,
			String channel, SliceWave sliceWave) {

		SpectrogramRenderer sr = new SpectrogramRenderer();
		boolean decorateX = false;
		if (index == settings.channels.size() - 1)
			decorateX = settings.decorate;

		// y-axis labels will sometimes not be displayed if x-axis tick marks
		// are not displayed. Note sure why.
		sr.xTickMarks = true;
		sr.xTickValues = decorateX;
		sr.xUnits = decorateX;
		sr.xLabel = decorateX;
		
		sr.yTickMarks = true;
		sr.yTickValues = settings.decorate;
		sr.setOverlap(settings.overlap);
		sr.setLogPower(settings.logPower);
		sr.setWave(sliceWave);
		double t1 = Util.dateToJ2K(settings.startTime);
		sr.setViewStartTime(t1);
		sr.setViewEndTime(t1+(settings.duration * 60));
		sr.setTimeZone("UTC");
		sr.setMinFreq(settings.minFreq);
		sr.setMaxFreq(settings.maxFreq);
		sr.setNfft(settings.nfft);
		sr.setBinSize(settings.binSize);
		sr.setMaxPower(settings.maxPower);
		sr.setMinPower(settings.minPower);
		if (settings.decorate)
			sr.setYLabelText(channel);

		return sr;
	}

	/**
	 * waves accessor
	 * 
	 * @return This objects waves
	 */
	public Map<String, SliceWave> getWaves() {
		return waves;
	}

	/**
	 * waves mutator
	 * 
	 * @param waves
	 *            SliceWaves to plot
	 */
	public void setWaves(Map<String, SliceWave> waves) {
		this.waves = waves;
	}

	/**
	 * Populate waves with SliceWaves from dataSource
	 */
	public void getData() {

		logger.finest("getting data...");
		waves = new HashMap<String, SliceWave>();

		double t1 = Util.dateToJ2K(settings.startTime);
		double t2 = t1 + (settings.duration * 60);

		for (String channel : settings.channels) {
			Wave wave = settings.dataSource.getWave(channel, t1, t2);

			if (wave == null)
				waves.put(channel, null);
			else {
				if (settings.detrend)
					wave.detrend();

				SliceWave sWave = new SliceWave(wave);
				sWave.setSlice(t1, t2);
				waves.put(channel, sWave);
			}
		}
	}

	/**
	 * Create the plot object
	 * 
	 * @return generated Plot
	 */
	private Plot generatePlot() {

		int labelWidth = settings.decorate ? LABEL_WIDTH : 0;
		int labelHeight = settings.decorate ? LABEL_HEIGHT : 0;

		if (waves == null)
			getData();

		Plot plot = new Plot(settings.width, settings.height);

		int channelHeight = (settings.height - (labelHeight * 2))
				/ settings.channels.size();
		int waveHeight = (int) (channelHeight * (settings.waveRatio * .01));
		int sgramHeight = channelHeight - waveHeight;

		int i = 0;
		for (String channel : settings.channels) {
			int thisChannelHeight = channelHeight;

			// If padding is required due to rounding errors, put it in the
			// first plot
			if (i == 0)
				thisChannelHeight += settings.height
						- (channelHeight * settings.channels.size());

			// renderer adds a 1 pixel border, making images larger than
			// requested
			int top = (i * thisChannelHeight) + labelHeight - 1 + i;

			if (waves.get(channel) == null) {
				BasicFrameRenderer fr = new BasicFrameRenderer();
				fr.addRenderer(new TextRenderer(0, 0, ""));
				fr.setLocation(labelWidth, top, settings.width
						- (labelWidth + 2), waveHeight);
				plot.addRenderer(fr);

				fr = new BasicFrameRenderer();
				TextRenderer tr = new TextRenderer(settings.width / 2, top
						+ thisChannelHeight / 2, channel + " - no data");
				tr.horizJustification = TextRenderer.CENTER;
				tr.vertJustification = TextRenderer.CENTER;
				tr.color = NO_DATA_COLOR;
				if (settings.height < IMAGE_HEIGHT_FONT_THREASHOLD)
					tr.font = NO_DATA_THUMBNAIL_FONT;
				else
					tr.font = NO_DATA_FONT;
				fr.addRenderer(tr);
				plot.addRenderer(fr);
			} else {
				SliceWaveRenderer swr = getSliceWaveRenderer(waves.get(channel));

				// skip 1 pixel border
				swr.setLocation(labelWidth - 1, top, settings.width
						- (labelWidth * 2) + 1, waveHeight);
				swr.update();
				
				plot.addRenderer(swr);

				SpectrogramRenderer sr = getSpectrogramRenderer(i, channel,
						waves.get(channel));

				// skip 1 pixel border
				sr.setLocation(labelWidth - 1, top + waveHeight, settings.width
						- (labelWidth * 2) + 1, sgramHeight);
				sr.update();
				plot.addRenderer(sr);
			}

			i++;
		}

		return plot;
	}

	/**
	 * Create plot and write PNG to disk, generating filename and path along the
	 * way.
	 */
	public void generateTimeStampPNG() {
		generatePNG(settings.pathToPath(settings.getTimeStampFilePath() + settings.getTimeStampFileName()
				+ ".png"));
	}

	/**
	 * Create plot and write PNG to disk, generating filename and path along the
	 * way.
	 */
	public void generateCurrentPNG() {
		generatePNG(settings.pathToPath(settings.getBareFilePath() + settings.getBareFileName()
				+ ".png"));
	}

	/**
	 * Create plot and write PNG to a file
	 * 
	 * @param fileName
	 *            name of file, including path
	 */
	public void generatePNG(String fileName) {
		Plot plot = generatePlot();
		logger.fine("generating " + fileName);
		new File(fileName).getParentFile().mkdirs();
		try {
			plot.writePNG(fileName);
		} catch (PlotException e) {
			fatalError(e.getLocalizedMessage());
		}
	}

	/**
	 * Create plot and write JPEG to disk, generating filename and path along
	 * the way.
	 */
	public void generateTimeStampJPEG() {
		generateJPEG(settings.pathToPath(settings.getTimeStampFilePath() + settings.getTimeStampFileName()
				+ ".jpg"));
	}

	/**
	 * Create plot and write JPEG to disk, generating filename and path along
	 * the way.
	 */
	public void generateCurrentJPEG() {
		generateJPEG(settings.pathToPath(settings.getBareFilePath() + settings.getBareFileName()
				+ ".jpg"));
	}

	/**
	 * Create plot and write JPEG to a file
	 * 
	 * @param fileName
	 *            name of file, including path
	 */
	public void generateJPEG(String fileName) {
		Plot plot = generatePlot();
		new File(fileName).getParentFile().mkdirs();
		try {
			plot.writeJPEG(fileName);
		} catch (PlotException e) {
			fatalError(e.getLocalizedMessage());
		}
	}

	/**
	 * Create plot and write PS to disk, generating filename and path along the
	 * way.
	 */
	public void generateTimeStampPS() {
		generatePS(settings.pathToPath(settings.getTimeStampFilePath() + settings.getBareFileName()
				+ ".ps"));
	}
	
	/**
	 * Create plot and write PS to disk, generating filename and path along the
	 * way.
	 */
	public void generateCurrentPS() {
		generatePS(settings.pathToPath(settings.getBareFilePath() + settings.getBareFileName()
				+ ".ps"));
	}

	/**
	 * Create plot and write PostScript to a file
	 * 
	 * @param fileName
	 *            name of file, including path
	 */
	public void generatePS(String fileName) {
		Plot plot = generatePlot();
		new File(fileName).getParentFile().mkdirs();
		plot.writePS(fileName);
	}

	/**
	 * Respond to errors. Mostly an attempt to keep a scheduledTask running.
	 * 
	 * @param msg
	 *            The error message.
	 */
	public void fatalError(String msg) {
		logger.severe(msg);
		if (settings.onError.equals("exit"))
			System.exit(1);
	}
}
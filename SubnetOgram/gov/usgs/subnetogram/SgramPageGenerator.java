/**
 * 
 */
package gov.usgs.subnetogram;

import gov.usgs.subnetogram.image.SgramImage;
import gov.usgs.subnetogram.image.SgramImageSettings;
import gov.usgs.subnetogram.image.SgramThumbnailSettings;
import gov.usgs.subnetogram.page.SgramPage;
import gov.usgs.subnetogram.page.SgramSinglePage;
import gov.usgs.subnetogram.page.SgramSinglePageSettings;
import gov.usgs.subnetogram.page.SgramSinglePageTimeStamp;
import gov.usgs.subnetogram.page.mosaic.SgramMosaic;
import gov.usgs.subnetogram.page.mosaic.SgramMosaicSettings;
import gov.usgs.subnetogram.page.mosaic.SgramMosaicTimeStamp;
import gov.usgs.subnetogram.page.mosaic.daily.SgramDailyMosaic;
import gov.usgs.subnetogram.page.mosaic.daily.SgramDailyMosaicSettings;
import gov.usgs.subnetogram.page.mosaic.daily.SgramDailyMosaicTimeStamp;
import gov.usgs.util.ConfigFile;
import gov.usgs.util.Util;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Generates a series of SubnetOgram pages at a set interval
 * 
 * TODO: Note images that contain traces with no data in file to be filled later
 * 
 * @author Tom Parker
 * 
 */
public class SgramPageGenerator implements Runnable {
	private final ScheduledExecutorService scheduler = Executors
			.newScheduledThreadPool(1);
	private static final Logger logger = Logger
			.getLogger("gov.usgs.subnetogram");
	private ConfigFile configFile;

	/**
	 * Class constructor
	 */
	public SgramPageGenerator(ConfigFile cf) {
		configFile = cf;
		configFile.put("applicationLaunch", "" + System.currentTimeMillis());
	}

	/**
	 * Class constructor
	 */
	public SgramPageGenerator(String cf) {
		configFile = new ConfigFile(cf);
		if (!configFile.wasSuccessfullyRead())
			throw new RuntimeException("Error reading config file "
					+ configFile);

		configFile.put("applicationLaunch", "" + System.currentTimeMillis());
		// settings = new SubnetOgramSettings(configFile);
	}

	public void run() {

		final Runnable pageGenerator = new Runnable() {
			public void run() {
				try {
					SgramSettings settings = new SgramSettings(configFile);
					SgramPageGenerator.generatePage(settings);
				} catch (Exception e) {
					logger.severe("Tom says this shouldn't happen.");
					logger.severe(e.getMessage());
					e.printStackTrace();
				}
			}
		};

		int interval = Integer.parseInt(configFile.getString("duration")) * 60; // in
																				// seconds

		long initialDelay = interval;
		if (Util.stringToBoolean(configFile.getString("delayStart"))) {
			initialDelay -= ((System.currentTimeMillis() / 1000) % interval);
		}
		System.out.println("delaying start " + initialDelay + " seconds.");
		System.out.println("First page at " + new Date(System.currentTimeMillis() + initialDelay * 1000));
		// wait until the top of the next interval and start running
		scheduler.scheduleAtFixedRate(pageGenerator, initialDelay, interval,
				TimeUnit.SECONDS);

		// Instant gratification
		pageGenerator.run();

		logger.finest("run scheduled!");
	}

	private static void generateImages(SgramImageSettings imageSettings) {
		// create full-sized image
		logger.finest("creating image");
		SgramImage sgramImage = new SgramImage(imageSettings);
		sgramImage.getData();
		sgramImage.generateTimeStampPNG();
		sgramImage.generateCurrentPNG();
	}

	private static void generatePage(SgramSettings settings) {
		logger.finest("generating a page...");
		ConfigFile cf = settings.configFile;
		SgramImageSettings imageSettings = new SgramImageSettings(
				cf.getSubConfig("image", true));
		SgramThumbnailSettings thumbnailSettings = new SgramThumbnailSettings(
				cf.getSubConfig("thumbnail", true));

		// create images
		generateImages(imageSettings);
		generateImages(thumbnailSettings);

		// write HTML
		if (cf.getBoolean("writeHtml")) {
			List<SgramPage> pages = new LinkedList<SgramPage>();

			SgramSinglePageSettings pageSettings = new SgramSinglePageSettings(
					cf.getSubConfig("page", true));
			pages.add(new SgramSinglePage(pageSettings, imageSettings));
			pages.add(new SgramSinglePageTimeStamp(pageSettings, imageSettings));

			SgramMosaicSettings mosaicSettings = new SgramMosaicSettings(
					cf.getSubConfig("mosaic", true));
			pages.add(new SgramMosaic(mosaicSettings, thumbnailSettings));
			pages.add(new SgramMosaicTimeStamp(mosaicSettings,
					thumbnailSettings));

			SgramDailyMosaicSettings dailyMosaicSettings = new SgramDailyMosaicSettings(
					cf.getSubConfig("mosaic", true));
			pages.add(new SgramDailyMosaic(dailyMosaicSettings,
					thumbnailSettings));
			pages.add(new SgramDailyMosaicTimeStamp(dailyMosaicSettings,
					thumbnailSettings));

			for (SgramPage page : pages)
				page.generateHTML();
		}
	}

	public static void main(String[] args) {
		logger.setLevel(Level.ALL);
		logger.finest("creating SgramPageGenerator");

		SgramPageGenerator pageGen = new SgramPageGenerator(
				"config/Little_Sitkin.config");
		new Thread(pageGen).start();
	}
}

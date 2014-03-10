package gov.usgs.subnetogram.page.mosaic.daily;

import gov.usgs.subnetogram.image.SgramThumbnailSettings;
import gov.usgs.subnetogram.page.mosaic.SgramMosaic;
import gov.usgs.util.Time;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;

import org.thymeleaf.context.Context;

public class SgramDailyMosaic extends SgramMosaic {
	protected final static long ONE_DAY_MILLIS = 1000 * 60 * 60 * 24;

	protected SgramDailyMosaicSettings settings;

	public SgramDailyMosaic(SgramDailyMosaicSettings settings,
			SgramThumbnailSettings thumbnailSettings) {
		
		
		super(settings, thumbnailSettings);
		this.settings = settings;
		long now = System.currentTimeMillis();
		Calendar c = Calendar.getInstance();
		Date d = new Date(now - (now % ONE_DAY_MILLIS));
		thumbnails = createThumbnailSettings(d, thumbnailSettings);
		subnetLinks = getDailySubnetLinks();

		Calendar cal = Calendar.getInstance();
		cal.setTime(d);
		cal.add(Calendar.DATE, 1);
		nextTime = cal.getTime();

		cal.setTime(d);
		cal.add(Calendar.DATE, -1);
		previousTime = cal.getTime();
		startTime = previousTime;

		previousSubnetFile = generateDailySubnetAddress(settings.previousSubnetName);
		nextSubnetFile = generateDailySubnetAddress(settings.nextSubnetName);

		c.setTime(d);
		c.add(Calendar.DATE, 1);
		nextFileName = generateDailyFileName(c.getTime(), settings.dailyFileNameFormat,
				settings.subnetName);
		nextPath = settings.generateFilePath(c.getTime(),
				settings.nextFilePathDateFormat, settings.subnetName);
		nextRelPath = settings.getRelativePath(settings.getBareFilePath(),
				nextPath);

		c.setTime(d);
		c.add(Calendar.DATE, -1);
		previousFileName = generateDailyFileName(c.getTime(), settings.dailyFileNameFormat,
				settings.subnetName);
		previousPath = settings.generateFilePath(c.getTime(),
				settings.previousFilePathDateFormat, settings.subnetName);
		previousRelPath = settings.getRelativePath(settings.getBareFilePath(),
				previousPath);

	}

	public String generateDailyFileName(Date time, String dateFormat,
			String subnetName) {
		StringBuilder sb = new StringBuilder();
		sb.append(subnetName);

		if (dateFormat != null)
			sb.append(Time.format(dateFormat, time));

		sb.append(settings.fileSuffix);
		sb.append(settings.dailyFileSuffix);
		return sb.toString();
	}

	protected String generateDailySubnetAddress(String subnet) {
		String nextSubnetPath = settings.generateFilePath(settings.endTime,
				null, subnet);
		String nextSubnetRelPath = settings.getRelativePath(
				settings.getBareFilePath(), nextSubnetPath);
		String nextSubnetFileName = generateDailyFileName(settings.endTime,
				null, subnet);

		return nextSubnetRelPath + nextSubnetFileName + settings.fileExtension;
	}

	protected LinkedHashMap<String, String> getDailySubnetLinks() {
		LinkedHashMap<String, String> subnetLinks = new LinkedHashMap<String, String>();
		for (String subnet : settings.subnets)
			subnetLinks.put(subnet, generateDailySubnetAddress(subnet));

		return subnetLinks;
	}

	public void generateHTML() {
		applyTemplate();
	}

	protected void applySettings(Context ctx) {
		super.applySettings(ctx);
		Calendar cal = Calendar.getInstance();
		cal.setTime(settings.startTime);
		cal.add(Calendar.HOUR, 1);
		ctx.setVariable("textStartDate", settings.textDateFormat.format(settings.startTime));
	}

	public void applyTemplate() {
		Context ctx = new Context();
		settings.applySettings(ctx);
		applySettings(ctx);

		try {
			FileWriter fw = new FileWriter(fileName);
			templateEngine.process(templateName, ctx, fw);
			fw.close();
		} catch (IOException e) {
			fatalError(e.getLocalizedMessage());
		}
	}

}

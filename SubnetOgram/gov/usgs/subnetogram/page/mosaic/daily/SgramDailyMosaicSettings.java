package gov.usgs.subnetogram.page.mosaic.daily;

import gov.usgs.subnetogram.page.mosaic.SgramMosaicSettings;
import gov.usgs.util.ConfigFile;
import gov.usgs.util.Time;

import java.util.Date;

import org.thymeleaf.context.Context;

public class SgramDailyMosaicSettings extends SgramMosaicSettings {

	public final Date startTime;
	public final int duration;

	public SgramDailyMosaicSettings(ConfigFile cf) {
		super(cf);
		long now = System.currentTimeMillis();
		startTime = new Date(now - (now % (1000 * 60 * 60 * 24)));
		duration = 1440;
	}

	protected void setDefaults(ConfigFile cf) {
		super.setDefaults(cf);
	}

	public String generateFileName(Date time, String dateFormat,
			String subnetName) {
		StringBuilder sb = new StringBuilder();
		sb.append(subnetName);

		if (dateFormat != null)
			sb.append(Time.format(dateFormat, time));

		sb.append(fileSuffix);
		return sb.toString();
	}

	/**
	 * 
	 */
	public String getBareFileName() {
		return getBareDailyFileName();
	}

	/**
	 * 
	 */
	public String getTimeStampFileName() {
		return getTimeStampDailyFileName();
	}

	public String getNextMosaicTitle() {
		return "Recent Mosaic";
	}

	public String getNextMosaicPage() {
		return super.getBareFileName();
	}

	/**
	 * {@inheritDoc}
	 */
	public void applySettings(Context ctx) {
		super.applySettings(ctx);

		ctx.setVariable("startTime", startTime);
		ctx.setVariable("duration", duration);
		ctx.setVariable("nextMosaicTitle", getNextMosaicTitle());
	}
}

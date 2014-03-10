package gov.usgs.netRSFileMover;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Logger;

/**
 * A class to write to an output stream at a specified rate. Intended to be used
 * with output rates that are small but above 1bps. This class will block once
 * it has written as many bytes as it should in a given timeWindow
 * 
 * @author Tom Parker
 * 
 */
public class ThrottledOutputStream extends OutputStream {

	public static final int DEFAULT_TIME_WINDOW_MS = 250;

	private final int bytesPerSecond;
	private final OutputStream outStream;

	private int timeWindowMS;
	private int bytesPerWindow;

	private long windowStart;
	private long bytesWriten;

	private final static Logger LOGGER = Logger.getLogger(NetRSSettings.class
			.getName());

	/**
	 * Simple constructor
	 * 
	 * @param outStream
	 *            OutputStream to write to
	 * @param bytesPerSecond
	 *            as int to limit upper bound on write speed
	 */
	ThrottledOutputStream(OutputStream outStream, int bytesPerSecond) {

		LOGGER.finest("constructing ThrottledOutputStream at " + bytesPerSecond
				+ " bps");
		this.bytesPerSecond = bytesPerSecond;
		this.outStream = outStream;
		windowStart = System.currentTimeMillis();

		setTimeWindow(DEFAULT_TIME_WINDOW_MS);
	}

	/**
	 * timeWindowMS mutator method.
	 * 
	 * @param timeWindow
	 *            window length in ms
	 */
	public void setTimeWindow(int timeWindow) {
		timeWindowMS = timeWindow;
		bytesPerWindow = timeWindowMS * bytesPerSecond / 1000;

		LOGGER.finest("I will write no more than " + bytesPerWindow
				+ " bytes every " + timeWindowMS + "ms.");
	}

	/**
	 * write 4 bytes and block if it's time.
	 */
	public void write(int b) throws IOException {

		outStream.write(b);
		bytesWriten += 4;

		if (bytesWriten >= bytesPerWindow) {
			bytesWriten = 0;

			long now = System.currentTimeMillis();
			long sleep = windowStart + timeWindowMS - now;
			if (sleep > 0) {
				try {
					LOGGER.finest("sleeping for " + sleep + " ms.");
					Thread.sleep(sleep);
				} catch (InterruptedException e) {
				} finally {
					windowStart = System.currentTimeMillis();
				}
			}
		}
	}
}

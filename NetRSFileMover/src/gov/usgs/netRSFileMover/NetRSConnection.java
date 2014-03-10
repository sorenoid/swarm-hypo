package gov.usgs.netRSFileMover;

import gov.usgs.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Logger;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.io.CopyStreamEvent;
import org.apache.commons.net.io.CopyStreamListener;

/**
 * A class to hold a FTP connection to a NetRS.
 * 
 * @author Tom Parker
 * 
 */
public class NetRSConnection {
	private static final int ONE_MINUTE = 1000 * 60;
	private static final long ONE_DAY = ONE_MINUTE * 60 * 24;

	private static final Logger LOGGER = Log.getLogger(NetRSConnection.class
			.getName());

	private final NetRSSettings settings;
	private final SimpleDateFormat fileNameFormat;

	private FTPClient ftp;

	private long pollTime;
	private final long endTime;
	private final long oldestPollTime;

	/**
	 * Simple constructor
	 * 
	 * @param settings
	 */
	public NetRSConnection(NetRSSettings settings) {

		this.settings = settings;

		fileNameFormat = new SimpleDateFormat(settings.fileNameFormat);
		fileNameFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

		long startTime = System.currentTimeMillis();
		endTime = startTime + (settings.duration * ONE_MINUTE);

		// Find start of current file and backup one interval. Do not attempt to
		// transfer the file that is currently being written.
		pollTime = startTime - (startTime % (settings.duration * ONE_MINUTE));
		pollTime -= settings.duration * ONE_MINUTE;

		// Retrieve files that are at most this old
		oldestPollTime = pollTime - (settings.maxDays * ONE_DAY);

		LOGGER.info("Will retreive files from  "
				+ fileNameFormat.format(new Date(oldestPollTime)) + " to "
				+ fileNameFormat.format(new Date(pollTime)));

		ftp = new FTPClient();

		ftp.setConnectTimeout(settings.connectTimeout * 1000);
		if (settings.printHash)
			ftp.setCopyStreamListener(getCopyStreamListener());
	}

	/**
	 * Connect to NetRS, setting receive window if needed
	 * 
	 * @throws IOException
	 */
	private void connect() throws IOException {

		LOGGER.fine("Connecting to " + settings.address);
		try {

			ftp.connect(settings.address);
			ftp.login(settings.userName, settings.password);

			if (settings.passiveFTP)
				ftp.enterLocalPassiveMode();
			else
				ftp.enterLocalActiveMode();

			// Grrr... This is not guaranteed to work and I can find no way to
			// tell if it did work. Best to sniff the wire to check. There must
			// be a better way.
			if (settings.windowSize > 0)
				ftp.setReceiveBufferSize(settings.windowSize);

		} catch (IOException e) {
			if (ftp.isConnected())
				ftp.disconnect();

			throw e;
		}
	}

	/**
	 * Poll the next file from the receiver, updating pollTime
	 * 
	 * @throws IOException
	 */
	public void poll() {

		// Stop polling if I've been running too long.
		if (System.currentTimeMillis() > endTime) {
			pollTime = Long.MIN_VALUE;
			LOGGER.info("I've been running too long. I'll stop");
			return;
		}

		String filename = fileNameFormat.format(new Date(pollTime));
		LOGGER.info("Polling " + settings.systemName + " for " + filename);

		pollTime -= settings.duration * ONE_MINUTE;

		File outFile = new File(settings.outputDir + File.separator
				+ settings.systemName + File.separator + filename);

		// Just return if I already have the file
		if (outFile.exists()) {
			LOGGER.fine("I already have " + filename + " skipping it.");
			return;
		}

		try {
			connect();
		} catch (IOException e) {
			LOGGER.warning("Could not connect to " + settings.systemName);
		}

		getFile(filename, outFile);

		if (settings.depthFirst && !polledLast())
			poll();

		if (ftp.isConnected())
			try {
				ftp.disconnect();
			} catch (IOException e) {
			}
	}

	/**
	 * Pull a single file from the receiver
	 * 
	 * @param remoteFile
	 * @param localFile
	 * @return true if file was retrieved
	 * @throws IOException
	 */
	private void getFile(String remoteFile, File outFile) {

		// download to a temp file to help avoid exposing partial files
		File tmpFile = new File(settings.outputDir + File.separator + "tmp"
				+ File.separator + outFile.getName());
		tmpFile.getParentFile().mkdirs();

		OutputStream output;
		long resumeOffset = tmpFile.length();
		boolean resume = settings.resumeTransfer && resumeOffset > 0;
		try {
			output = new FileOutputStream(tmpFile, resume);
		} catch (FileNotFoundException e) {
			LOGGER.warning("Can't create temp file " + tmpFile);
			return;
		}

		if (settings.bytesPerSecond > 0)
			output = new ThrottledOutputStream(output, settings.bytesPerSecond);

		long now = System.currentTimeMillis();

		if (resume) {
			LOGGER.info("resuming transfer at " + resumeOffset + " bytes.");
			ftp.setRestartOffset(resumeOffset);
		}
		boolean result = false;
		try {
			ftp.setFileType(FTP.BINARY_FILE_TYPE);
			result = ftp.retrieveFile(remoteFile, output);
		} catch (IOException e) {
			LOGGER.warning("Couldn't retrieve " + remoteFile);
			e.printStackTrace();
		}

		if (output != null)
			try {
				output.close();
			} catch (IOException e) {
			}

		if (result) {
			if (settings.printHash)
				System.out.println();
			LOGGER.fine("got file in " + (System.currentTimeMillis() - now)
					+ " ms");
			
			try {
				moveFile(tmpFile, outFile);
			} catch (IOException e) {
				LOGGER.warning("Couldn't write file to "
						+ outFile.getAbsolutePath() + ". " + e.getMessage());
			}

		} else {
			tmpFile.delete();
			LOGGER.info("Couldn't get file. Server replied: "
					+ ftp.getReplyString());
			LOGGER.info("Undeterred I will continue.");
		}
	}

	/**
	 *  copy and remove file. More robust than using .copyTo()
	 * @param sourceFile
	 * @param destFile
	 * @throws IOException
	 */
	private void moveFile(File sourceFile, File destFile) throws IOException {
		File p = destFile.getParentFile();
		if (!p.exists())
			p.mkdirs();
		
		if (!destFile.exists())
			destFile.createNewFile();
		
		FileChannel source = null;
		FileChannel dest = null;
		
		try {
			source = new FileInputStream(sourceFile).getChannel();
			dest = new FileOutputStream(destFile).getChannel();
			dest.transferFrom(source, 0, source.size());
		}
		finally {
			if (source != null)
				source.close();
			
			if (dest != null)
				dest.close();
		}
		
		sourceFile.delete();
	}

	/**
	 * Decide if I've checked for all files.
	 * 
	 * @return true if I've polled all files
	 */
	public boolean polledLast() {
		return pollTime < oldestPollTime;
	}

	/**
	 * create a CopyStreamListener to print hash marks during downloads. Based
	 * on FTPClient sample code.
	 * 
	 * @return a hash-printing CopyStreamListener
	 */
	private static CopyStreamListener getCopyStreamListener() {
		return new CopyStreamListener() {
			private long kBytesTotal = 0;

			public void bytesTransferred(CopyStreamEvent event) {
				bytesTransferred(event.getTotalBytesTransferred(),
						event.getBytesTransferred(), event.getStreamSize());
			}

			public void bytesTransferred(long totalBytesTransferred,
					int bytesTransferred, long streamSize) {
				long kBytes = totalBytesTransferred / (1024);
				
				for (long l = kBytesTotal; l < kBytes; l++)
					System.out.print("#");
				
				kBytesTotal = kBytes;

			}
		};
	}
}
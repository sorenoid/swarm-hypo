package gov.usgs.subnetogram.page;

import gov.usgs.subnetogram.image.SgramImageSettings;

public class SgramSinglePageTimeStamp extends SgramSinglePage {

	public SgramSinglePageTimeStamp(SgramSinglePageSettings pageSettings,
			SgramImageSettings imageSettings) {
		super(pageSettings, imageSettings);

		isCurrent = false;

		pathToRoot = settings.getRelativePath(settings.getTimeStampFilePath(), settings.pathRoot);
		networkLinks = getNetworkLinks();

		subnetLinks = getSubnetLinks();
		previousSubnetFile = generateSubnetAddress(settings.previousSubnetName);
		nextSubnetFile = generateSubnetAddress(settings.nextSubnetName);

		fileName = settings.getTimeStampFilePath()
				+ settings.getTimeStampFileName() + settings.fileExtension;

		imageName = settings.getRelativePath(settings.getTimeStampFilePath(),
				imageSettings.getTimeStampFilePath())
				+ imageSettings.getTimeStampFileName();

		nextRelPath = settings.getRelativePath(settings.getTimeStampFilePath(),
				nextPath);
		previousRelPath = settings.getRelativePath(
				settings.getTimeStampFilePath(), previousPath);

		currentRelPath = settings.getRelativePath(
				settings.getTimeStampFilePath(), settings.pathRoot);
		if (settings.networkName != null)
			currentRelPath += settings.networkName + '/';

		currentFile = currentRelPath + settings.subnetName + '/'
				+ settings.subnetName + settings.fileSuffix
				+ settings.fileExtension;
		currentDailyMosaic = currentRelPath + settings.subnetName + '/'
				+ settings.subnetName + "Daily" + settings.mosaicSuffix
				+ settings.fileExtension;
		currentMosaic = currentRelPath + settings.subnetName + '/'
				+ settings.subnetName + settings.mosaicSuffix
				+ settings.fileExtension;
		

	}

	protected String generateSubnetAddress(String subnet) {
		String nextSubnetPath = settings.generateFilePath(settings.endTime,
				settings.filePathDateFormat, subnet);
		String nextSubnetRelPath = settings.getRelativePath(
				settings.getTimeStampFilePath(), nextSubnetPath);
		String nextSubnetFileName = settings.generateFileName(settings.endTime,
				settings.fileNameDateFormat, subnet);

		return nextSubnetRelPath + nextSubnetFileName + settings.fileExtension;
	}

}

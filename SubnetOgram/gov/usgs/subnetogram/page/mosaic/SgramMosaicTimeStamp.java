package gov.usgs.subnetogram.page.mosaic;

import java.util.LinkedHashMap;

import gov.usgs.subnetogram.image.SgramThumbnailSettings;

public class SgramMosaicTimeStamp extends SgramMosaic {

	public SgramMosaicTimeStamp(SgramMosaicSettings settings,
			SgramThumbnailSettings thumbnailSettings) {
		super(settings, thumbnailSettings);
		
		pathToRoot = settings.getRelativePath(settings.getTimeStampFilePath(), settings.pathRoot);
		networkLinks = getNetworkLinks();

		subnetLinks = getTsSubnetLinks();
		previousSubnetFile = generateTsSubnetAddress(settings.previousSubnetName);
		nextSubnetFile = generateTsSubnetAddress(settings.nextSubnetName);
		isCurrent = false;
		fileName = settings.getTimeStampFilePath()
				+ settings.getTimeStampFileName() + settings.fileExtension;
		imageName = settings.getRelativePath(settings.getTimeStampFilePath(),
				imageSettings.getTimeStampFilePath())
				+ imageSettings.getTimeStampFileName();
		nextRelPath = settings.getRelativePath(settings.getTimeStampFilePath(),
				nextPath);
		previousRelPath = settings.getRelativePath(
				settings.getTimeStampFilePath(), previousPath);
		
		currentRelPath = settings.getRelativePath(settings.getTimeStampFilePath(), settings.pathRoot);
		if (settings.networkName != null)
			currentRelPath += settings.networkName + '/';
		currentFile = currentRelPath + settings.subnetName + '/' + settings.subnetName + settings.fileSuffix
				+ settings.fileExtension;
		currentDailyMosaic = currentRelPath + settings.subnetName + '/' + settings.subnetName + "Daily"
				+ settings.mosaicSuffix + settings.fileExtension;
		currentMosaic = currentRelPath + settings.subnetName + '/' + settings.subnetName + settings.mosaicSuffix
				+ settings.fileExtension;

	}

	protected LinkedHashMap<String, String> getTsSubnetLinks() {
		LinkedHashMap<String, String> subnetLinks = new LinkedHashMap<String, String>();
		for (String subnet : settings.subnets)
			subnetLinks.put(subnet, generateTsSubnetAddress(subnet));

		return subnetLinks;
	}

	protected String generateTsSubnetAddress(String subnet) {
		String nextSubnetPath = settings.generateFilePath(settings.endTime,
				settings.filePathDateFormat, subnet);
		String nextSubnetRelPath = settings.getRelativePath(
				settings.getTimeStampFilePath(), nextSubnetPath);
		String nextSubnetFileName = settings.generateFileName(settings.endTime,
				settings.fileNameDateFormat, subnet);

		return nextSubnetRelPath + nextSubnetFileName + settings.fileExtension;
	}
}

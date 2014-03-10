package gov.usgs.subnetogram.page;

import gov.usgs.subnetogram.image.SgramImageSettings;
import gov.usgs.util.ConfigFile;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import org.thymeleaf.templateresolver.TemplateResolver;

/**
 * Create a page with a single subnetOgram image.
 * 
 * @author Tom Parker
 * 
 */
public class SgramSinglePage implements SgramPage {

	protected ConfigFile cf;
	protected static final Logger logger = Logger
			.getLogger("gov.usgs.subnetogram");
	protected SgramImageSettings imageSettings;
	protected SgramSinglePageSettings settings;
	protected TemplateEngine templateEngine;
	protected String templateName;
	protected LinkedHashMap<String, String> subnetLinks;
	protected LinkedHashMap<String, String> networkLinks;

	protected String previousSubnetFile;
	protected String nextSubnetFile;
	protected String nextFile;
	protected boolean isCurrent;
	protected String fileName;
	protected String imageName;
	protected String nextPath;
	protected String nextRelPath;
	protected String nextFileName;
	protected String previousPath;
	protected String previousRelPath;
	protected String previousFileName;
	protected String currentMosaic;
	protected String currentFile;
	protected String currentDailyMosaic;
	protected String currentRelPath;
	protected Date nextTime;
	protected Date previousTime;
	protected String pathToRoot;
	
	/** Time of earliest data point */
	protected Date startTime;

	/** Time of last data point */
	protected Date endTime;
	/** Time zone offset in milliseconds */
	public final int timeZoneOffset;


	public SgramSinglePage(SgramSinglePageSettings pageSettings,
			SgramImageSettings imageSettings) {

		if (pageSettings == null)
			throw new RuntimeException("null pageSettings");

		if (imageSettings == null)
			throw new RuntimeException("null imageSettings");

		this.imageSettings = imageSettings;
		this.settings = pageSettings;

		pathToRoot = settings.getRelativePath(settings.getBareFilePath(), settings.pathRoot);

		networkLinks = getNetworkLinks();
		subnetLinks = getSubnetLinks();
		previousSubnetFile = generateSubnetAddress(settings.previousSubnetName);
		nextSubnetFile = generateSubnetAddress(settings.nextSubnetName);

		// startTime is optional, but has rules. Lots of rules.
		// TODO: figure this stuff out
		Calendar cal = Calendar.getInstance();
		cal.setTime(settings.startTime);
		cal.setTimeZone(settings.timeZone);
		cal.add(Calendar.MINUTE, -settings.embargo);

		// always reference zero milliseconds
		cal.add(Calendar.MILLISECOND, -1
				* ((int) (cal.getTimeInMillis() % 1000)));

		if (settings.onMark)
			cal.add(Calendar.SECOND, -1
					* ((int) (cal.getTimeInMillis() / 1000) % (settings.period * 60)));

		startTime = cal.getTime();

		cal.add(Calendar.SECOND, settings.duration * 60);
		endTime = cal.getTime();

		timeZoneOffset = settings.timeZone.getOffset(endTime.getTime());


		cal.setTime(endTime);
		cal.add(Calendar.MINUTE, settings.duration);
		nextTime = cal.getTime();

		cal.add(Calendar.MINUTE, -2 * settings.duration);
		previousTime = cal.getTime();

		nextPath = settings.generateFilePath(nextTime,
				settings.nextFilePathDateFormat, settings.subnetName);
		nextRelPath = settings.getRelativePath(settings.getBareFilePath(),
				nextPath);
		nextFileName = settings.generateFileName(nextTime,
				settings.nextFileNameDateFormat, settings.subnetName);

		isCurrent = true;

		templateName = "subnetOgramImage";
		logger.finest("creating new SgramPage");

		fileName = settings.getBareFilePath() + settings.getBareFileName()
				+ settings.fileExtension;
		imageName = settings.getRelativePath(settings.getBareFilePath(),
				imageSettings.getBareFilePath())
				+ imageSettings.getBareFileName();

		previousPath = settings.generateFilePath(previousTime,
				settings.previousFilePathDateFormat, settings.subnetName);
		previousRelPath = settings.getRelativePath(settings.getBareFilePath(),
				previousPath);
		previousFileName = settings.generateFileName(previousTime,
				settings.previousFileNameDateFormat, settings.subnetName);

		currentRelPath = settings.getRelativePath(settings.getBareFilePath(), settings.pathRoot);
		if (settings.networkName != null)
			currentRelPath += settings.networkName + '/';
		currentFile = currentRelPath + settings.subnetName + '/' + settings.subnetName + settings.fileSuffix
				+ settings.fileExtension;
		currentDailyMosaic = currentRelPath + settings.subnetName + '/' + settings.subnetName + "Daily"
				+ settings.mosaicSuffix + settings.fileExtension;
		currentMosaic = currentRelPath + settings.subnetName + '/' + settings.subnetName + settings.mosaicSuffix
				+ settings.fileExtension;

		
		
		
		initializeTemplateEngine();
	}

	protected void initializeTemplateEngine() {
		TemplateResolver templateResolver;

		templateEngine = new TemplateEngine();

		// resolver for compiled in templates
		templateResolver = new ClassLoaderTemplateResolver();
		templateResolver.setPrefix("templates/" + settings.template + "/");
		templateResolver.setSuffix(settings.fileExtension);
		templateEngine.addTemplateResolver(templateResolver);

		// resolver for filesystem-based templates
		templateResolver = new FileTemplateResolver();
		templateResolver.setTemplateMode("XHTML");
		templateResolver.setPrefix(Matcher.quoteReplacement("templates"
				+ File.separator + settings.template + File.separator));
		templateResolver.setSuffix(settings.fileExtension);
		templateResolver.setCacheTTLMs(3600000L);

		templateEngine.addTemplateResolver(templateResolver);
	}

	protected String generateSubnetAddress(String subnet) {
		String nextSubnetPath = settings.generateFilePath(endTime,
				null, subnet);
		String nextSubnetRelPath = settings.getRelativePath(
				settings.getBareFilePath(), nextSubnetPath);
		String nextSubnetFileName = settings.generateFileName(endTime,
				null, subnet);

		return nextSubnetRelPath + nextSubnetFileName + settings.fileExtension;
	}

	protected String generateNetworkAddress(String network) {
		return pathToRoot + network + File.separator + "index" + settings.fileExtension;
	}

	protected LinkedHashMap<String, String> getSubnetLinks() {
		LinkedHashMap<String, String> subnetLinks = new LinkedHashMap<String, String>();
		for (String subnet : settings.subnets)
			subnetLinks.put(subnet, generateSubnetAddress(subnet));

		return subnetLinks;
	}

	protected LinkedHashMap<String, String> getNetworkLinks() {
		LinkedHashMap<String, String> networkLinks = new LinkedHashMap<String, String>();
		for (String network : settings.networks)
			networkLinks.put(network, generateNetworkAddress(network));

		return networkLinks;
	}
	protected void applySettings(Context ctx) {
		ctx.setVariable("imageSettings", imageSettings);
		ctx.setVariable("fileName", fileName);
		ctx.setVariable("imageName", imageName);
		ctx.setVariable("mosaicAddress", currentMosaic);
		ctx.setVariable("subnetLinks", subnetLinks);
		ctx.setVariable("networkLinks", networkLinks);
		ctx.setVariable("previousSubnetFile", previousSubnetFile);
		ctx.setVariable("nextSubnetFile", nextSubnetFile);
		ctx.setVariable("nextFile", nextRelPath + nextFileName
				+ settings.fileExtension);
		ctx.setVariable("previousFile", previousRelPath + previousFileName
				+ settings.fileExtension);
		ctx.setVariable("isCurrent", isCurrent);
		ctx.setVariable("currentFile", currentFile);
		ctx.setVariable("currentDailyMosaic", currentDailyMosaic);
		ctx.setVariable("startTime", startTime);
		ctx.setVariable("endTime", endTime);
		ctx.setVariable("timeZoneOffset", timeZoneOffset);
		ctx.setVariable("textStartTime", settings.textTimeFormat.format(startTime));
//		ctx.setVariable("textEndTime", settings.textTimeFormat.format(endTime));
		ctx.setVariable("textStartDate", settings.textDateFormat.format(startTime));
		ctx.setVariable("pathToRoot", pathToRoot);
	}
	
	public void applyTemplate(String fileName, String imageName) {
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

	public void fatalError(String msg) {
		logger.severe(msg);
		if (settings.onError.equals("exit"))
			System.exit(1);
	}

	public void generateHTML() {

		applyTemplate(fileName, imageName);
	}

}
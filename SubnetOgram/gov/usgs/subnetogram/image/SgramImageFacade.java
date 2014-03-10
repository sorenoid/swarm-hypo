package gov.usgs.subnetogram.image;

/**
 * A facade class demonstrating use of {@link SubnetOgram}.
 * 
 * @author Tom Parker
 * 
 */
public class SgramImageFacade {
	//
	// // JSAP related stuff.
	// public static String JSAP_PROGRAM_NAME =
	// "java gov.usgs.subnetogram.image.SubnetOgramImageFacade";
	// public static String JSAP_EXPLANATION_PREFACE =
	// "SubnetOgram image facade\n"
	// + "\n"
	// +
	// "I will create a subnetOgram iamge for you to demonstrate use of the class."
	// + "Pay attention." + "\n";
	//
	// private static final String DEFAULT_JSAP_EXPLANATION =
	// "All output goes to standard error.\n"
	// + "The command line takes precedence over the config file.\n";
	//
	// private static final Parameter[] DEFAULT_JSAP_PARAMETERS = new
	// Parameter[] {
	// new FlaggedOption(
	// "dataSource",
	// JSAP.STRING_PARSER,
	// null,
	// JSAP.REQUIRED,
	// 's',
	// "dataSource",
	// "The source of seismic data. Copy it from your swarm.config file. You'll probably have to put it in quotes. It should look something like: pubavo1;wws:pubavo1.wr.usgs.gov:16022:10000:0"),
	// new FlaggedOption(
	// "channel",
	// JSAP.STRING_PARSER,
	// null,
	// JSAP.REQUIRED,
	// 'c',
	// "channel",
	// "A space-seperated SCNL to plot. Repeat argument once for each channel. Quote the channel name to help with the spaces."),
	// new FlaggedOption("height", JSAP.STRING_PARSER, null,
	// JSAP.REQUIRED, 'h', "height", "Height of generated image."),
	// new FlaggedOption("width", JSAP.STRING_PARSER, null, JSAP.REQUIRED,
	// 'w', "width", "Width of generated image."),
	// new FlaggedOption("duration", JSAP.STRING_PARSER, null,
	// JSAP.REQUIRED, 'd', "duration",
	// "Duration of generated image in minutes."),
	// new FlaggedOption("name", JSAP.STRING_PARSER, null, JSAP.REQUIRED,
	// 'n', "name", "Name of subnet"),
	// new FlaggedOption("fileType", JSAP.STRING_PARSER, "png",
	// JSAP.REQUIRED, 't', "type",
	// "File type must be one of: jpg, png, ps."), };
	//
	// public static JSAPResult getArguments(String[] args) {
	// JSAPResult config = null;
	// try {
	// SimpleJSAP jsap = new SimpleJSAP(JSAP_PROGRAM_NAME,
	// JSAP_EXPLANATION_PREFACE + DEFAULT_JSAP_EXPLANATION,
	// DEFAULT_JSAP_PARAMETERS);
	//
	// config = jsap.parse(args);
	//
	// if (jsap.messagePrinted()) {
	// // The following error message is useful for catching the case
	// // when args are missing, but help isn't printed.
	// if (!config.getBoolean("help"))
	// System.err.println("Try using the --help flag.");
	//
	// System.exit(1);
	// }
	// } catch (Exception ex) {
	// ex.printStackTrace();
	// System.exit(1);
	// }
	// return config;
	// }
	//
	// /**
	// * Example method to create a plot and thumbnail from hardcoded
	// parameters.
	// *
	// * @param args
	// * ignored
	// * @throws PlotException
	// */
	// public static void main(String[] args) throws PlotException {
	//
	// JSAPResult config = getArguments(args);
	//
	// Logger logger = Logger.getLogger("gov.usgs.subnetogram");
	// logger.setLevel(Level.FINEST);
	//
	// ConfigFile cf = new ConfigFile();
	// cf.put("dataSource", config.getString("dataSource"));
	// for (String s : (String[]) config.getObjectArray("channel",
	// new String[0]))
	// cf.put("channel", s);
	//
	// cf.put("startTime", "-" + config.getString("duration") + "i");
	// cf.put("subnetName", config.getString("name"));
	// cf.put("applicationLaunch", "" + System.currentTimeMillis());
	// cf.put("filePathDateFormat", "");
	// cf.put("fileNameDateFormat", "");
	//
	// cf.toString();
	// SubnetOgramImageSettings settings = new SubnetOgramImageSettings(cf);
	// SubnetOgramImage sgram = new SubnetOgramImage(settings);
	// if (config.getString("fileType").equals("png"))
	// sgram.generatePNG();
	// else if (config.getString("fileType").equals("jpg"))
	// sgram.generateJPEG();
	// else if (config.getString("fileType").equals("ps"))
	// sgram.generatePS();
	// }
}
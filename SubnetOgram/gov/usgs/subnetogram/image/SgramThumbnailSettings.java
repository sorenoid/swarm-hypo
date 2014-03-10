package gov.usgs.subnetogram.image;

import gov.usgs.util.ConfigFile;

import java.util.regex.Matcher;

import org.thymeleaf.context.Context;

/**
 * A class to hold setting specific to a given SubnetOgram thumbnail
 * 
 * Adding a setting? Yes, it's a bit tedious. However it's all here and it makes
 * everything else easy.
 * 
 * 1. Set default if appropriate
 * 
 * 2. Create public final instance variable
 * 
 * 3. Apply default setting in applyDefaults()
 * 
 * 4. Set value in constructor
 * 
 * 5. Add variable to toString()
 * 
 * 6. Add variable to applySettings()
 * 
 * 7. Add variable to getConfigFile()
 * 
 * @author Tom Parker
 * 
 */

public class SgramThumbnailSettings extends SgramImageSettings {

	/**
	 * The full-sized image
	 */
	public final String target;

	/**
	 * Holds the settings required to create a subnetOgramImage
	 * 
	 * @param cf
	 *            The fully parsed config file
	 */
	public SgramThumbnailSettings(ConfigFile cf) {
		super(cf);
		cf.put("target",
				getTimeStampFileName().replaceFirst(Matcher.quoteReplacement(fileSuffix), ""));
		target = cf.getString("target");
	}

	/**
	 * {@inheritDoc}
	 */
	protected void setDefaults(ConfigFile cf) {
		super.setDefaults(cf);
	}

	/**
	 * return settings with all fields decrimented by duration
	 */
	protected ConfigFile getConfigFileOffset(int offset) {
		ConfigFile cf = super.getConfigFileOffset(offset);

		return cf;
	}

	/**
	 * Generate the settings for the next image
	 * 
	 * @return
	 */
	public SgramThumbnailSettings getNext() {
		return new SgramThumbnailSettings(getConfigFileOffset(duration));
	}

	/**
	 * Generate the settings for the previous image
	 * 
	 * @return
	 */
	public SgramThumbnailSettings getPrevious() {
		
		ConfigFile c = getConfigFileOffset(-duration);

		SgramThumbnailSettings s = new SgramThumbnailSettings(c);

		return s;
	}

	/**
	 * {@inheritDoc}
	 */
	public String toString() {
		StringBuilder settings = new StringBuilder();
		settings.append(super.toString());

		settings.append("target = " + target + "\n");
		return settings.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	public void applySettings(Context ctx) {
		super.applySettings(ctx);

		ctx.setVariable("target", target);
	}
}

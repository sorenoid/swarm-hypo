package gov.usgs.subnetogram.page.mosaic;

import gov.usgs.subnetogram.image.SgramThumbnailSettings;
import gov.usgs.subnetogram.page.SgramSinglePage;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;

import org.thymeleaf.context.Context;

/**
 * A class to create a page with a mosaiced array of SubnetOgram thumbnail
 * images, each with a link to the full page.
 * 
 * @author Tom Parker
 */
public class SgramMosaic extends SgramSinglePage {

	protected LinkedList<LinkedList<SgramThumbnailSettings>> thumbnails;
	protected SgramMosaicSettings settings;

	public SgramMosaic(SgramMosaicSettings settings,
			SgramThumbnailSettings thumbnailSettings) {
		super(settings, thumbnailSettings);

		isCurrent = true;
		this.settings = settings;

		this.templateName = "subnetOgramMosaic";
		initializeTemplateEngine();

		thumbnails = createThumbnailSettings(startTime,
				thumbnailSettings);
	}

	protected LinkedList<LinkedList<SgramThumbnailSettings>> createThumbnailSettings(
			Date startTime, SgramThumbnailSettings thumbnailSettings) {

		LinkedList<LinkedList<SgramThumbnailSettings>> thumbnails = new LinkedList<LinkedList<SgramThumbnailSettings>>();

		LinkedList<SgramThumbnailSettings> thumbs = new LinkedList<SgramThumbnailSettings>();
		while (!thumbnailSettings.startTime.before(startTime)) {
			thumbs.offerFirst(thumbnailSettings);
			thumbnailSettings = thumbnailSettings.getPrevious();
		}

		int rowCount = (int) Math
				.ceil(((double) thumbs.size()) / settings.cols);
		for (int i = 0; i < rowCount; i++) {
			LinkedList<SgramThumbnailSettings> row = new LinkedList<SgramThumbnailSettings>();
			for (int j = 0; j < settings.cols; j++) {
				if (!thumbs.isEmpty())
					row.add(thumbs.removeFirst());
			}
			thumbnails.add(row);
		}
		return thumbnails;
	}

	public void generateHTML() {
		applyTemplate();
	}

	protected void applySettings(Context ctx) {
		super.applySettings(ctx);
		ctx.setVariable("thumbnails", thumbnails);
		ctx.setVariable("templateDir",
				settings.getRelativePath(fileName, settings.pathRoot)
						+ "templates/" + settings.template + "/");
		ctx.setVariable("nextMosaicAddress",
				settings.getRelativePath(fileName, settings.getBareFilePath())
						+ settings.getNextMosaicPage() + settings.fileExtension);
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
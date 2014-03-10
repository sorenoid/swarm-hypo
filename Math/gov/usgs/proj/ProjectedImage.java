package gov.usgs.proj;

import gov.usgs.util.ProgressListener;
import gov.usgs.util.ResourceReader;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

/**
 * 
 * $Log: not supported by cvs2svn $
 * @author Dan Cervelli
 * @version $Id: ProjectedImage.java,v 1.1 2007-04-24 21:45:39 dcervelli Exp $
 */
public class ProjectedImage
{
	public enum Interpolation
	{
		NEAREST_NEIGHBOR,
		BILINEAR
	}
	
	/**
	 * Coordinate system for image pixels. 
	 */
	private double deltaX;
	private double deltaY;
	private double originX;
	private double originY;
	
	/**
	 * Bounds of image-coordinate system.
	 */
	private double minX;
	private double maxX;
	private double minY;
	private double maxY;
	
	/**
	 * Pixels on the top and bottom of the image that should be ignored.
	 */
	private int deadPixelsN = 0;
	private int deadPixelsS = 0;
	
	/**
	 * Only pixels within this lat/lon box are considered useful.
	 */
	private GeoRange usefulRange;
	
	private BufferedImage image;
	
	/**
	 * The projection of the image.
	 */
	private Projection projection;
	
	public ProjectedImage()
	{}
	
	public ProjectedImage(String ifn) throws Exception
	{
		image = ImageIO.read(new File(ifn));
		calculateBounds();
	}

	public ProjectedImage(BufferedImage img)
	{
		image = img;
	}
	
	public void readWorldFile(String wfn)
	{
		ResourceReader rr = ResourceReader.getResourceReader(wfn);
		deltaX = Double.parseDouble(rr.nextLine());
		rr.nextLine();
		rr.nextLine();
		deltaY = Double.parseDouble(rr.nextLine());
		originX = Double.parseDouble(rr.nextLine());
		originY = Double.parseDouble(rr.nextLine());
		calculateBounds();
	}
	
	public ProjectedImage(BufferedImage img, Projection proj, double dx, double dy, double ox, double oy, int dpN, int dpS)
	{
		image = img;
		deltaX = dx;
		deltaY = dy;
		originX = ox;
		originY = oy;
		projection = proj;
		deadPixelsN = dpN;
		deadPixelsS = dpS;
		calculateBounds();
	}

	public void setDeadPixels(int n, int s)
	{
		deadPixelsN = n;
		deadPixelsS = s;
		calculateBounds();
	}
	
	public void setImage(BufferedImage img)
	{
		image = img;
	}
	
	public void setProjection(Projection p)
	{
		projection = p;
	}
	
	protected void calculateBounds()
	{
		double a = originX;
		double b = originX + deltaX * image.getWidth();
		minX = Math.min(a, b);
		maxX = Math.max(a, b);
		a = originY + deadPixelsS * deltaY;
		b = originY + deltaY * (image.getHeight() - (deadPixelsN + deadPixelsS));
		minY = Math.min(a, b);
		maxY = Math.max(a, b);
	}
	
	public void setUsefulRange(GeoRange gr)
	{
		usefulRange = gr;
	}
	
	public void setUsefulRangeToExtents()
	{
		usefulRange = new GeoRange(minX, maxX, minY, maxY);
	}
	
	public GeoRange getUsefulRange()
	{
		return usefulRange;
	}
	
	public Projection getProjection()
	{
		return projection;
	}
	
	public boolean containsXY(double x, double y)
	{
		return x >= minX && x <= maxX && y >= minY && y <= maxY;
	}
	
	public boolean containsLL(Point2D.Double ll)
	{
		if (usefulRange == null)
			return true;
		else 
			return usefulRange.contains(ll);
	}
	
	public int getSourcePixel(int alpha, double sx, double sy, Interpolation interp)
	{
		if (interp == Interpolation.NEAREST_NEIGHBOR)
		{
			int xo = (int)((sx - originX) / deltaX);
			int yo = (int)((sy - originY) / deltaY);
			if (xo >= image.getWidth() || yo >= image.getHeight() || xo < 0  || yo < 0)
				return 0xfff0f0;
			else
				return image.getRGB(xo, yo);
		}
		else if (interp == Interpolation.BILINEAR)
		{
			double xo = (sx - originX) / deltaX;
			double yo = (sy - originY) / deltaY;
			
			if (xo >= image.getWidth() - 1 || yo >= image.getHeight() - 1 || xo < 0  || yo < 0)
				return 0xfff0f0;
			
			int pul = image.getRGB((int)xo, (int)yo);
			int pur = image.getRGB((int)xo + 1, (int)yo);
			int plr = image.getRGB((int)xo + 1, (int)yo + 1);
			int pll = image.getRGB((int)xo, (int)yo + 1);
			
			int px = (int)xo;
			int py = (int)yo;
			double delx = xo - px;
			double dely = yo - py;
			
			int ul = pul & 0xff;
			int ur = pur & 0xff;
			int lr = plr & 0xff;
			int ll = pll & 0xff;
			int b = (int)Math.round(dely * delx * lr + (1 - delx) * dely * ll + (1 - delx) * (1 - dely) * ul + delx * (1 - dely) * ur);
			ul = (pul >> 8) & 0xff;
			ur = (pur >> 8) & 0xff;
			lr = (plr >> 8) & 0xff;
			ll = (pll >> 8) & 0xff;
			int g = (int)Math.round(dely * delx * lr + (1 - delx) * dely * ll + (1 - delx) * (1 - dely) * ul + delx * (1 - dely) * ur);
			ul = (pul >> 16) & 0xff;
			ur = (pur >> 16) & 0xff;
			lr = (plr >> 16) & 0xff;
			ll = (pll >> 16) & 0xff;
			int r = (int)Math.round(dely * delx * lr + (1 - delx) * dely * ll + (1 - delx) * (1 - dely) * ul + delx * (1 - dely) * ur);
			ul = (pul >> 24) & 0xff;
			ur = (pur >> 24) & 0xff;
			lr = (plr >> 24) & 0xff;
			ll = (pll >> 24) & 0xff;
			int a = (int)Math.round(dely * delx * lr + (1 - delx) * dely * ll + (1 - delx) * (1 - dely) * ul + delx * (1 - dely) * ur);
			
			return b | g << 8 | r << 16 | a << 24;
		}
		else
			return 0;
	}
	
	public ProjectedImage reprojectImage(Projection dest, double ox, double oy, double dx, double dy, int w, int h, Interpolation interp)
	{
		return reprojectImage(dest, ox, oy, dx, dy, w, h, interp, null);
	}
	
	public ProjectedImage reprojectImage(Projection dest, double ox, double oy, double dx, double dy, int w, int h, Interpolation interp, ProgressListener pl)
	{
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics g = img.getGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, w, h);
		g.dispose();
		for (int x = 0; x < w; x++)
		{
			for (int y = 0; y < h; y++)
			{
				double cx = ox + x * dx;
				double cy = oy - y * dy;
				Point2D.Double ll = dest.inverse(new Point2D.Double(cx, cy));
				if (!containsLL(ll))
					continue;
				Point2D.Double src = projection.forward(ll);
				if (containsXY(src.x, src.y))
					img.setRGB(x, y, getSourcePixel(0xff, src.x, src.y, interp));
			}
			if (pl != null)
				pl.progressDone((float)x / (float)w);
		}
		
		ProjectedImage pi = new ProjectedImage();
		pi.image = img;
		pi.originX = ox;
		pi.originY = oy;
		pi.deltaX = dx;
		pi.deltaY = -dy;
		pi.calculateBounds();
		pi.projection = dest;
		pi.setUsefulRange(new GeoRange(pi.projection, minX, maxX, minY, maxY));
		return pi;
	}
	
	public BufferedImage getImage()
	{
		return image;
	}
	
	public double getOriginX()
	{
		return originX;
	}
	
	public double getOriginY()
	{
		return originY;
	}
	
	public double getDeltaX()
	{
		return deltaX;
	}
	
	public double getDeltaY()
	{
		return deltaY;
	}
	
	public double[] getBounds()
	{
		return new double[] { minX, maxX, minY, maxY };
	}
}

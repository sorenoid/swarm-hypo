package gov.usgs.proj;

import gov.usgs.proj.Projection;

import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

/**
 * 
 * $Log: not supported by cvs2svn $
 * Revision 1.1  2007/04/15 22:41:32  dcervelli
 * Initial commit.
 *
 * @author Dan Cervelli
 * @version $Id: UnprojectImage.java,v 1.2 2007-04-15 22:55:35 dcervelli Exp $
 */
public class UnprojectImage
{
	public enum Interpolation
	{
		NEAREST_NEIGHBOR,
		BILINEAR
	}
	
	private double deltaX;
	private double deltaY;
	private double originX;
	private double originY;
	
	private BufferedImage image;
	
	public UnprojectImage(BufferedImage img, double dx, double dy, double ox, double oy)
	{
		image = img;
		deltaX = dx;
		deltaY = dy;
		originX = ox;
		originY = oy;
	}
	
	public int getSourcePixel(double sx, double sy, Interpolation interp)
	{
		if (interp == Interpolation.NEAREST_NEIGHBOR)
		{
			int xo = (int)((sx - originX) / deltaX);
			int yo = (int)((sy - originY) / deltaY);
			if (xo >= image.getWidth() || yo >= image.getHeight() || xo < 0  || yo < 0)
				return 0;
			else
				return image.getRGB(xo, yo);
		}
		else if (interp == Interpolation.BILINEAR)
		{
			double xo = (sx - originX) / deltaX;
			double yo = (sy - originY) / deltaY;
			
			if (xo >= image.getWidth() - 1 || yo >= image.getHeight() - 1 || xo < 0  || yo < 0)
				return 0;
			
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
			
			return b | g << 8 | r << 16;
		}
		else
			return 0;
	}
	
	public BufferedImage getUnprojectedImage(Projection proj, int w, int h, double lon, double lat, double dlon, double dlat, Interpolation interp)
	{
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		for (int x = 0; x < w; x++)
			for (int y = 0; y < h; y++)
			{
				Point2D.Double pt = new Point2D.Double(lon + x * dlon, lat + y * dlat);
				Point2D.Double src = proj.forward(pt);
				img.setRGB(x, y, getSourcePixel(src.x, src.y, interp));
			}
		
		return img;
	}
}

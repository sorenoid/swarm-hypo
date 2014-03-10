package gov.usgs.proj;

import java.awt.geom.Point2D;

/**
 * An identity projection.
 * 
 * $Log: not supported by cvs2svn $
 * Revision 1.1  2005/08/26 17:30:00  uid879
 * Initial avosouth commit.
 *
 * @author Dan Cervelli
 */
public class Identity extends Projection
{
	public Point2D.Double forward(Point2D.Double lonLat)
	{
		return lonLat;
	}

	public Point2D.Double inverse(Point2D.Double xy)
	{
		return xy;
	}
	
	public double getScale(Point2D.Double latLon)
	{
		return 1.0;
	}
}

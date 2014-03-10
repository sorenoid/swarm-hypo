package gov.usgs.proj;

import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;

/**
 * 
 * $Log: not supported by cvs2svn $
 * Revision 1.1  2007/04/15 03:52:13  dcervelli
 * Initial commit.
 *
 * @author Dan Cervelli
 * @version $Id: LambertConformalConic.java,v 1.2 2007-04-24 21:28:11 dcervelli Exp $
 */
public class LambertConformalConic extends Projection
{
	private double phi1;
	private double phi2;
	private Point2D.Double origin;
	
	private double n;
	private double F;
	private double m1;
	private double rho0;
	
	public LambertConformalConic(double p1, double p2, Point2D.Double o)
	{
		name = "Lambert Conformal Conic";
		setParameters(p1, p2, o);
		setEllipsoid(Ellipsoid.ELLIPSOIDS[11]);
	}
	
	public void setParameters(double p1, double p2, Point2D.Double o)
	{
		phi1 = p1;
		phi2 = p2;
		origin = o;
		calculateConstants();
	}
	
	@Override
	public void setEllipsoid(Ellipsoid e)
	{
		ellipsoid = e;
		calculateConstants();
	}
	
	private double m(double ang)
	{
		double s = Math.sin(ang);
		return Math.cos(ang) / Math.sqrt((1 - ellipsoid.eccentricitySquared * s * s));
	}
	
	private double t(double ang)
	{
		double e = Math.sqrt(ellipsoid.eccentricitySquared);
		double top = Math.tan(Math.PI / 4 - ang / 2);
		double s = Math.sin(ang);
		double bottom = Math.pow((1 - e * s) / (1 + e * s), e / 2);
		return top / bottom;
	}
	
	private void calculateConstants()
	{
		double phi0 = origin.y * DEG2RAD;
		double sp1 = phi1 * DEG2RAD;
		double sp2 = phi2 * DEG2RAD;
		m1 = m(sp1);
		double m2 = m(sp2);
		double t1 = t(sp1);
		double t2 = t(sp2);
		double t0 = t(phi0);
		n = Math.log(m1 / m2) / Math.log(t1 / t2);
		F = m1 / (n * Math.pow(t1, n));
		double a = ellipsoid.equatorialRadius;
		rho0 = a * F * Math.pow(t0, n);
	}
	
	@Override
	public Double forward(Double lonLat)
	{
		double lambda = lonLat.x * DEG2RAD;
		double phi = lonLat.y * DEG2RAD;
		double lambda0 = origin.x * DEG2RAD;
		
		double t = t(phi);
		double a = ellipsoid.equatorialRadius;
		double rho = a * F * Math.pow(t, n);
		double theta = n * (lambda - lambda0);
		double x = rho * Math.sin(theta);
		double y = rho0 - rho * Math.cos(theta);
		
 		return new Point2D.Double(x, y);
	}

	@Override
	public double getScale(Double lonLat)
	{
		double phi = lonLat.y * DEG2RAD;
		double t = t(phi);
		double m = m(phi);
		double tn = Math.pow(t, n);
		double k = (m1 * tn) / (m * tn); 
		return k;
	}

	@Override
	public Double inverse(Double xy)
	{
		double theta;
		if (n < 0)
			theta = Math.atan(-xy.x / (-rho0 + xy.y));
		else 
			theta = Math.atan(xy.x / (rho0 - xy.y));
			
		double rho = Math.sqrt(xy.x * xy.x + (rho0 - xy.y) * (rho0 - xy.y));
		if (n < 0)
			rho = -rho;
		
		double a = ellipsoid.equatorialRadius;
		double t = Math.pow(rho / (a * F), 1 / n);
		double lambda = (theta * RAD2DEG) / n + origin.x;
		double phi = Math.PI / 2 - 2 * Math.atan(t);
		double e = Math.sqrt(ellipsoid.eccentricitySquared);
		for (int i = 0; i < 5; i++)
		{
			double esp = e * Math.sin(phi);
			double b = Math.pow((1 - esp) / (1 + esp), e / 2);
			phi = Math.PI / 2 - 2 * Math.atan(t * b);
		}
		
		return new Point2D.Double(lambda, phi * RAD2DEG);
	}

	public String toString()
	{
		return String.format("%s\nStandard Parallel 1: %f\nStandard Parallel 2: %f\nOrigin: %s", name, phi1, phi2, origin.toString());
	}
	
	public static void main(String[] args)
	{
		Point2D.Double pt = new Point2D.Double(-75, 35);
		LambertConformalConic lcc = new LambertConformalConic(33, 45, new Point2D.Double(-96, 23));
		Point2D.Double xy = lcc.forward(pt);
		System.out.println(xy);
		Point2D.Double inv = lcc.inverse(xy);
		System.out.println(inv);
	}
}

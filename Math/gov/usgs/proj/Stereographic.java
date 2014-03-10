package gov.usgs.proj;

import java.awt.geom.Point2D;

/**
 * $Log: not supported by cvs2svn $
 * @author Dan Cervelli
 */
public class Stereographic extends Projection
{
	private Point2D.Double origin;
	
	public void setOrigin(Point2D.Double pt)
	{
		origin = pt;
	}
	
	public Point2D.Double forward(Point2D.Double lonLat)
	{
		double r = Ellipsoid.ELLIPSOIDS[0].equatorialRadius;
		double k0 = 1.0;
		double phiO = origin.getY() * DEG2RAD;
		double lambdaO = origin.getX() * DEG2RAD;
		double phi = lonLat.getY() * DEG2RAD;
		double lambda = lonLat.getX() * DEG2RAD;
		double k = 2 * k0 / (1 + Math.sin(phiO) * Math.sin(phi) + Math.cos(phiO) * Math.cos(phi) * Math.cos(lambda - lambdaO));
		double x = r * k * Math.cos(phi) * Math.sin(lambda - lambdaO);
		double y = r * k * (Math.cos(phiO) * Math.sin(phi) - Math.sin(phiO) * Math.cos(phi) * Math.cos(lambda - lambdaO));
		return new Point2D.Double(x, y);
	}
	
	public Point2D.Double inverse(Point2D.Double xy)
	{
		double r = Ellipsoid.ELLIPSOIDS[0].equatorialRadius;
		double k0 = 1;
		double x = xy.x;
		double y = xy.y;
		double rho = Math.sqrt(x * x + y * y);
		if (Math.abs(rho) < 0.00001)
			return origin;
		double c = 2 * Math.atan2(rho, (2 * r * k0));
//		double c = 2 * Math.atan2((2 * r * k0), rho);
		double phiO = origin.getY() * DEG2RAD;
		double lambdaO = origin.getX() * DEG2RAD;
		double phi = Math.asin(Math.cos(c) * Math.sin(phiO) + (y * Math.sin(c) * Math.cos(phiO) / rho));
//		double lambda = lambdaO + Math.atan(x * Math.sin(c) / (rho * Math.cos(phiO) * Math.cos(c) - y * Math.sin(phiO) * Math.sin(c)));
		double phiOd = origin.getY();
		double lambda = 0;
		if (phiOd == 90)
			lambda = lambdaO + Math.atan2(x, -y);
		else if (phiOd == -90)
			lambda = lambdaO + Math.atan2(x, y);
		else
			lambda = lambdaO + Math.atan2(x * Math.sin(c), (rho * Math.cos(phiO) * Math.cos(c) - y * Math.sin(phiO) * Math.sin(c)));
		return new Point2D.Double(lambda * RAD2DEG, phi * RAD2DEG);
	}
	
	public double getScale(Point2D.Double lonLat)
	{
		return 1;
	}

	public static void main(String[] args)
	{
		Point2D.Double o = new Point2D.Double(-100, 40);
		Point2D.Double p = new Point2D.Double(-75, 30);
		
		Stereographic sg = new Stereographic();
		sg.origin = o;
		Point2D.Double p2 = sg.forward(p);
		System.out.println(p2);
		System.out.println(sg.inverse(p2));
	}
}

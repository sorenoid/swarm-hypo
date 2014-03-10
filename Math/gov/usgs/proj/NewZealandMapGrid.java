package gov.usgs.proj;

import gov.usgs.math.Complex;

import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;

/**
 * Projection as described in this document:
 * 
 * http://www.linz.govt.nz/docs/miscellaneous/nzmg.pdf
 * 
 * $Log: not supported by cvs2svn $
 * Revision 1.1  2007/03/18 23:28:14  dcervelli
 * Initial commit.
 *
 * @author Dan Cervelli
 * @version $Id: NewZealandMapGrid.java,v 1.2 2007-04-24 21:34:33 dcervelli Exp $
 */
public class NewZealandMapGrid extends Projection
{
	protected static double[] A = new double[] {
			 0.6399175073,
			-0.1358797613,
			 0.063294409,
			-0.02526853,
			 0.0117879,
			-0.0055161,
			 0.0026906,
			-0.001333,
			 0.00067,
			-0.00034
		};
	
	protected static Complex[] B = new Complex[] {
			new Complex(0.7557853228, 0.0),
			new Complex(0.249204646, 0.003371507),
			new Complex(-0.001541739, 0.041058560),
			new Complex(-0.10162907, 0.01727609),
			new Complex(-0.26623489, -0.36249218),
			new Complex(-0.6870983, -1.1651967)
		};
	
	protected static Complex[] C = new Complex[] {
			new Complex(1.3231270439, 0.0),
			new Complex(-0.577245789, -0.007809598),
			new Complex(0.508307513, -0.112208952),
			new Complex(-0.15094762, 0.18200602),
			new Complex(1.01418179, 1.64497696),
			new Complex(1.9660549, 2.5127645)
		};
	
	protected static double[] D = new double[] {
			1.5627014243,
			0.5185406398,
			-0.03333098,
			-0.1052906,
			-0.0368594,
			0.007317,
			0.01220,
			0.00394,
			-0.0013
		};
	
	protected static double a = 6378388;
	protected static double E_0 = 2510000;
	protected static double N_0 = 6023150;
	protected static double PHI_O = -41.0;
	protected static double LAMBDA_0 = 173.0;
	
	public NewZealandMapGrid()
	{
		name = "New Zealand Map Grid";
	}
	
	@Override
	public Point2D.Double forward(Double lonLat)
	{
		double phi = lonLat.y;
		double lambda = lonLat.x;
		double deltaPhi = (phi - PHI_O) * 3600.0 * 1e-5;
		double deltaPsi = 0;
		for (int i = 1; i <= 10; i++)
		{
			deltaPsi += A[i - 1] * Math.pow(deltaPhi, i);
		}
		double deltaLambda = (lambda - LAMBDA_0) * Math.PI / 180.0;
		
		Complex theta = new Complex(deltaPsi, deltaLambda);
		Complex z = new Complex();
		for (int i = 1; i <= 6; i++)
		{
			Complex p = B[i - 1].mult(theta.pow(i));
			z = z.plus(p);
		}
		
		double east = (z.im * a) + E_0;
		double north = (z.re * a) + N_0;
		return new Point2D.Double(east, north);
	}

	@Override
	public Point2D.Double inverse(Double xy)
	{
		double north = xy.y;
		double east = xy.x;
		Complex z = new Complex((north - N_0) / a, (east - E_0) / a);
		
		Complex theta0 = new Complex();
		for (int i = 1; i <= 6; i++)
		{
			Complex p = C[i - 1].mult(z.pow(i)); 
			theta0 = theta0.plus(p);
		}
		Complex top = new Complex(z);
		Complex bottom = new Complex();
		for (int i = 1; i <= 6; i++)
		{
			if (i != 1)
			{
				Complex p = B[i - 1].mult(theta0.pow(i)).mult(i - 1);
				top = top.plus(p);
			}
			Complex p = B[i - 1].mult(theta0.pow(i - 1)).mult(i);
			bottom = bottom.plus(p);
		}
		Complex theta1 = top.divide(bottom);
		double deltaPhi = 0;
		for (int i = 1; i <= 9; i++)
		{
			deltaPhi += D[i - 1] * Math.pow(theta1.re, i);
		}
		double phi = PHI_O + (deltaPhi * 1e5/3600.0);
		double lambda = LAMBDA_0 + (theta1.im * 180 / Math.PI);
		return new Point2D.Double(lambda, phi);
	}
	
	@Override
	public double getScale(Double lonLat)
	{
		return 1.0;  // not correct, no information provided for how to calculate this correctly.
	}
	
	public static void main(String[] args)
	{
		NewZealandMapGrid nzmg = new NewZealandMapGrid();
		Point2D.Double ll1 = new Point2D.Double(172.739194, -34.444066);
		Point2D.Double mg1 = nzmg.forward(ll1);
		System.out.println(mg1);
		Point2D.Double ll1p = nzmg.inverse(mg1);
		System.out.println(ll1p + "\n");
		
		Point2D.Double ll2 = new Point2D.Double(172.723106, -40.512409);
		Point2D.Double mg2 = nzmg.forward(ll2);
		System.out.println(mg2);
		Point2D.Double ll2p = nzmg.inverse(mg2);
		System.out.println(ll2p + "\n");
		
		Point2D.Double ll3 = new Point2D.Double(169.172062, -46.651295);
		Point2D.Double mg3 = nzmg.forward(ll3);
		System.out.println(mg3);
		Point2D.Double ll3p = nzmg.inverse(mg3);
		System.out.println(ll3p + "\n");
	}
}

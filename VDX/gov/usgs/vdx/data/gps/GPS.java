package gov.usgs.vdx.data.gps;

import cern.colt.matrix.*;
import cern.colt.matrix.linalg.*;

/**
 * This class represents a set of static utility functions for working with 
 * GPS data.  This class makes extensive use of the CERN Colt linear algebra
 * library.
 *
 * @author Dan Cervelli
 */
public class GPS
{
    private static final DoubleFactory2D DENSE = DoubleFactory2D.dense;
    private static final DoubleFactory2D SPARSE = DoubleFactory2D.sparse;
	
	/** The constructor is private because the class is composed entirely
	 * of static methods and not meant to be instantiated.
	 */
    private GPS() {}
    
	/** Creates time, position, and covariance matrices out of a 2-D double 
	 * array composed of t, x, y, z, sx, sy, sz.  The time matrix returned
	 * is simply a Nx1 matrix of times.  The position matrix is a (3*N)x1 
	 * composed of x, y, z triples.  The covariance matrix is a (3*N)x(3*N) 
	 * spare array with the main diagnol populated.
	 * @param data the input data
	 * @return the time, position, and covariance matrices
	 */
    public static DoubleMatrix2D[] arrayToMatrix(double[][] data)
    {
        if (data == null)
            return null;
        
        DoubleMatrix2D t = DENSE.make(data.length, 1);
        for (int i = 0; i < data.length; i++)
            t.setQuick(i, 0, data[i][0]);
        
        DoubleMatrix2D xyz = DENSE.make(3 * data.length, 1);
        for (int i = 0; i < data.length; i++)
        {
            xyz.setQuick(i * 3, 0, data[i][1]);
            xyz.setQuick(i * 3 + 1, 0, data[i][2]);
            xyz.setQuick(i * 3 + 2, 0, data[i][3]);
        }
        
        DoubleMatrix2D cov = null;
        if (data[0].length > 4)
        {
            cov = SPARSE.make(3 * data.length, 3 * data.length);
            for (int i = 0; i < data.length; i++)
            {
                cov.setQuick(i * 3, i * 3, data[i][4] * data[i][4]);
                cov.setQuick(i * 3 + 1, i * 3 + 1, data[i][5] * data[i][5]);
                cov.setQuick(i * 3 + 2, i * 3 + 2, data[i][6] * data[i][6]);
            }
        }
        
        return new DoubleMatrix2D[] {t, xyz, cov};
    }
    
	/** Shortcut for xyz2LLH(xyz[0], xyz[1], xyz[2]).
	 * @param xyz XYZ data
	 * @return LLH data
	 */
    public static double[] xyz2LLH(double[] xyz)
    {
        return xyz2LLH(xyz[0], xyz[1], xyz[2]);
    }
    
	/** Converts XYZ coordinates into longitude/latitude/height coordinates.
	 * The ellipsoid parameters are hard-coded.  LLH is returned as a double 
	 * array.
	 * @param x x-coordinate
	 * @param y y-coordinate
	 * @param z z-coordinate
	 * @return LLH data
	 */
    public static double[] xyz2LLH(double x, double y, double z)
    {
        double[] llh = new double[3];
        double da = 0.0; // datum parameter
        double df = 0.0; // datum parameter
        double a = 6378137 - da;
        double f = 1 / 298.2572235630 - df;
        double b = (1 - f) * a;
        double e2 = 2 * f - f * f;
        double E2 = (a * a - b * b) / (b * b);
        double p = Math.sqrt(x * x + y * y);
        llh[0] = Math.atan2(y, x);
        double theta = Math.atan((z * a) / (p * b));
        llh[1] = Math.atan((z + E2 * b * Math.pow(Math.sin(theta), 3)) / (p - e2 * a * Math.pow(Math.cos(theta), 3)));
        double N = a / Math.sqrt(1 - e2 * Math.sin(llh[1]) * Math.sin(llh[1]));
        llh[2] = p / Math.cos(llh[1]) - N;
        llh[0] = Math.toDegrees(llh[0]);
        llh[1] = Math.toDegrees(llh[1]);
        return llh;
    }
    
	/** Converts longitude/latitude/height coordinate to XYZ coordinate.
	 * Ellipsoid parameters are hard-coded.  XYZ coordinates are returned as 
	 * a double array.
	 * @param lon longitude
	 * @param lat latitude
	 * @param h height
	 * @return XYZ data
	 */
    public static double[] llh2XYZ(double lon, double lat, double h)
    {
        double da = 0.0; // datum parameter
        double df = 0.0; // datum parameter
        double a = 6378137 - da;
        double f = 1 / 298.2572235630 - df;
        double b = (1 - f) * a;
        double phi = Math.toRadians(lat);
        double lam = Math.toRadians(lon);
        double N = (a * a) / Math.sqrt(a * a * Math.cos(phi) * Math.cos(phi) + b * b * Math.sin(phi) * Math.sin(phi));
        double[] xyz = new double[3];
        xyz[0] = (N + h) * Math.cos(phi) * Math.cos(lam);
        xyz[1] = (N + h) * Math.cos(phi) * Math.sin(lam);
        xyz[2] = (b * b * N / (a * a) + h) * Math.sin(phi);
        return xyz;
    }

	/** Creates a transformation matrix for translating from lon/lat/height
	 * to east/north/up based on a specified origin.
	 * @param lon origin longitude
	 * @param lat origin latitude
	 * @return 3x3 transformation matrix
	 */
    public static DoubleMatrix2D createENUTransform(double lon, double lat)
    {
        double sinLon = Math.sin(Math.toRadians(lon));
        double sinLat = Math.sin(Math.toRadians(lat));
        double cosLon = Math.cos(Math.toRadians(lon));
        double cosLat = Math.cos(Math.toRadians(lat));
        DoubleMatrix2D t = DoubleFactory2D.dense.make(new double[][] 
        {
            {          -sinLon,           cosLon,      0 },
            { -sinLat * cosLon, -sinLat * sinLon, cosLat },
            {  cosLat * cosLon,  cosLat * sinLon, sinLat }
        });
        return t;
    }
 
	/** Creates a sparse transformation matrix (see createENUTransform) that 
	 * can be multiplied by a position matrix of length 3*n.
	 * @param lon origin longitude
	 * @param lat origin latitude
	 * @param n the number of coordinates
	 * @return the transformation matrix
	 */
	public static DoubleMatrix2D createFullENUTransform(double lon, double lat, int n)
    {
        DoubleMatrix2D t = createENUTransform(lon, lat);
        DoubleMatrix2D[][] parts = new DoubleMatrix2D[n][n];
        for (int i = 0; i < n; i++)
            parts[i][i] = t;
        DoubleMatrix2D fullT = DoubleFactory2D.sparse.compose(parts);
        return fullT;
    }
    
	/** Rearranges columnar 3*Nx1 matrix to an Nx3 row matrix.
	 * @param rm source data
	 * @return row oriented data
	 */
    public static DoubleMatrix2D column3NToRows(DoubleMatrix2D rm)
    {
        DoubleMatrix2D r = DENSE.make(rm.rows() / 3, 3);
        for (int i = 0; i < rm.rows() / 3; i++)
        {
            r.setQuick(i, 0, rm.getQuick(i * 3, 0));
            r.setQuick(i, 1, rm.getQuick(i * 3 + 1, 0));
            r.setQuick(i, 2, rm.getQuick(i * 3 + 2, 0));
        }
        return r;
    }
    
	/** Solves weighted least squares.
	 * @param g model parameters
	 * @param d data
	 * @param sd sigmas
	 * @return least squares solution
	 */
    public static DoubleMatrix2D solveWeightedLeastSquares(DoubleMatrix2D g, DoubleMatrix2D d, DoubleMatrix2D sd)
    {
        DoubleMatrix2D sdi = Algebra.DEFAULT.inverse(sd);
        DoubleMatrix2D m1 = Algebra.DEFAULT.inverse(Algebra.DEFAULT.mult(Algebra.DEFAULT.mult(g.viewDice(), sdi), g));
        m1 = Algebra.DEFAULT.mult(Algebra.DEFAULT.mult(Algebra.DEFAULT.mult(m1, g.viewDice()), sdi), d);
        return m1;
    }
    
	/** Gets error parameters.
	 * @param g model parameters
	 * @param sd sigmas
	 * @return residual
	 */
    public static DoubleMatrix2D getErrorParameters(DoubleMatrix2D g, DoubleMatrix2D sd)
    {
		DoubleMatrix2D sdi = Algebra.DEFAULT.inverse(sd);
		return Algebra.DEFAULT.inverse(Algebra.DEFAULT.mult(Algebra.DEFAULT.mult(g.viewDice(), sdi), g));
    }
}
package gov.usgs.math;

/**
 * A class for calculating the discrete Fourier transform (DFT).  This is very 
 * slow.
 *
 * $Log: not supported by cvs2svn $
 * @author Dan Cervelli
 */
public class DFT
{
    private static final double TWOPI = 2 * Math.PI;
    private double[] data;
    private int numData;
    
    /** Constructs a new DFT transformer.
	 * @param d the data to transform
	 */
    public DFT(double[] d)
    {
        data = d;
        numData = data.length;
    }
    
	/** Gets the DFT of a point in the data.
	 * @param m the data point index
	 * @return the DFT at that point
	 */
    public Complex getDFTPoint(int m)
    {
        Complex cx = new Complex();

        if (m >= 0 && m < numData)
        {
            double r = 0.0;
            double i = 0.0;

            // At m == 0 the DFT reduces to the sum of the data
            if (m == 0)
            {
                for (int n = 0; n < numData; n++)
                    r = r + data[n];
            }
            else
            {
                double x;
                double scale;

                for (int n = 0; n < numData; n++) 
                {
                    x = data[n];
                    scale = (TWOPI * n * m) / numData;
                    r = r + x * Math.cos(scale);
                    i = i - x * Math.sin(scale);
                } 
            } 
            cx.re = r;
            cx.im = i;
        }
        return cx;
    }
    
	/** Gets the full DFT (of the first half of the points).
	 * @return the DFT as a complex array
	 */
    public Complex[] getFullDFT()
    {
        Complex[] dft = new Complex[numData / 2];
        for (int i = 0; i < numData / 2; i++)
            dft[i] = getDFTPoint(i);
            
        return dft;
    }
    
}

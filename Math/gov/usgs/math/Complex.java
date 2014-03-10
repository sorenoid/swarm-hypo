package gov.usgs.math;

/**
 * A class for complex numbers.  All methods of this class return a new complex
 * number with the result of the operation without altering the original value
 * of the complex.  The only way to change the actual values of the complex 
 * number are through the public variables re and im.
 *
 *<p>Most of this code was translated from the files complex.C and complex.h by:<br>
 *<br>
 *   A.J. Fisher, University of York, fisher@minster.york.ac.uk<br>
 *   September 1992<br>
 * as part of his software for generating digital IIR filters.
 *
 * $Log: not supported by cvs2svn $
 * Revision 1.1  2005/08/26 17:30:00  uid879
 * Initial avosouth commit.
 *
 * @author Dan Cervelli
 */
public class Complex
{
	/** The real part of this complex number.
	 */
    public double re;
	/** The imaginary part of this complex number.
	 */
    public double im;

	/** Constructs a 0,0 complex number.
	 */
    public Complex()
    {
        re = 0;
        im = 0;
    }
    
	/** Contructs a complex number from arguments.
	 * @param r the real part
	 * @param i the imaginary part
	 */
    public Complex(double r, double i)
    {
        re = r;
        im = i;
    }
	
	/** Contructs a complex number from another.
	 * @param c the other complex number
	 */
	public Complex(Complex c)
	{
		re = c.re;
		im = c.im;
	}
    
	/** Gets the magnitude of this complex number.
	 * @return the magnitude
	 */
    public double mag()
    {
        return Math.sqrt(re * re + im * im);
    }
	
	/** Returns the conjugate of this complex.
	 * @return the conjugate
	 */
	public Complex conj()
	{
		return new Complex(re, -im);
	}
	
	/** Multiplies this complex by a scalar.
	 * @param d the scalar
	 * @return the result
	 */
	public Complex mult(double d)
	{
		return new Complex(re * d, im * d);
	}
	
	/** Multiplies this complex by another.
	 * @param c the other complex
	 * @return the result
	 */
	public Complex mult(Complex c)
	{
		return new Complex(re * c.re - im * c.im,
				re * c.im + im * c.re);
	}
	
	/** Divides this complex by a scalar.
	 * @param d the scalar
	 * @return the result
	 */
	public Complex divide(double d)
	{
		return new Complex(re / d, im / d);
	}
	
	/** Divides this complex by another complex.
	 * @param c the other
	 * @return the result
	 */
	public Complex divide(Complex c)
	{
		double mag = c.re * c.re + c.im * c.im;
		return new Complex(
				(re * c.re + im * c.im) / mag,
				(im * c.re - re * c.im) / mag);
	}
	
	/** Adds this complex to another complex.
	 * @param c the other
	 * @return the result
	 */
	public Complex plus(Complex c)
	{
		return new Complex(re + c.re, im + c.im);
	}
	
	/** Substracts another complex from this complex.
	 * @param c the other 
	 * @return the result
	 */
	public Complex minus(Complex c)
	{
		return new Complex(re - c.re, im - c.im);
	}
	
	/** Negates this complex.
	 * @return the negation
	 */
	public Complex neg()
	{
		return new Complex(-re, -im);
	}
	
	/** Tests for equality.
	 * @param c the other complex
	 * @return whether or not these complexes are equal
	 */
	public boolean equals(Complex c)
	{
		return (c.re == re && c.im == im);
	}
	
	/** Gets the hypotenuse of this complex.  Synonym for mag()
	 * @return the hypotenuse
	 */
	public double hypot()
	{
		return Math.sqrt(im * im + re * re);
	}
	
	/** Gets the arctangent of this complex.
	 * @return the arctangent
	 */
	public double atan2()
	{
		return Math.atan2(im, re);
	}
	
	/** Gets the complex square root.
	 * @return the square root
	 */
	public Complex sqrt()
	{
		double r = hypot();
		Complex z = new Complex(Math.sqrt(0.5 * (r + re)), 
				Math.sqrt(0.5 * (r - re)));
		if (im < 0)
			z.im = -z.im;
		return z;
	}
	
	/** Gets the exponential of this complex.
	 * @return the exponential
	 */
	public Complex exp()
	{
		return expj(im).mult(Math.exp(re));
	}
	
	/** Gets a new complex of value [cos(theta), sin(theta)].
	 * @param theta the angle
	 * @return the result
	 */
	public static Complex expj(double theta)
	{
		return new Complex(Math.cos(theta), Math.sin(theta));
	}
	
	public static Complex expk(double theta, int k)
	{
		return new Complex(Math.cos(k * theta), Math.sin(k * theta));
	}
	
	/** Gets the square of this complex.
	 * @return the result
	 */
	public Complex sqr()
	{
		return this.mult(this);
	}

	public Complex pow(int k)
	{
		double rk = Math.pow(mag(), k);
		double t = atan2();
		return new Complex(rk * Math.cos(k * t), rk * Math.sin(k * t));
	}
	
	/** Evaluates a complex polynomial.
	 * @param coeffs the coefficents
	 * @param n the number of coefficients
	 * @param z the complex
	 * @return the result
	 */
	public static Complex eval(Complex[] coeffs, int n, Complex z)
	{
		Complex sum = new Complex();
		for (int i = n; i >= 0; i--)
			sum = (sum.mult(z)).plus(coeffs[i]);
		return sum;
	}
	
	/** Does eval(top) / eval(bottom)
	 * @param topco the top coefficents
	 * @param nt the number of top coefficients
	 * @param botco the bottom coefficents
	 * @param nb the number of bottom coefficients
	 * @param z the complex
	 * @return the result
	 */
	public static Complex evaluate(Complex[] topco, int nt, Complex[] botco, int nb, Complex z)
	{
		Complex c1 = eval(topco, nt, z);
		Complex c2 = eval(botco, nb, z);
		return c1.divide(c2);
	}
	
	/** Performs the bilinear transformation.
	 * @return the result
	 */
	public Complex blt()
	{
		Complex c1 = new Complex(2 + re, im);
		Complex c2 = new Complex(2 - re, -im);
		return c1.divide(c2);
	}
	
	/** Gets a string representation of this complex.
	 * @return the string representation
	 */
	public String toString()
	{
		return "re=" + re + ", im=" + im;
	}
	
}

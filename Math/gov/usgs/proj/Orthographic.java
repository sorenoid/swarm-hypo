package gov.usgs.proj;

import gov.usgs.util.CodeTimer;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

/**
 * 
 * @author Dan Cervelli
 */
public class Orthographic extends Projection {
	private Point2D.Double origin;

	public void setOrigin(Point2D.Double pt) {
		origin = pt;
	}

	public Point2D.Double forward(Point2D.Double lonLat) {
		double r = Ellipsoid.ELLIPSOIDS[0].equatorialRadius;
		double phiO = origin.getY() * DEG2RAD;
		double lambdaO = origin.getX() * DEG2RAD;
		double phi = lonLat.getY() * DEG2RAD;
		double lambda = lonLat.getX() * DEG2RAD;

		double x = r * Math.cos(phi) * Math.sin(lambda - lambdaO);
		double y = r
				* (Math.cos(phiO) * Math.sin(phi) - Math.sin(phiO)
						* Math.cos(phi) * Math.cos(lambda - lambdaO));

		return new Point2D.Double(x, y);
	}

	public Point2D.Double inverse(Point2D.Double xy) {
		double r = Ellipsoid.ELLIPSOIDS[0].equatorialRadius;
		double x = xy.x;
		double y = xy.y;
		double rho = Math.sqrt(x * x + y * y);
		double c = Math.asin(rho / r);
		if (Math.abs(rho) < 0.00001)
			return origin;

		double phiO = origin.getY() * DEG2RAD;
		double lambdaO = origin.getX() * DEG2RAD;

		double phi = Math.asin(Math.cos(c) * Math.sin(phiO)
				+ (y * Math.sin(c) * Math.cos(phiO) / rho));
		double lambda = lambdaO
				+ Math.atan2(
						x * Math.sin(c),
						(rho * Math.cos(phiO) * Math.cos(c) - y
								* Math.sin(phiO) * Math.sin(c)));
		return new Point2D.Double(lambda * RAD2DEG, phi * RAD2DEG);
	}

	public double getScale(Point2D.Double lonLat) {
		return 1;
	}

	public double[] getProjectedExtents(GeoRange gr) {
		return new double[] { -7000000, 7000000, -7000000, 7000000 };
	}

	public FastProjector getFastProjector() {
		return new FastProjector() {
			double r = Ellipsoid.ELLIPSOIDS[0].equatorialRadius;
			double phiO = origin.getY() * DEG2RAD;
			double lambdaO = origin.getX() * DEG2RAD;
			double spO = Math.sin(phiO);
			double cpO = Math.cos(phiO);
			double sc, cc;
			double x, y, rho, c;

			public void forward(Point2D.Double pt) {
				pt = Orthographic.this.forward(pt);
			}

			public void inverse(Point2D.Double xy) {
				x = xy.x;
				y = xy.y;
				rho = Math.sqrt(x * x + y * y);
				if (rho < 0.00001) {
					xy.x = origin.x;
					xy.y = origin.y;
					return;
				}
				if (rho > r) {
					xy.x = Double.NaN;
					return;
				}
				c = Math.asin(rho / r);
				cc = Math.cos(c);
				sc = Math.sin(c);

				xy.x = (lambdaO + Math.atan2(x * sc, (rho * cpO * cc - y * spO
						* sc)))
						* RAD2DEG;
				xy.y = Math.asin(cc * spO + (y * sc * cpO / rho)) * RAD2DEG;
			}
		};
	}

	public static void main(String[] args) throws Exception {
		Orthographic og = new Orthographic();
		GeoRange gr = new GeoRange(-180, 180, -90, 90);

		final int m = 21;
		final Image[] imgs = new Image[m];
		CodeTimer ctl = new CodeTimer("load");
		BufferedImage bi = ImageIO.read(new FileInputStream(
				"c:\\mapdata\\nasa\\world.jpg"));
		int[] pix = bi.getRGB(0, 0, bi.getWidth(), bi.getHeight(), null, 0,
				bi.getWidth());
		ctl.mark("decode");
		for (int i = 0; i < pix.length; i++) {
			pix[i] = 0xff000000 | pix[i];
		}
		ctl.mark("toint");
		ctl.stopAndReport();
		System.out.println("pix.length: " + pix.length);

		for (int k = 0; k < m; k++) {
			System.out.println(k);
			og.setOrigin(new Point2D.Double(0, 0));
			og.setOrigin(new Point2D.Double(10, -90
					+ ((double) k / (double) m * 360) + 3));
			imgs[k] = og.getProjectedImage(50, 1000, 1000, pix, 2700, 1350, gr,
					-10000000, 10000000, -10000000, 10000000);
		}

		JFrame f = new JFrame("GeoImageSet Test, Projected") {
			public static final long serialVersionUID = -1;
			int cycle = 0;

			public void paint(Graphics g) {
				Graphics2D g2 = (Graphics2D) g;
				g2.drawImage(imgs[++cycle % m], 0, 0, null);
			}
		};

		f.setSize(1200, 1000);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setVisible(true);

		while (true) {
			f.repaint();
			try {
				Thread.sleep(50);
			} catch (Exception e) {
			}
		}
	}
}

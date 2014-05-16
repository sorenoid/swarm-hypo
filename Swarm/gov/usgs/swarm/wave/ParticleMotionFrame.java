package gov.usgs.swarm.wave;

import gov.usgs.util.Pair;

import java.awt.GridLayout;
import java.util.ArrayList;

import javax.swing.JFrame;


/**
 * A window that holds the three particle motion plots
 * 
 * @author Jamil Shehzad
 */
@SuppressWarnings("serial")
public class ParticleMotionFrame extends JFrame {

    private ParticleMotionViewPanel component1;
    private ParticleMotionViewPanel component2;
    private ParticleMotionViewPanel component3;

    /**
     * @param compLabels
     *            labels ordered N, E, Z
     * @param compData
     *            wave data ordered N, E, Z
     */
    public ParticleMotionFrame(ArrayList<WaveViewPanel> views, String[] compLabels, double[][] compData) {
        super();
        String labelN = compLabels[0];
        String labelE = compLabels[1];
        String labelZ = compLabels[2];

        double[] dataN = compData[0];
        double[] dataE = compData[1];
        double[] dataZ = compData[2];

        Pair<Double, Double> extentN = extent(dataN);
        Pair<Double, Double> extentE = extent(dataE);
        Pair<Double, Double> extentZ = extent(dataZ);

        // Plots should always have Z on y-axis if Z is involved and
        // Should have N on y-axis when plotting N vs E
        component1 = new ParticleMotionViewPanel(labelE, dataE, labelN, dataN, extent(extentE, extentN));
        component2 = new ParticleMotionViewPanel(labelN, dataN, labelZ, dataZ, extent(extentN, extentZ));
        component3 = new ParticleMotionViewPanel(labelE, dataE, labelZ, dataZ, extent(extentE, extentZ));

        this.setTitle("Particle Motion Plot");
        GridLayout gr = new GridLayout(1, 3);
        gr.setHgap(2);
        gr.setHgap(2);
        this.getContentPane().setLayout(gr);
        this.add(component1);
        this.add(component2);
        this.add(component3);
        this.setSize(756, 306);
        this.setResizable(false);
    }

    public static Pair<Double, Double> extent(double[] dataN) {
        double min = dataN[0];
        double max = dataN[0];
        for (int i = 0; i < dataN.length; i++) {
            double d = dataN[i];
            if (d > max) {
                max = d;
            } else if (d < min) {
                min = d;
            }
        }
        return new Pair<Double, Double>(min, max);
    }

    public static Pair<Double, Double> extent(Pair<Double, Double> extent1, Pair<Double, Double> extent2) {
        return new Pair<Double, Double>(Math.min(extent1.item1, extent2.item1), Math.max(extent1.item2, extent2.item2));
    }
}

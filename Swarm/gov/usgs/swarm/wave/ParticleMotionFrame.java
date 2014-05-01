package gov.usgs.swarm.wave;

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

	public static double [] nData = new double[2]; //0-Min 1-Max
	public static double [] zData = new double[2]; //0-Min 1-Max
	public static double [] eData = new double[2]; //0-Min 1-Max

	public ParticleMotionFrame() {
		super();
		component1 = new ParticleMotionViewPanel();
		component2 = new ParticleMotionViewPanel();
		component3 = new ParticleMotionViewPanel();
		
		this.setTitle("Particle Motion Plot");
		GridLayout gr = new GridLayout(1,3);
		gr.setHgap(2);
		gr.setHgap(2);
		this.getContentPane().setLayout(gr);
		this.add(component1);
		this.add(component2);
		this.add(component3);
		this.setSize(756, 306);
		this.setResizable(false);
	}

	public ParticleMotionFrame(ArrayList<WaveViewPanel> views, double[] data1,
			double[] data2, double[] data3) {
		super();

		
		component1 = new ParticleMotionViewPanel();	
		
		boolean zWave = views.get(0).getChannel().getLastComponentCode().endsWith("Z");
		if(zWave){
			zData[0] = component1.getMin(data1);
			zData[1] = component1.getMax(data1);
			boolean nWave = views.get(0).getChannel().getLastComponentCode().endsWith("N");
			if(nWave){
				nData[0] = component1.getMin(data2);
				nData[1] = component1.getMax(data2);
			}else{
				eData[0] = component1.getMin(data2);
				eData[1] = component1.getMax(data2);
			}
		}else{
			nData[0] = component1.getMin(data1);
			nData[1] = component1.getMax(data1);
			eData[0] = component1.getMin(data2);
			eData[1] = component1.getMax(data2);
		}
		
		
		component2 = new ParticleMotionViewPanel();
		zWave = views.get(1).getChannel().getLastComponentCode().endsWith("Z");
		if(zWave){
			double min = component2.getMin(data2);
			double max = component2.getMax(data2);
			if(zData[0] > min){
				zData[0] = min;
			}
			if(zData[1] < max){
				zData[1] = max;
			}
			min = component2.getMin(data3);
			max = component2.getMax(data3);
			boolean nWave = views.get(1).getChannel().getLastComponentCode().endsWith("N");
			if(nWave){
				if(nData[0] > min){
					nData[0] = min;
				}
				if(nData[1] < max){
					nData[1] = max;
				}
			}else{
				if(eData[0] > min){
					eData[0] = min;
				}
				if(eData[1] < max){
					eData[1] = max;
				}
			}
		}else{
			double min = component2.getMin(data2);
			double max = component2.getMax(data2);
			if(nData[0] > min){
				nData[0] = min;
			}
			if(nData[1] < max){
				nData[1] = max;
			}
			min = component2.getMin(data3);
			max = component2.getMax(data3);
			if(eData[0] > min){
				eData[0] = min;
			}
			if(eData[1] < max){
				eData[1] = max;
			}
		}
		

		component3 = new ParticleMotionViewPanel();
		zWave = views.get(2).getChannel().getLastComponentCode().endsWith("Z");
		if(zWave){
			double min = component3.getMin(data3);
			double max = component3.getMax(data3);
			if(zData[0] > min){
				zData[0] = min;
			}
			if(zData[1] < max){
				zData[1] = max;
			}
			min = component3.getMin(data1);
			max = component3.getMax(data1);
			boolean nWave = views.get(2).getChannel().getLastComponentCode().endsWith("N");
			if(nWave){
				if(nData[0] > min){
					nData[0] = min;
				}
				if(nData[1] < max){
					nData[1] = max;
				}
			}else{
				if(eData[0] > min){
					eData[0] = min;
				}
				if(eData[1] < max){
					eData[1] = max;
				}
			}
		}else{
			double min = component3.getMin(data3);
			double max = component3.getMax(data3);
			if(nData[0] > min){
				nData[0] = min;
			}
			if(nData[1] < max){
				nData[1] = max;
			}
			min = component3.getMin(data1);
			max = component3.getMax(data1);
			if(eData[0] > min){
				eData[0] = min;
			}
			if(eData[1] < max){
				eData[1] = max;
			}
		}
		
		if(zData[0] < 0){
			double temp = (-1) * zData[0];
			if(temp > zData[1]){
				zData[1] = temp;
			}else{
				zData[0] = (-1) * zData[1];
			}
		}else{
			zData[0] = (-1) * zData[1];
		}
		
		if(nData[0] < 0){
			double temp = (-1) * nData[0];
			if(temp > nData[1]){
				nData[1] = temp;
			}else{
				nData[0] = (-1) * nData[1];
			}
		}
		
		if(eData[0] < 0){
			double temp = (-1) * eData[0];
			if(temp > eData[1]){
				eData[1] = temp;
			}else{
				eData[0] = (-1) * eData[1];
			}
		}
		
		this.setTitle("Particle Motion Plot");
		GridLayout gr = new GridLayout(1,3);
		gr.setHgap(2);
		gr.setHgap(2);
		this.getContentPane().setLayout(gr);
		this.add(component1);
		this.add(component2);
		this.add(component3);
		this.setSize(756, 306);
		this.setResizable(false);
	}

	public ParticleMotionViewPanel getComponent1() {
		return component1;
	}

	public ParticleMotionViewPanel getComponent2() {
		return component2;
	}

	public ParticleMotionViewPanel getComponent3() {
		return component3;
	}
	
}

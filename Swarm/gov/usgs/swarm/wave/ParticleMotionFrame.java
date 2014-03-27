package gov.usgs.swarm.wave;

import java.awt.GridLayout;

import javax.swing.JFrame;


/**
 * 
 * @author Olufemi Thompson
 *
 * A window that holds the three paritcle motion plots
 *
 */
@SuppressWarnings("serial")
public class ParticleMotionFrame extends JFrame {

	private ParticleMotionViewPanel component1;
	private ParticleMotionViewPanel component2;
	private ParticleMotionViewPanel component3;

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

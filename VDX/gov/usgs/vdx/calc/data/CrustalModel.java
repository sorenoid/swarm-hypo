package gov.usgs.vdx.calc.data;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Data structure used in Hypo71 algorithm.
 * 
 * @author Oleg Shepelev
 */
@XmlRootElement
public class CrustalModel {
	private float V;
	private float D;

	public CrustalModel() {
	}
	
	public CrustalModel(float v, float d) {
		V = v;
		D = d;
	}

	@XmlElement
	public float getV() {
		return V;
	}

	public void setV(float v) {
		V = v;
	}

	@XmlElement
	public float getD() {
		return D;
	}

	public void setD(float d) {
		D = d;
	}
}
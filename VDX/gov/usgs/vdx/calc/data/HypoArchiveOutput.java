package gov.usgs.vdx.calc.data;




import java.util.LinkedList;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class HypoArchiveOutput {

	private LinkedList<Station> stations = new LinkedList<Station>();
	private ControlCard controlCard;
	private LinkedList<PhaseRecord> phasRecords = new LinkedList<PhaseRecord>();
	private LinkedList<CrustalModel> crustalModel = new LinkedList<CrustalModel>();
	
	@XmlElements(value = { @XmlElement })
	public LinkedList<Station> getStations() {
		return stations;
	}
	public void setStations(LinkedList<Station> stations) {
		this.stations = stations;
	}
	
	@XmlElement
	public ControlCard getControlCard() {
		return controlCard;
	}
	public void setControlCard(ControlCard controlCard) {
		this.controlCard = controlCard;
	}
	
	@XmlElements(value = { @XmlElement })
	public LinkedList<PhaseRecord> getPhasRecords() {
		return phasRecords;
	}
	public void setPhasRecords(LinkedList<PhaseRecord> phasRecords) {
		this.phasRecords = phasRecords;
	}
	
	@XmlElements(value = { @XmlElement })
	public LinkedList<CrustalModel> getCrustalModel() {
		return crustalModel;
	}
	public void setCrustalModel(LinkedList<CrustalModel> crustalModel) {
		this.crustalModel = crustalModel;
	}
	
	
	

	
}

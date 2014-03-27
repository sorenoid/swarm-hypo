package gov.usgs.swarm.database.model;

import java.io.Serializable;

public class Hypo71 implements Serializable {

	private static final long serialVersionUID = -6841860280903924060L;
	private Integer hypoId;
	private String hypoOutput;
	private String stationLocation;
	private Double velocity;
	private Attempt attempt;

	public Hypo71() {
	}

	public Hypo71(String hypoOutput, String stationLocation, Double velocity) {
		super();
		this.hypoOutput = hypoOutput;
		this.stationLocation = stationLocation;
		this.velocity = velocity;
	}

	public Integer getHypoId() {
		return hypoId;
	}

	public void setHypoId(Integer hypoId) {
		this.hypoId = hypoId;
	}

	public String getHypoOutput() {
		return hypoOutput;
	}

	public void setHypoOutput(String hypoOutput) {
		this.hypoOutput = hypoOutput;
	}

	public String getStationLocation() {
		return stationLocation;
	}

	public void setStationLocation(String stationLocation) {
		this.stationLocation = stationLocation;
	}

	public Double getVelocity() {
		return velocity;
	}

	public void setVelocity(Double velocity) {
		this.velocity = velocity;
	}

	public Attempt getAttempt() {
		return attempt;
	}

	public void setAttempt(Attempt attempt) {
		this.attempt = attempt;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((hypoId == null) ? 0 : hypoId.hashCode());
		result = prime * result
				+ ((hypoOutput == null) ? 0 : hypoOutput.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Hypo71 other = (Hypo71) obj;
		if (hypoId == null) {
			if (other.hypoId != null)
				return false;
		} else if (!hypoId.equals(other.hypoId))
			return false;
		if (hypoOutput == null) {
			if (other.hypoOutput != null)
				return false;
		} else if (!hypoOutput.equals(other.hypoOutput))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Hypo71 [hypoId=" + hypoId + ", hypoOutput=" + hypoOutput
				+ ", stationLocation=" + stationLocation + ", velocity="
				+ velocity + "]";
	}
}
package gov.usgs.swarm.database.model;

import java.io.Serializable;

public class FileData implements Serializable {

	private static final long serialVersionUID = 5541527061465973308L;
	private Integer fileDataId;
	private String stationCode;
	private Double azimuth;
	private File file;

	public FileData(String stationCode, Double azimuth) {
		super();
		this.stationCode = stationCode;
		this.azimuth = azimuth;
	}

	public Integer getFileDataId() {
		return fileDataId;
	}

	public void setFileDataId(Integer fileDataId) {
		this.fileDataId = fileDataId;
	}

	public String getStationCode() {
		return stationCode;
	}

	public void setStationCode(String stationCode) {
		this.stationCode = stationCode;
	}

	public Double getAzimuth() {
		return azimuth;
	}

	public void setAzimuth(Double azimuth) {
		this.azimuth = azimuth;
	}

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((azimuth == null) ? 0 : azimuth.hashCode());
		result = prime * result
				+ ((fileDataId == null) ? 0 : fileDataId.hashCode());
		result = prime * result
				+ ((stationCode == null) ? 0 : stationCode.hashCode());
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
		FileData other = (FileData) obj;
		if (azimuth == null) {
			if (other.azimuth != null)
				return false;
		} else if (!azimuth.equals(other.azimuth))
			return false;
		if (fileDataId == null) {
			if (other.fileDataId != null)
				return false;
		} else if (!fileDataId.equals(other.fileDataId))
			return false;
		if (stationCode == null) {
			if (other.stationCode != null)
				return false;
		} else if (!stationCode.equals(other.stationCode))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "FileData [fileDataId=" + fileDataId + ", stationCode="
				+ stationCode + ", azimuth=" + azimuth + "]";
	}
}
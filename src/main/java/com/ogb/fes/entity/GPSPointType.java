package com.ogb.fes.entity;

public class GPSPointType extends GPSPoint {

	String type;
	
	public GPSPointType() {
		type = "";
	}

	public GPSPointType(double latitude, double longitude, String type) {
		super(latitude, longitude);
		this.type=type;
	}

	public GPSPointType(GPSPoint point, String type) {
		super(point.getLatitude(),point.getLongitude());
		this.type=type;
	}
	
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		GPSPointType other = (GPSPointType) obj;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "{lat=" + latitude + "; lon="+ longitude + "; type=" + type+"}";
	}
}

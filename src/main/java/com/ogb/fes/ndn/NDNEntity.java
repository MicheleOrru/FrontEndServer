package com.ogb.fes.ndn;


import java.util.ArrayList;

import com.ogb.fes.entity.GPSPoint;
import com.ogb.fes.utils.GeoJSON;
import com.ogb.fes.utils.Utils;


public class NDNEntity
{
	public enum COMMANDS
	{
		TILE,
		INSERT,
		DELETE
	}
	
	private GeoJSON geoJSON;
	
	private GPSPoint point;
	private String   userID;
	private String   tenantID;
	private String   nonce;
	private String	 collectionID;
	private String   reference;
	private COMMANDS command;
	
	
	private NDNEntity() {
		super();
	}

	public NDNEntity(GeoJSON geoJSONContainer, int pointIndex, String reference, COMMANDS command) {
		this();

	
		this.point    	  = geoJSONContainer.getCoordinates().get(pointIndex);
		this.userID   	  = geoJSONContainer.getUserID();
		this.tenantID	  = geoJSONContainer.getTenantID();
		this.nonce    	  = geoJSONContainer.getNonce();
		this.collectionID = geoJSONContainer.getCollectionID();
		
		this.setReference(reference);
		this.command  	  = command;
		this.setGeoJSON(geoJSONContainer);
	}

	
	public String getDataName(int zoomLevel) {
		
		String prefix = Utils.gpsPointToNDNNname(point, zoomLevel, Utils.Format.LONG_LAT);
		String res    = "";
		
		switch (command) {
			case TILE:
				String ndnListPrefix      = "/OGB" + prefix + "/GPS_id/TILE"; 
				String ndnDataRequestName = "/OGB" + prefix + "/GPS_id/DATA/" + tenantID + "/" + collectionID;
				res = ndnListPrefix + ndnDataRequestName;
				res = res + "/" + res.hashCode();
				break;
				
			case INSERT:
				res = "/OGB"+prefix+"/GPS_id/DATA/" + tenantID + "/" + collectionID + "/" + userID + "/" + nonce;
				break;
				
			case DELETE:
				res = "/OGB"+prefix+"/GPS_id/DELETE/OGB"+prefix+"/GPS_id/DATA/" + tenantID + "/" + collectionID + "/" + userID + "/" + nonce;
				break;
				
			default:
				break;
		}
		
		return res;
	}	
	
	
	public ArrayList<String> getDataNames()
	{
		ArrayList<String> result = new ArrayList<>();
		int ZOOM_LEVEL = 3;
		
		for (int i = 0; i< ZOOM_LEVEL; i++) {
			result.add(getDataName(i));
		}
		
		return result;
	}
	
	public String getGeoJSONName()
	{
		String prefix = Utils.gpsPointToNDNNname(point, 2, Utils.Format.LONG_LAT);
		String res    = "/OGB"+prefix+"/GPS_id/GEOJSON/"+tenantID+"/"+collectionID+"/"+userID+"/"+nonce;
		
		return res;
	}
	
	
	public COMMANDS getCommand() {
		return command;
	}

	public void setCommand(COMMANDS command) {
		this.command = command;
	}

	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((collectionID == null) ? 0 : collectionID.hashCode());
		result = prime * result + ((command == null) ? 0 : command.hashCode());
		result = prime * result + ((nonce == null) ? 0 : nonce.hashCode());
		result = prime * result + ((point == null) ? 0 : point.hashCode());
		result = prime * result + ((tenantID == null) ? 0 : tenantID.hashCode());
		result = prime * result + ((userID == null) ? 0 : userID.hashCode());
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
		NDNEntity other = (NDNEntity) obj;
		if (command != other.command)
			return false;
		if (nonce == null) {
			if (other.nonce != null)
				return false;
		} else if (!nonce.equals(other.nonce))
			return false;
		if (point == null) {
			if (other.point != null)
				return false;
		} else if (!point.equals(other.point))
			return false;
		if (tenantID == null) {
			if (other.tenantID != null)
				return false;
		} else if (!tenantID.equals(other.tenantID))
			return false;
		if (collectionID == null) {
			if (other.collectionID != null)
				return false;
		} else if (!collectionID.equals(other.collectionID))
			return false;
		if (userID == null) {
			if (other.userID != null)
				return false;
		} else if (!userID.equals(other.userID))
			return false;
		return true;
	}

	public GeoJSON getGeoJSON() {
		return geoJSON;
	}

	public void setGeoJSON(GeoJSON geoJSON) {
		this.geoJSON = geoJSON;
	}

	public String getReference() {
		return reference;
	}

	public void setReference(String reference) {
		this.reference = reference;
	}
}

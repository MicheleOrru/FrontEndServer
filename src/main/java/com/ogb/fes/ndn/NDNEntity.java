package com.ogb.fes.ndn;

import java.util.ArrayList;
import java.util.Random;

import com.ogb.fes.entity.GPSPoint;
import com.ogb.fes.utils.DateTime;
import com.ogb.fes.utils.GeoJSONContainer;
import com.ogb.fes.utils.Utils;

import net.named_data.jndn.Name;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.SecurityException;


public class NDNEntity
{
	public enum COMMANDS
	{
		INSERT,
		DELETE
	}
	
	private GeoJSONContainer  geoJSON;
	
	private GPSPoint point;
	private String   userID;
	private String   tenantID;
	private String   objID;
	private String	 collectionId;
	private String   reference;
	private COMMANDS command;
	
	
	private NDNEntity() {
		super();
	}

	public NDNEntity(GeoJSONContainer geoJSONContainer, int pointIndex, String reference, COMMANDS command) {
		this();

	
		this.point    	  = geoJSONContainer.getCoordinates().get(pointIndex);
		this.userID   	  = geoJSONContainer.getUserID();
		this.tenantID	  = geoJSONContainer.getTenantID();
		this.objID    	  = geoJSONContainer.getObjID();
		this.collectionId = geoJSONContainer.getCollectionID();
		
		this.reference    = reference;
		this.command  	  = command;
		this.geoJSON      = geoJSONContainer;
	}

	
	public String getDataName(int zoomLevel) {
		
		String prefix = Utils.gpsPointToNDNNname(point, zoomLevel, Utils.Format.LAT_LONG);
		String res    = "/OGB"+prefix+"/GPS_id/DATA/"+tenantID+"/"+collectionId+"/"+userID+"/"+objID;
		
		switch (command) {
			case INSERT:
				break;
			case DELETE:
				res =  "/OGB"+prefix+"/GPS_id/DELETE"+res;
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
		String prefix = Utils.gpsPointToNDNNname(point, 2, Utils.Format.LAT_LONG);
		String res    = "/OGB"+prefix+"/GPS_id/GEOJSON/"+tenantID+"/"+collectionId+"/"+userID+"/"+objID;
		
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
		result = prime * result + ((collectionId == null) ? 0 : collectionId.hashCode());
		result = prime * result + ((command == null) ? 0 : command.hashCode());
		result = prime * result + ((objID == null) ? 0 : objID.hashCode());
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
		if (objID == null) {
			if (other.objID != null)
				return false;
		} else if (!objID.equals(other.objID))
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
		if (collectionId == null) {
			if (other.collectionId != null)
				return false;
		} else if (!collectionId.equals(other.collectionId))
			return false;
		if (userID == null) {
			if (other.userID != null)
				return false;
		} else if (!userID.equals(other.userID))
			return false;
		return true;
	}
}

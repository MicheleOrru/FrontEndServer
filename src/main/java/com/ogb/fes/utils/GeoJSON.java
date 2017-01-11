package com.ogb.fes.utils;


import java.util.ArrayList;

import org.json.JSONObject;

import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.GeoJsonImportFlags;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.OperatorContains;
import com.esri.core.geometry.OperatorIntersects;
import com.esri.core.geometry.SpatialReference;
import com.ogb.fes.entity.GPSNode;
import com.ogb.fes.entity.GPSPoint;
import com.ogb.fes.ndn.NDNEntity.COMMANDS;


public class GeoJSON {

	protected JSONObject fullContent;			// Actual geoJSON content

	protected JSONObject properties;
	protected String     nonce;
	protected String     collectionID;
	protected String     tenantID;
	protected String     userID;
	protected String     objectID;
	
	protected String              type;
	protected JSONObject          geometry;
	protected ArrayList<GPSPoint> coordinates;
	
	protected GPSPoint containerSouthWest;
	protected GPSPoint containerNorthEst;


	protected GeoJSON(String tid, String uid, String cid) {
		super();

		fullContent = new JSONObject();
		properties  = new JSONObject();
		geometry    = new JSONObject();
		coordinates = new ArrayList<GPSPoint>();
		
		containerSouthWest = new GPSPoint();
		containerNorthEst  = new GPSPoint();

		nonce 		 = Utils.generateNonce(16);
		collectionID = cid;
		tenantID	 = tid;
		userID  	 = uid;
		objectID     = new String();
	}

	

	public boolean check() {

		if (type.compareToIgnoreCase("POINT") != 0 && type.compareToIgnoreCase("MULTIPOINT") != 0 && type.compareToIgnoreCase("POLYGON") != 0)
			return false;

		if (coordinates.size() <= 0)
			return false;

		for (GPSPoint point : coordinates ) {
			if (point.latitude < -90 || point.latitude > 90)
				return false;
			if (point.longitude < -180 || point.longitude > 180)
				return false;
		}

		return true;
	}


	public JSONObject getProperties() {
		return properties;
	}
	public JSONObject getGeometry() {
		return geometry;
	}
	public ArrayList<GPSPoint> getCoordinates() {
		return coordinates;
	}
	public String getNonce() {
		return nonce;
	}
	public String getTenantID() {
		return tenantID;
	}
	public String getUserID() {
		return userID;
	}
	public JSONObject getFullContent() {
		return fullContent;
	}
	public String getCollectionID() {
		return collectionID;
	}
	public String getOid()
	{
		return this.objectID;
	}

	public void setGeometry(JSONObject geometry) {
		this.geometry = geometry;
	}
	public void setProperties(JSONObject properties) {
		this.properties = properties;
	}
	public void setCoordinates(ArrayList<GPSPoint> coordinates) {
		this.coordinates = coordinates;
	}
	public void setNonce(String nonce) {
		this.nonce = nonce;

		JSONObject properties = fullContent.optJSONObject("properties");
		if (properties == null)
			properties = new JSONObject();
		
		properties.put("oid", this.nonce);
		fullContent.put("properties", properties);
	}
	public void insertObjID(String objID) {
		JSONObject properties = fullContent.optJSONObject("properties");
		if (properties == null)
			properties = new JSONObject();
		
		properties.put("oid", objID);
		fullContent.put("properties", properties);
		
	}
	public void setTenantID(String tenantID) {
		this.tenantID = tenantID;

		//JSONObject properties = fullContent.getJSONObject("properties");
		//properties.put("tid", this.tenantID);
		//fullContent.put("properties", properties);
	}
	public void setUserID(String userID) {
		this.userID = userID;

		//JSONObject properties = fullContent.getJSONObject("properties");
		//properties.put("uid", this.userID);
		//fullContent.put("properties", properties);
	}

	public void setFullContent(JSONObject fullContent) {
		this.fullContent = fullContent;
	}

	public void setCollectionID(String collectionID) {
		this.collectionID = collectionID;

		//JSONObject properties = fullContent.getJSONObject("properties");
		//properties.put("cid", this.collectionID);
		//fullContent.put("properties", properties);
	}


	public void computeNonce() {
		//GPSPoint firstPonint = this.coordinates.get(0);
		//String   prefix      = Utils.gpsPointToNDNNname(firstPonint, 2, Utils.Format.LONG_LAT);
		setNonce(Utils.generateNonce(16));
		//insertObjID("OGB"+prefix+"/GPS_id/"+Utils.generateNonce(16));
	}
	
	public void computeOid() {
		GPSPoint firstPonint = this.coordinates.get(0);
		String   prefix      = Utils.gpsPointToNDNNname(firstPonint, 2, Utils.Format.LONG_LAT);
		this.objectID        = "/OGB"+prefix+"/GPS_id/GEOJSON/"+this.tenantID+"/"+this.collectionID+"/"+this.userID+"/"+this.nonce;
		
		insertObjID(this.objectID);
	}
	
	public ArrayList<String> computeTilesConteined(COMMANDS command) {	
		Geometry actualGeometry = GeometryEngine.geometryFromGeoJson(geometry.toString(), GeoJsonImportFlags.geoJsonImportDefaults, Geometry.Type.Unknown).getGeometry();
	
		//Use to calculate the tile width and tile height as 1/tileSize
		//expressed in km not in GPS coordinates (1 = 1km)
		int level0Dimension = 100;
		int startLat = (int)Utils.floor10(containerSouthWest.latitude*level0Dimension,0);
		int startLng = (int)Utils.floor10(containerSouthWest.longitude*level0Dimension,0);
		int stopLat  = (int)Utils.ceil10(containerNorthEst.latitude*level0Dimension,0);
		int stopLng  = (int)Utils.ceil10(containerNorthEst.longitude*level0Dimension,0);
	
		//System.out.println("computeTilesConteined: GEOMETRY: " + actualGeometry);
		
		GPSNode           threeNode    = new GPSNode();
		ArrayList<String> fullNameList = getListOfCoveredAtLevel(threeNode, 0, 2, actualGeometry, startLat, stopLat, startLng, stopLng, command);
		ArrayList<String> nameList     = new ArrayList<String>();
		
		for (Rectangle rect : threeNode.getLeafArrayList()) {
			nameList.add(getCommandName(rect.computeGPSPoint(), rect.computeResolution(), command));
		}
		
		return nameList;
	}
	
	private ArrayList<String> getListOfCoveredAtLevel(GPSNode threeNode, int level, int last_level, Geometry jsonGeometry, int startLat, int stopLat, int startLng, int stopLng, COMMANDS command)
	{
		ArrayList<String> resultList = new ArrayList<>();
		SpatialReference sr = SpatialReference.create(4326);
		
		int step = (int)Math.pow(10, 2-level);

		//double inclusion
		double tileDimension = 0.01*step;
		//double tileDimension = 0.00999999*step;
		//System.out.println("\ncomputeTilesConteined: at level "+level+" step: " + step+" Box conteiner = ["+ startLat + "; " + startLng+"] ["+stopLat + "; " + stopLng+"]");
		
		for (int i=startLat; i<stopLat; i+=step) {
			for (int j=startLng; j<stopLng; j+=step) {
				
				double x = j/(double)100.0;
				double y = i/(double)100.0; 
				
				Envelope envelope = new Envelope(Utils.floor10(x, level), Utils.floor10(y, level), Utils.floor10(x+tileDimension, level), Utils.floor10(y+tileDimension, level));
//				for (int k = 0; k < level; k++) {
//					System.out.print("\t");
//				}
//				System.out.println(" Envelope "+ envelope);

				boolean conteined = OperatorContains.local().execute(jsonGeometry, envelope, sr, null);
				if (conteined) {
					//System.out.println("\t CONTEINED Envelope "+ envelope);
					double latName = y;
					double lonName = x;
					if (y < 0)
					{
						latName+=tileDimension;
						if (latName < 0.000001 && latName > -0.000001)
							latName = -0.00001;
					}
					if (x < 0)
					{
						lonName+=tileDimension;
						if (lonName < 0.000001 && lonName > -0.000001)
							lonName = -0.00001;
					}
					
					GPSPoint point = new GPSPoint(latName, lonName);
					threeNode.appendChild(point, level);
					resultList.add(getCommandName(point, level, command));
				}
				else
				{
					boolean intersect = OperatorIntersects.local().execute(jsonGeometry, envelope, sr, null);
					if (intersect) {	
						if (level != last_level)
						{
							//System.out.println("\t INTERSECTED Envelope "+ envelope);
							resultList.addAll(getListOfCoveredAtLevel(threeNode, level+1, last_level, jsonGeometry, i, i+step, j, j+step, command));
						}
						else							
						{
							//System.out.println("\t at level "+level + " Intersect " + envelope);
							double latName = y;
							double lonName = x;
							if (y < 0)
							{
								latName+=tileDimension;
								if (latName < 0.000001 && latName > -0.000001)
									latName = -0.00001;
							}
							if (x < 0)
							{
								lonName+=tileDimension;
								if (lonName < 0.000001 && lonName > -0.000001)
									lonName = -0.00001;
							}
							
							GPSPoint point = new GPSPoint(latName, lonName);
							threeNode.appendChild(point, level);
							resultList.add(getCommandName(point, level, command));
						}
					}
					//System.out.println("\t at level "+level + " NO Intersect " + envelope);
				}
			}
		}
		
		return resultList;
	}
	
	
	public String getCommandName(GPSPoint point, int zoomLevel, COMMANDS command) 
	{		
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
	

}

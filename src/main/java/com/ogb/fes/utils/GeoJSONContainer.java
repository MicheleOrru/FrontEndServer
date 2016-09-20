package com.ogb.fes.utils;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ogb.fes.entity.GPSPoint;


public class GeoJSONContainer {

	private JSONObject fullContent;			// Actual geoJSON content

	private JSONObject properties;
	private String     objID;
	private String     collectionID;
	private String     tenantID;
	private String     userID;

	private String              type;
	private JSONObject          geometry;
	private ArrayList<GPSPoint> coordinates;


	public GeoJSONContainer(String tid, String uid, String cid) {
		super();

		fullContent = new JSONObject();
		properties  = new JSONObject();
		geometry    = new JSONObject();
		coordinates = new ArrayList<GPSPoint>();

		objID 		 = new String();
		collectionID = cid;
		tenantID	 = tid;
		userID  	 = uid;
	}

	public GeoJSONContainer(JSONObject json, String tid, String uid, String cid) {
		this(tid, uid, cid);

		fullContent = json;

		properties  = json.optJSONObject("properties");
		geometry    = json.optJSONObject("geometry");

		type = geometry.optString("type");

		if (type.equals("MultiPoint"))
		{
			JSONArray jsonArray = geometry.optJSONArray("coordinates");
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONArray coordinate = jsonArray.getJSONArray(i);
				coordinates.add(new GPSPoint(coordinate.getDouble(0), coordinate.getDouble(1)));
			}
		} 
		else if (type.equals("Point"))
		{
			JSONArray coordinate = geometry.optJSONArray("coordinates");
			coordinates.add(new GPSPoint(coordinate.getDouble(0), coordinate.getDouble(1)));
		}

		//userID   	 = properties.optString("uid");
		//tenantID 	 = properties.optString("tid");		
		//collectionID = properties.optString("cid");
		//objID    	 = properties.optString("oid");
	}

	public GeoJSONContainer(Map<String, Object> map, String tid, String uid, String cid) throws JSONException, JsonProcessingException {
		this(new JSONObject(new ObjectMapper().writeValueAsString(map)),tid, uid, cid); 
	}

	public boolean check() {

		if (type.compareToIgnoreCase("POINT") != 0 && type.compareToIgnoreCase("MULTIPOINT") != 0)
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
	public String getObjID() {
		return objID;
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


	public void setGeometry(JSONObject geometry) {
		this.geometry = geometry;
	}
	public void setProperties(JSONObject properties) {
		this.properties = properties;
	}
	public void setCoordinates(ArrayList<GPSPoint> coordinates) {
		this.coordinates = coordinates;
	}
	public void setObjID(String objID) {
		this.objID = objID;

		JSONObject properties = fullContent.getJSONObject("properties");
		properties.put("oid", this.objID);
		fullContent.put("properties", properties);
	}
	public void insertObjID(String objID) {
		JSONObject properties = fullContent.getJSONObject("properties");
		properties.put("oid", this.objID);
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


	public void computeObjID() {
		GPSPoint firstPonint = this.coordinates.get(0);
		String   prefix      = Utils.gpsPointToNDNNname(firstPonint, 2, Utils.Format.LAT_LONG);
		setObjID("OGB"+prefix+"/GPS_id/"+Utils.generateNonce(16));
		insertObjID("OGB"+prefix+"/GPS_id/"+Utils.generateNonce(16));
	}

}

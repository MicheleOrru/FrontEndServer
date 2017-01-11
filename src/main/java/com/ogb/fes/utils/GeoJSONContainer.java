package com.ogb.fes.utils;


import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ogb.fes.entity.GPSPoint;


public class GeoJSONContainer extends GeoJSON {
	
	public GeoJSONContainer(JSONObject json, String tid, String uid, String cid) {
		super(tid, uid, cid);

		fullContent = json;
		properties  = json.optJSONObject("properties");
		geometry    = json.optJSONObject("geometry");
		type        = geometry.optString("type");
		
		if (type.equals("Point"))
		{
			JSONArray coordinate = geometry.optJSONArray("coordinates");
			coordinates.add(new GPSPoint(coordinate.getDouble(1), coordinate.getDouble(0)));
			
			double lat = coordinate.getDouble(1);
			double lon = coordinate.getDouble(0);
			
			containerSouthWest = new GPSPoint(Utils.floor10(lat, 0),Utils.floor10(lon, 0));
			containerNorthEst  = new GPSPoint(Utils.ceil10(lat+0.01, 0),Utils.ceil10(lon+0.01, 0));			
		}
		else if (type.equals("MultiPoint"))
		{
			JSONArray jsonArray = geometry.optJSONArray("coordinates");
			double    south     = Double.MAX_VALUE;
			double    west      = Double.MAX_VALUE;
			double    north     = Double.MIN_VALUE;
			double    east      = Double.MIN_VALUE;
			
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONArray coord = jsonArray.getJSONArray(i);
				double    lat   = coord.getDouble(1);
				double    lon   = coord.getDouble(0);
				
				if (lat < south)
					south = lat;
				if (lat > north)
					north = lat;
				if (lon < west)
					west = lon;
				if (lon > east)
					east = lon;

				coordinates.add(new GPSPoint(coord.getDouble(1), coord.getDouble(0)));
			}
			
			containerSouthWest = new GPSPoint(Utils.floor10(south, 0),Utils.floor10(west, 0));
			containerNorthEst  = new GPSPoint(Utils.ceil10(north+0.01, 0),Utils.ceil10(east+0.01, 0));
		} 
		else if (type.equals("Polygon"))
		{
			JSONArray jsonArray = geometry.optJSONArray("coordinates");
			double    south     = Double.MAX_VALUE;
			double    west      = Double.MAX_VALUE;
			double    north     = Double.MIN_VALUE;
			double    east      = Double.MIN_VALUE;
			
			jsonArray = jsonArray.optJSONArray(0);
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONArray coord = jsonArray.getJSONArray(i);
				double    lat   = coord.getDouble(1);
				double    lon   = coord.getDouble(0);
				
				if (lat < south)
					south = lat;
				if (lat > north)
					north = lat;
				if (lon < west)
					west = lon;
				if (lon > east)
					east = lon;

				coordinates.add(new GPSPoint(coord.getDouble(1), coord.getDouble(0)));
			}
			
			containerSouthWest = new GPSPoint(Utils.floor10(south, 0), Utils.floor10(west, 0));
			containerNorthEst  = new GPSPoint(Utils.ceil10(north+0.01, 0), Utils.ceil10(east+0.01, 0));	
		}

		computeNonce();
		computeOid();
	}

	public GeoJSONContainer(Map<String, Object> map, String tid, String uid, String cid) throws JSONException, JsonProcessingException {
		this(new JSONObject(new ObjectMapper().writeValueAsString(map)), tid, uid, cid); 
	}
}

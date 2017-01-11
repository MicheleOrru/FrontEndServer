package com.ogb.fes.utils;


import java.util.ArrayList;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.GeoJsonImportFlags;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.OperatorContains;
import com.esri.core.geometry.OperatorIntersects;
import com.esri.core.geometry.SpatialReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ogb.fes.entity.GPSNode;
import com.ogb.fes.entity.GPSPoint;
import com.ogb.fes.entity.GPSRect;
import com.ogb.fes.ndn.NDNEntity.COMMANDS;

/*
 * "geometry": {
		"$geoIntersects": {
			"$geometry": {
				"coordinates": 
 */

public class GeoJSONQuery extends GeoJSON {
	
	public GeoJSONQuery(JSONObject json, String tid, String uid, String cid) {
		super(tid, uid, cid);

		fullContent = json;
		properties  = json.optJSONObject("properties");
		geometry    = json.optJSONObject("geometry").optJSONObject("$geoIntersects").optJSONObject("$geometry");
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
	
	
	public GeoJSONQuery(Map<String, Object> map, String tid, String uid, String cid)  throws JSONException, JsonProcessingException {
		this(new JSONObject(new ObjectMapper().writeValueAsString(map)), tid, uid, cid); 
	}
	
	
	public ArrayList<Rectangle> computeTilesConteined(COMMANDS command, int maxTiles) {		
		Geometry actualGeometry = GeometryEngine.geometryFromGeoJson(geometry.toString(), GeoJsonImportFlags.geoJsonImportDefaults, Geometry.Type.Unknown).getGeometry();
		
		//Use to calculate the tile width and tile height as 1/tileSize
		//expressed in km not in GPS coordinates (1 = 1km)
		int level0Size = 100;
		int startLat   = (int)Utils.floor10(containerSouthWest.latitude*level0Size,0);
		int startLng   = (int)Utils.floor10(containerSouthWest.longitude*level0Size,0);
		int stopLat    = (int)Utils.ceil10(containerNorthEst.latitude*level0Size,0);
		int stopLng    = (int)Utils.ceil10(containerNorthEst.longitude*level0Size,0);
		int endLevel   = 2;
		
		GPSNode threeHead = new GPSNode();
		GPSRect gpsRect   = new GPSRect(containerNorthEst, containerSouthWest);
		if (gpsRect.estimateTiles100Count() > maxTiles) {
			endLevel = 0;
		}
		if (gpsRect.estimateTiles10Count() > maxTiles) {
			endLevel = 1;
		}
		getListOfCoveredAtLevel(threeHead, 0, endLevel, actualGeometry, startLat, stopLat, startLng, stopLng, command);
		ArrayList<Rectangle> totale = threeHead.aggregateThree(maxTiles);
		totale = threeHead.aggregateThree(maxTiles);
		return totale;
	}
	
	private ArrayList<String> getListOfCoveredAtLevel(GPSNode threeNode, int level, int lastLevel, Geometry jsonGeometry, int startLat, int stopLat, int startLng, int stopLng, COMMANDS command) {
		
		ArrayList<String> resultList = new ArrayList<>();
		SpatialReference  sr = SpatialReference.create(4326);
		
		int    step     = (int)Math.pow(10, 2-level);
		double tileSize = 0.01*step;

		for (int i=startLat; i<stopLat; i+=step) {
			for (int j=startLng; j<stopLng; j+=step) {
				
				double x = j/(double)100.0;
				double y = i/(double)100.0; 
				
				Envelope envelope = new Envelope(Utils.floor10(x, level), Utils.floor10(y, level), Utils.floor10(x+tileSize, level), Utils.floor10(y+tileSize, level));
				
				boolean conteined = OperatorContains.local().execute(jsonGeometry, envelope, sr, null);
				if (conteined) {
					appendNodeAndName(threeNode, resultList, y, x, tileSize, lastLevel, command);
				}
				else {
					boolean intersect = OperatorIntersects.local().execute(jsonGeometry, envelope, sr, null);
					if (intersect) {	
						if (level != lastLevel) {
							resultList.addAll(getListOfCoveredAtLevel(threeNode, level+1, lastLevel, jsonGeometry, i, i+step, j, j+step, command));
						}
						else {
							appendNodeAndName(threeNode, resultList, y, x, tileSize, lastLevel, command);
						}
					}
				}
			}
		}
		
		return resultList;
	}
	
	
	private void appendNodeAndName(GPSNode threeNode, ArrayList<String> resultList, double y, double x, double tileSize, int level, COMMANDS command) {
		//System.out.println("\t at level "+level + " Intersect " + envelope);
		double latName = y;
		double lonName = x;
		if (y < 0)
		{
			latName+=tileSize;
			if (latName < 0.000001 && latName > -0.000001)
				latName = -0.00001;
		}
		if (x < 0)
		{
			lonName+=tileSize;
			if (lonName < 0.000001 && lonName > -0.000001)
				lonName = -0.00001;
		}
		
		GPSPoint point = new GPSPoint(latName, lonName);
		threeNode.appendChild(point, level);
		resultList.add(getCommandName(point, level, command));
	}
	
	public double computeArea() {
		Geometry actualGeometry = GeometryEngine.geometryFromGeoJson(geometry.toString(), GeoJsonImportFlags.geoJsonImportDefaults, Geometry.Type.Unknown).getGeometry();
		return actualGeometry.calculateArea2D()*100*100;
	}
}

package com.ogb.fes.entity;


import java.awt.color.CMMException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.json.JSONException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ogb.fes.ndn.NDNEntity.COMMANDS;
import com.ogb.fes.utils.DateTime;
import com.ogb.fes.utils.GeoJSONQuery;
import com.ogb.fes.utils.Rectangle;
import com.ogb.fes.utils.Utils;
import com.ogb.fes.utils.Utils.Format;


public class RangeQueryParams 
{
	public static int MAX_TILES  = 80;
	public static int MIN_TILES  = 10;
	public static int STEP_TILES = 10;

	public String              tid, cid, uid;
	public ArrayList<String>   requestedProperties;
	public String              queryFunction;
	public String              queryType;
	
	public ArrayList<ArrayList<GPSPoint>> coordinatesPolygonPoint; //Used for handle queryType == Polygon
	public ArrayList<GPSPoint>            coordinatesBoxPoint;     //Used for handle queryType == Box

	private HashMap<String, Object> hashMapParamas;

	public RangeQueryParams() {
		super();

	}

	@SuppressWarnings("unchecked")
	public boolean setParams(HashMap<String, Object> params, String tid, String uid, String cid) {

		this.tid = tid;
		this.cid = cid;
		this.uid = uid;
		this.hashMapParamas = params;
		
		requestedProperties = new ArrayList<String>();
		if (params.containsKey("$select") == true) 
		{
			requestedProperties = (ArrayList<String>)params.get("$select");
			System.out.println("Retrive $select Success!");
		}


		if (params.containsKey("geometry") == true)
		{
			//System.out.println("Retrive geometry Success!");

			HashMap<String, Object> locationField = (HashMap<String, Object>)params.get("geometry");
			if (locationField.containsKey("$geoIntersects") == true)
			{
				queryFunction = "$geoIntersects";
				//System.out.println("Retrive $geoIntersects Success!");

				HashMap<String, Object> functionField = (HashMap<String, Object>)locationField.get("$geoIntersects");
				if (functionField.containsKey("$geometry") == true)
				{
					//System.out.println("Retrive $geometry Success!");
					HashMap<String, Object> geometry = (HashMap<String, Object>)functionField.get("$geometry");
					
					queryType = (String)geometry.get("type");

					if (queryType.compareToIgnoreCase("Box") == 0) {
						coordinatesBoxPoint = new ArrayList<GPSPoint>();
						for (List<Double> coord: (List<List<Double>>)geometry.get("coordinates")) {
							coordinatesBoxPoint.add(new GPSPoint(coord.get(1), coord.get(0)));
						}
					}
					else if (queryType.compareToIgnoreCase("Polygon") == 0) {
						coordinatesPolygonPoint = new ArrayList<ArrayList<GPSPoint>>();
						for (List<List<Double>> coord: (List<List<List<Double>>>)geometry.get("coordinates")) {
							ArrayList<GPSPoint> polyg = new ArrayList<>();
							for (List<Double> singleCoord: coord) {
								polyg.add(new GPSPoint(singleCoord.get(1), singleCoord.get(0)));
							}
							coordinatesPolygonPoint.add(polyg);
						}
					}

					return true;
				}
				else
				{
					System.out.println(DateTime.currentTime() +" RangeQueryParams - Retrive $geometry Error!");
				}
			}
			else
			{
				System.out.println(DateTime.currentTime() +" RangeQueryParams - Retrive $geoIntersects Error!");
			}
		}
		else
		{
			System.out.println(DateTime.currentTime() +" RangeQueryParams - Retrive geometry Error!");
		}

		return false;
	}

	public HashSet<String> tessellation(ServiceStats stats) {
		if (queryFunction.compareToIgnoreCase("$geoIntersects") == 0) {
			return tessellationIntersect(stats);
		}

		return new HashSet<>();
	}


	private HashSet<String> tessellationIntersect(ServiceStats stats) {

		//System.out.println(DateTime.currentTime() + "RangeQueryParams - tessellationIntersect: queryType: " + queryType );
		
		if (queryType.compareToIgnoreCase("Box") == 0) {
			return tessellationIntersectBox(stats);
		}
		else if (queryType.compareToIgnoreCase("Polygon") == 0) {
			try {
				return tessellationIntersectPolygon(stats);
			}
			catch (Exception e) {
				return new HashSet<String>();
			}
		}

		return new HashSet<String>();
	}

	private HashSet<String> tessellationIntersectPolygon(ServiceStats stats) throws JSONException, JsonProcessingException  {
		double          startTime;
		double          stopTime;
		HashSet<String> tileNames    = new HashSet<String>();
		GeoJSONQuery    geoJSONQuery = new GeoJSONQuery(hashMapParamas, tid, uid, cid);
		
		//Storing request area to stats object
		stats.setRequestArea(geoJSONQuery.computeArea());
		
		startTime = System.nanoTime();
		ArrayList<Rectangle> tilesRect = new ArrayList<Rectangle>();
		for (int m=MIN_TILES;m<=MAX_TILES;m=m+STEP_TILES) {
			tilesRect = geoJSONQuery.computeTilesConteined(COMMANDS.TILE, m);
			if ((computeResponseSquareArea(tilesRect))<2*stats.getRequestArea())
				break;
		}
		
		stopTime = System.nanoTime();
		
		stats.setTilesComputedTime(stopTime-startTime);
		
		//Storing optimal cover computation time (in millisecond)
		stats.setTilesComputedTime((stopTime-startTime)/1e6);

		//Storing tilesRect area to stats object
		stats.setResponseArea(computeResponseSquareArea(tilesRect));

		//Storing the tilesRect count
		stats.setTilesCount(tilesRect.size());
		
		for (Rectangle rect : tilesRect) {
			tileNames.add(geoJSONQuery.getCommandName(rect.computeGPSPoint(), rect.computeResolution(), COMMANDS.TILE));
		}
		
		//Storing the tilesRect count
		stats.setTilesCount(tileNames.size());

		return tileNames;
	}
	
	private HashSet<String> tessellationIntersectBox(ServiceStats stats) {

		GPSRect roundedRect    = new GPSRect(coordinatesBoxPoint.get(1), coordinatesBoxPoint.get(0));
		int     resolution     = 2;
		int     relativeOffset = 1000;

		double startTime;
		double stopTime;

		startTime = System.nanoTime();

		//Storing request area to stats object
		stats.setRequestArea(roundedRect.computeArea()*100*100);
		ArrayList<Rectangle> tilesRect = new ArrayList<Rectangle>();		
		roundedRect = roundedRect.computeRelativeRect(relativeOffset);
		
		for (int m=MIN_TILES;m<=MAX_TILES;m=m+STEP_TILES) {
			tilesRect = restoreToOriginCoordinate(roundedRect.computeOptimalCoverTree(m), relativeOffset);
			if ((computeResponseSquareArea(tilesRect))<2*stats.getRequestArea())
				break;
		}
		
		stopTime = System.nanoTime();
		
		//Storing optimal cover computation time (in millisecond)
		stats.setTilesComputedTime((stopTime-startTime)/1e6);

		//Storing tilesRect area to stats object
		stats.setResponseArea(computeResponseSquareArea(tilesRect));

		//Storing the tilesRect count
		stats.setTilesCount(tilesRect.size());

		//System.out.println(DateTime.currentTime()+"RangeQuery - TilesRect: " + tilesRect);

		HashSet<String>    tileNames    = new HashSet<String>();
		ArrayList<GPSRect> tilesGPSRect = new ArrayList<GPSRect>();
		//Rectangle          queryRect    = new Rectangle(new GPSRect(coordinatesBoxPoint.get(1), coordinatesBoxPoint.get(0)));
		for (Rectangle rect : tilesRect) {
			GPSRect gpsRect = rect.toGPSRect();
			//System.out.println("Intersection area:" + rect.computeIntersectionAreaPercentage(new Rectangle(new GPSRect(coordinatesPoint.get(1), coordinatesPoint.get(0)))));
			// Repo side range query temporary disabled since not yet supported 
//			if (rect.computeIntersectionAreaPercentage(queryRect)>0.7) {
//				gpsRect.setInternal(true);
//			} else {
//				gpsRect.setInternal(false);
//			}
			gpsRect.setInternal(true);

			resolution = rect.computeResolution();
			tilesGPSRect.add(gpsRect);

			//String gps_id = Utils.gpsPointToNDNNname(new GPSPoint(rect.x, rect.y), resolution, Format.LONG_LAT);
			String gps_id = Utils.gpsRectToNDNName(gpsRect, resolution, Format.LONG_LAT);
			//System.out.println("REQUEST: " + gps_id);
			//System.out.println("MapService - Tail: " + tail + " -- gps_id: " + gps_id + " --- Resolution: " + resolution) ;

			String ndnListPrefix = "/OGB" + gps_id + "/GPS_id/TILE"; 

			String ndnDataRequestName = "/OGB" + gps_id + "/GPS_id/DATA/" + tid + "/" + cid;
			ndnDataRequestName = ndnListPrefix + ndnDataRequestName;

			/*
			if (!gpsRect.isInternal && resolution<1) {
				String ndnQueryName = "/QUERY/"+coordinatesBoxPoint.get(0).longitude+"/"+coordinatesBoxPoint.get(0).latitude + "/" + coordinatesBoxPoint.get(1).longitude + "/" + coordinatesBoxPoint.get(1).latitude;
				ndnDataRequestName +=ndnQueryName;
			}
			*/
			//Add the hash code to make unique the ndn name (wrong cache issue)
			ndnDataRequestName += "/"+ndnDataRequestName.hashCode();

			tileNames.add(ndnDataRequestName);
			//System.out.println(DateTime.currentTime()+"RangeQuery - NDN Reqeuest Element added: " + ndnDataRequestName);
		}

		//Storing the tilesRect count
		stats.setTilesCount(tileNames.size());

		return tileNames;
	}

	private ArrayList<Rectangle> restoreToOriginCoordinate(ArrayList<Rectangle> relList, float relValue) {

		ArrayList<Rectangle> list = new ArrayList<Rectangle>();

		for (Rectangle relRect : relList ) {
			list.add(new Rectangle(Utils.floor10(relRect.x-relValue,2), Utils.floor10(relRect.y-relValue,2), Utils.floor10(relRect.width, 2), Utils.floor10(relRect.height, 2)));
		}

		return list;
	}

	public double computeResponseSquareArea(ArrayList<Rectangle> list) {

		double resArea = 0.0;

		for (Rectangle rect : list) {
			resArea+=rect.computeArea()*100*100;
		}

		return Utils.floor10(resArea, 2);
	}

	@Override
	public String toString() {
		return "RangeQuery [Function=" + queryFunction + ", Type=" + queryType + ", Coordinates=" + coordinatesBoxPoint+"]";
	}
}

package com.ogb.fes.entity;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import com.ogb.fes.utils.Rectangle;
import com.ogb.fes.utils.Utils;
import com.ogb.fes.utils.Utils.Format;


public class RangeQueryParams 
{
	public static int MAX_TILES = 80;

	public String              tid, cid, uid;
	public ArrayList<String>   requestedProperties;
	public String              queryFunction;
	public String              queryType;
	public ArrayList<GPSPoint> coordinatesPoint;


	public RangeQueryParams() {
		super();


	}

	public boolean setParams(HashMap<String, Object> params, String tid, String uid, String cid) {


		this.tid = tid;
		this.cid = cid;
		this.uid = uid;

		requestedProperties = new ArrayList<String>();
		if (params.containsKey("$select") == true) 
		{
			requestedProperties = (ArrayList<String>)params.get("$select");
			System.out.println("Retrive $select Success!");
		}


		if (params.containsKey("geometry") == true)
		{
			//System.out.println("Retrive geometry Success!");

			@SuppressWarnings("unchecked")
			HashMap<String, Object> locationField = (HashMap<String, Object>)params.get("geometry");
			if (locationField.containsKey("$geoIntersects") == true)
			{
				queryFunction = "$geoIntersects";
				//System.out.println("Retrive $geoIntersects Success!");
				
				@SuppressWarnings("unchecked")
				HashMap<String, Object> functionField = (HashMap<String, Object>)locationField.get("$geoIntersects");
				if (functionField.containsKey("$geometry") == true)
				{
					//System.out.println("Retrive $geometry Success!");
					
					@SuppressWarnings("unchecked")
					HashMap<String, Object> geometry = (HashMap<String, Object>)functionField.get("$geometry");

					queryType        = (String)geometry.get("type");
					coordinatesPoint = new ArrayList<GPSPoint>();
					for (List<Double> coord: (List<List<Double>>)geometry.get("coordinates")) {
						coordinatesPoint.add(new GPSPoint(coord.get(1), coord.get(0)));
					}

					return true;
				}
				else
				{
					System.out.println("Retrive $geometry Error!");
				}
			}
			else
			{
				System.out.println("Retrive $geoIntersects Error!");
			}
		}
		else
		{
			System.out.println("Retrive geometry Error!");
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

		if (queryType.compareToIgnoreCase("Box") == 0) {
			return tessellationIntersectBox(stats);
		}

		return new HashSet<String>();
	}

	private HashSet<String> tessellationIntersectBox(ServiceStats stats) {

		GPSRect roundedRect    = new GPSRect(coordinatesPoint.get(1), coordinatesPoint.get(0));
		int     resolution     = 2;
		int     relativeOffset = 1000;

		double startTime;
		double stopTime;


		startTime = System.nanoTime();
		//System.out.println(DateTime.currentTime()+"RangeQuery - RectBound:  " + roundedRect);
		//System.out.println(DateTime.currentTime()+"RangeQuery - Resolution: " + resolution);
		//System.out.println(DateTime.currentTime()+"RangeQuery - MaxTiles:   " + MAX_TILES+"\n");

		//Storing request area to stats object
		stats.setRequestArea(roundedRect.computeArea()*100*100);
		ArrayList<Rectangle> tilesRect = new ArrayList<Rectangle>();		
		//System.out.println(DateTime.currentTime()+"RangeQuery - Before RoundedRect: " + roundedRect);
		roundedRect = roundedRect.computeRelativeRect(relativeOffset);
		//stopTime = System.nanoTime();
		//System.out.println(DateTime.currentTime()+"RangeQuery - After RoundedRect: " + roundedRect + " - Time: " + (stopTime-startTime)/1e6 +"s\n");

		//startTime = System.nanoTime();
		//System.out.println(DateTime.currentTime()+"RangeQuery - Before OptimalCover");
		tilesRect = restoreToOriginCoordinate(roundedRect.computeOptimalCoverTree(MAX_TILES), relativeOffset);
		stopTime = System.nanoTime();
		//System.out.println(DateTime.currentTime()+"RangeQuery - After OptimalCover Tiles Count: "+tilesRect.size() + " - Time: " + (stopTime-startTime)/1e9 +" s\n");

		//Storing optimal cover computation time (in millisecond)
		stats.setTilesComputedTime((stopTime-startTime)/1e6);

		//Storing tilesRect area to stats object
		stats.setResponseArea(computeResponseSquareArea(tilesRect));

		//Storing the tilesRect count
		stats.setTilesCount(tilesRect.size());

		//System.out.println(DateTime.currentTime()+"RangeQuery - TilesRect: " + tilesRect);

		HashSet<String>    tileNames    = new HashSet<String>();
		ArrayList<GPSRect> tilesGPSRect = new ArrayList<GPSRect>();
		Rectangle queryRect = new Rectangle(new GPSRect(coordinatesPoint.get(1), coordinatesPoint.get(0)));
		for (Rectangle rect : tilesRect) {
			GPSRect gpsRect = rect.toGPSRect();
			//System.out.println("Intersection area:" + rect.computeIntersectionAreaPercentage(new Rectangle(new GPSRect(coordinatesPoint.get(1), coordinatesPoint.get(0)))));
			if (rect.computeIntersectionAreaPercentage(queryRect)>0.7) {
				gpsRect.setInternal(true);
			} else {
				gpsRect.setInternal(false);
			}


			resolution = rect.computeResolution();
			tilesGPSRect.add(gpsRect);

			//String gps_id = Utils.gpsPointToNDNNname(new GPSPoint(rect.x, rect.y), resolution, Format.LONG_LAT);
			String gps_id = Utils.gpsRectToNDNName(gpsRect, resolution, Format.LONG_LAT);
			//System.out.println("REQUEST: " + gps_id);
			//System.out.println("MapService - Tail: " + tail + " -- gps_id: " + gps_id + " --- Resolution: " + resolution) ;

			String ndnListPrefix = "/OGB" + gps_id + "/GPS_id/TILE"; 

			String ndnDataRequestName = "/OGB" + gps_id + "/GPS_id/DATA/" + tid + "/" + cid;
			ndnDataRequestName = ndnListPrefix + ndnDataRequestName;

			if (!gpsRect.isInternal && resolution<1) {
				String ndnQueryName = "/QUERY/"+coordinatesPoint.get(0).longitude+"/"+coordinatesPoint.get(0).latitude
						+ "/" + coordinatesPoint.get(1).longitude + "/" + coordinatesPoint.get(1).latitude;
				ndnDataRequestName +=ndnQueryName;
			}
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
		return "RangeQuery [Function=" + queryFunction + ", Type=" + queryType + ", Coordinates=" + coordinatesPoint+"]";
	}
}

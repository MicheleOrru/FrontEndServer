package com.ogb.fes.service;


import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.json.JacksonJsonParser;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonParser;
import com.ogb.fes.FesApplication;
import com.ogb.fes.domain.User;
import com.ogb.fes.domainRepositories.UserRepository;
import com.ogb.fes.entity.ErrorResponse;
import com.ogb.fes.entity.GPSPoint;
import com.ogb.fes.entity.QueryServicePostResponse;
import com.ogb.fes.entity.RangeQueryParams;
import com.ogb.fes.entity.ServiceStats;
import com.ogb.fes.ndn.NDNQueryManager;
import com.ogb.fes.net.NetManager;
import com.ogb.fes.utils.DateTime;
import com.ogb.fes.utils.Rectangle;
import com.ogb.fes.utils.Utils;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.GeoJsonImportFlags;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.MapGeometry;
import com.esri.core.geometry.OperatorIntersects;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.SpatialReference;


@RestController
public class QueryService 
{
	@Autowired
	private UserRepository userRepo;

	static NDNQueryManager ndnQueryManager = NDNQueryManager.sharedManager();
	public static String     serverIP;   // Configured by FesApplication;


	private User checkAuthToken(String authToken) {
		if (authToken.length() <= 0)
			return null;

		User user = userRepo.findByToken(authToken);

		if (user == null) {
			user = checkTokenOnAUCServer(authToken);
		}

		return user;
	}

	private User checkTokenOnAUCServer(String token) {
		Map<String, Object> postParams = new HashMap<String, Object>();
		postParams.put("token", token);
		Map <String, Object> aucUser = new NetManager().sendCheckToken(postParams);
		if (aucUser == null)
			return null;

		return new User(aucUser);
	}

	@CrossOrigin(origins="*")
	@RequestMapping(method=RequestMethod.POST, value="/OGB/query-service/{cid}", consumes={"application/json"}, produces={"application/json"})
	public @ResponseBody Object postRequest(HttpServletResponse response, @RequestBody HashMap<String, Object> params, @PathVariable String cid, @RequestHeader(value="Authorization", defaultValue="") String authToken) throws Exception {

		//System.out.println("Received token: "+authToken);
		long startTime=System.currentTimeMillis();
		User user = checkAuthToken(authToken);
		if (user == null) {
			response.setStatus(420);
			return new ErrorResponse(420, "Invalid authorization token");
		}
		String tid = user.getUserID().split("/")[0];
		String uid = user.getUserID().split("/")[1];

		RangeQueryParams queryParams = new RangeQueryParams();
		if (queryParams.setParams(params,tid,uid,cid) == false) {
			response.setStatus(430);
			return new ErrorResponse(430, "Invalid query params");
		}

		ServiceStats    stats     = new ServiceStats();
		HashSet<String> tileNames = queryParams.tessellation(stats);


		//Filter tile names with the bloom filter service
		//tileNames = bfFilterService(tileNames, stats);

		//Resolve ndn query and filter the resultSet according to queryParams to reduce overdata response
		ArrayList<String> tileData = getTileData(tileNames, stats);
		long startTimePost=System.currentTimeMillis();
		tileData = postProcessingResult(tileData, queryParams);
		stats.setPostProcessingTime(System.currentTimeMillis()-startTimePost);
		
		//Generate the response object
		/*
		QueryServicePostResponse mapResponse = new QueryServicePostResponse(); 
		mapResponse.setTilesData(tileData);
		mapResponse.setFileList(new ArrayList<FilePair>(filePairset));
		mapResponse.setStats(stats);
		stopTime = System.nanoTime();
		System.out.println(DateTime.currentTime()+"MapService - After Response" + " - Time: " + (stopTime-startTime)/1e9 +" s\n");
		 */

		//ArrayList<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
		//for (String data : tileData)
		//	result.add(new JacksonJsonParser().parseMap(data));
		
		stats.setRequestTimeMillis(System.currentTimeMillis()-startTime);
		System.out.println("STATS: " + stats.toString());
		return tileData.toString();
	}

	private HashSet<String> bfFilterService(HashSet<String> arrayList, ServiceStats stats) {
		double startTime, stopTime;

		try {
			startTime = System.nanoTime();

			String url = "http://cloud.netgroup.uniroma2.it:8090/BFS/contains/";
			HttpURLConnection con = (HttpURLConnection)new URL(url).openConnection();

			con.setRequestProperty("Content-Type", "application/json");
			con.setRequestMethod("POST");

			JSONArray list = new JSONArray();
			for (String string : arrayList) {
				list.put(string);
			}

			String postParameters = list.toString();

			// Send post request
			con.setDoOutput(true);
			DataOutputStream wr = new DataOutputStream(con.getOutputStream());
			wr.writeBytes(postParameters);
			wr.flush();
			wr.close();

			int responseCode = con.getResponseCode();
			//System.out.println("Sending 'POST' to URL: " + url);
			//System.out.println("Response Code:         " + responseCode);

			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			StringBuffer response = new StringBuffer();
			String inputLine;

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();


			if (responseCode == 200) {
				//System.out.print("Filter result success!");
				JSONArray objectList = new JSONArray(response.toString());
				HashSet<String> filterList = new HashSet<String>();
				for (int i = 0; i < objectList.length(); i++)
					filterList.add(objectList.getString(i));

				stopTime = System.nanoTime();

				//Storing the tilesRect count with data
				stats.setTilesWithDataCount(filterList.size());

				//Storing the bloomFilter request-response time in millisecond
				stats.setBloomFilterRequestTime((stopTime-startTime)/1e6);


				return filterList;
			}

			//System.out.print("Error on post!!!");
			return arrayList;
		}
		catch (Exception exception) {
			return arrayList;
		}
	}

	private ArrayList<String> getTileData(HashSet<String> ndnNames, ServiceStats stats) throws Exception {
		double startTime, stopTime;

		HashMap<String, String> dataResult = new HashMap<String, String>();
		HashSet<String>         references = new HashSet<String>();


		startTime = System.nanoTime();
		//System.out.println(DateTime.currentTime()+"QueryService - Before ExecNDNRequest");
		ndnQueryManager.execNdnTileRequest(ndnNames, serverIP);
		ndnQueryManager.joinAll();
		ArrayList<String> ndnTileContents = ndnQueryManager.popAllResults();
		//System.out.println("********** Tile query latency:"+(System.nanoTime()-startTime)/1e6);
		//long startTime2 = System.nanoTime();
		for (String tileContent : ndnTileContents) {
			//System.out.println("*****************tileContent: "+tileContent);
			if (tileContent.startsWith("{") == false) {
				references.add(tileContent);
			}
			else {
				String oid = fastOidExtractor(tileContent);
				//System.out.println(DateTime.currentTime()+"QueryService - OID: " + oid);
				if (oid.length() > 0)
					dataResult.put(oid, tileContent);
			}
		}


		//Resolving the references if needed
		if (references.size() > 0) {

			ndnQueryManager.execNdnDataRequest(references, serverIP);
			ndnQueryManager.joinAll();
			ndnTileContents = ndnQueryManager.popAllResults();

			for (String tileContent : ndnTileContents) {
				//System.out.println(DateTime.currentTime()+"QueryService - TileContent: " + tileContent);
				if (tileContent.startsWith("{") == false) {
					throw new Exception(DateTime.currentTime()+"QueryService - Duplicate references in inserted OGB data!");
				}
				else {
					//Handle the fast oid search for better performance don't use jsonobject parser
					String oid = fastOidExtractor(tileContent);
					//System.out.println(DateTime.currentTime()+"QueryService - OID: " + oid);
					if (oid.length() > 0)
						dataResult.put(oid, tileContent);
				}
			}
		}
		//System.out.println("********** GeoJSON fetch latency:"+(System.nanoTime()-startTime2)/1e6);

		stopTime = System.nanoTime();
		//Storing the ndn request-response time in millisecond
		stats.setNdnRequestTime((stopTime-startTime)/1e6);	

		//System.out.println(DateTime.currentTime()+"MapService - After ExecNDNRequest" + " - Time: " + (stopTime-startTime)/1e9 +"s\n");

		return new ArrayList<String>(dataResult.values());
	}

	private String fastOidExtractor(String tileContent) {
		String oid =  tileContent.split("oid")[1];

		//Handle the fast oid search for better performance don't use jsonobject parser
		boolean foundToken1 = false, foundToken2 = false, foundToken3 = false;
		char token = '"';
		String oidString = "";

		for (char c : oid.toCharArray()) {
			if (c == token && foundToken1 == false)
			{
				foundToken1 = true;
				token = ':';
				continue;
			}
			if (c == token && foundToken1 == true && foundToken2 == false)
			{
				foundToken2 = true;
				token = '"';
				continue;
			}
			if (c == token && foundToken1 == true && foundToken2 == true && foundToken3 == false)
			{
				foundToken3 = true;
				token = '"';

				continue;
			}
			if (c != token && foundToken1 == true && foundToken2 == true && foundToken3 == true)
			{
				oidString += c;
			}
			else
				break;
		}

		return oidString;
	}

	private ArrayList<String> postProcessingResult(ArrayList<String> tileData, RangeQueryParams queryParams) {
		//TODO 
		/*1) Remove additional data due tasselation mechanism 
		 *2) Filter the tileData with the queryParams requestedProperties
		 *
		 */

		switch (queryParams.queryFunction) {
		case "$geoIntersects": 
			return postProcessingResultGeoIntersects(tileData, queryParams);
		default:
			System.out.println(DateTime.currentTime()+"postProcessingResult - no queryFunction support...");
			return tileData;
		}
	}

	private ArrayList<String> postProcessingResultGeoIntersects(ArrayList<String> tileData, RangeQueryParams queryParams) {
		switch (queryParams.queryType) {
		case "Box": 
			return postProcessingResultGeoIntersectsBox(tileData, queryParams);
		default:
			System.out.println(DateTime.currentTime()+"postProcessingResultGeoIntersects - no queryType support...");
			return tileData;
		}
	}
	
	private ArrayList<String> postProcessingResultGeoIntersectsBox(ArrayList<String> tileData, RangeQueryParams queryParams) {
		
		
		
		
		
		ArrayList<String> result = new ArrayList<String> ();  
		
		double sw_lat = queryParams.coordinatesPoint.get(0).getLatitude();
		double sw_lon = queryParams.coordinatesPoint.get(0).getLongitude();
		double ne_lat = queryParams.coordinatesPoint.get(1).getLatitude();
		double ne_lon = queryParams.coordinatesPoint.get(1).getLongitude();
		SpatialReference sr = SpatialReference.create(4326);
		Envelope boxEnvelope = new Envelope(sw_lon,sw_lat,ne_lon,ne_lat);		
		
		for (String geoJSON: tileData) {
			//System.out.println("Tile Data:" + geoJSON);
			JSONObject geometryJSON = new JSONObject(geoJSON).getJSONObject("geometry");
			//String toParse = geometryJSON.toString().replace("[[","[").replace("]]","]");
			MapGeometry geometry1 = GeometryEngine.geometryFromGeoJson(geometryJSON.toString(), GeoJsonImportFlags.geoJsonImportDefaults,Geometry.Type.Unknown);
			boolean intersects = OperatorIntersects.local().execute(geometry1.getGeometry(), boxEnvelope, sr, null);
			if (intersects) {
				result.add(geoJSON);
			}
			
		}
			
		return result;
	}

	private double computeResponseSquareArea(ArrayList<Rectangle> list) {

		double resArea = 0.0;

		for (Rectangle rect : list) {
			resArea+=rect.computeArea()*100*100;
		}

		return Utils.floor10(resArea, 2);
	}
}

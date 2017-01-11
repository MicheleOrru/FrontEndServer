package com.ogb.fes.service;


import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import javax.servlet.http.HttpServletResponse;

import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.ogb.fes.domain.User;
import com.ogb.fes.domainRepositories.UserRepository;
import com.ogb.fes.entity.ErrorResponse;
import com.ogb.fes.entity.GPSRect;
import com.ogb.fes.entity.RangeQueryParams;
import com.ogb.fes.entity.ServiceStats;
import com.ogb.fes.ndn.NDNQueryManager;
import com.ogb.fes.net.NetManager;
import com.ogb.fes.utils.DateTime;
import com.ogb.fes.utils.GeoJSONQuery;
import com.ogb.fes.utils.Rectangle;
import com.ogb.fes.utils.Utils;

import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.GeoJsonImportFlags;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.MapGeometry;
import com.esri.core.geometry.OperatorIntersects;
import com.esri.core.geometry.SpatialReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;

@RestController
public class QueryService 
{
	@Autowired
	private UserRepository userRepo;

	static NDNQueryManager ndnQueryManager = NDNQueryManager.sharedManager();
	public static String   serverIP;   // Configured by FesApplication;


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
	@RequestMapping(method = RequestMethod.POST, value="/OGB/query-service/element/{cid}", produces="application/json")
	public void postElementQueryRequest(HttpServletResponse response, @RequestBody HashMap<String, Object> params, @PathVariable String cid, @RequestHeader(value="Authorization", defaultValue="") String authToken, @RequestHeader(value="Accept-Encoding", defaultValue="") String contentHeader) throws Exception {

		OutputStream out = response.getOutputStream();
		if (contentHeader != null && contentHeader.length() > 0 && contentHeader.contains("gzip")) {
			response.addHeader("Content-Encoding", "gzip");
			out = new GZIPOutputStream(response.getOutputStream());
		}
		
		ObjectMapper mapper = new ObjectMapper();
		
		//System.out.println("Received token: "+authToken);
		long startTime = System.currentTimeMillis();
		User user = checkAuthToken(authToken);
		if (user == null) {
			response.setStatus(420);
			
			String jsonInString = mapper.writeValueAsString(new ErrorResponse(420, "Invalid authorization token").toString());
			out.write(jsonInString.getBytes());
		}

		String oid = (String)params.get("oid");
		if (oid == null || oid.length() <= 0) {
			response.setStatus(431);
			
			String jsonInString = mapper.writeValueAsString(new ErrorResponse(431, "Invalid oid in GeoJSON!").toString());
			out.write(jsonInString.getBytes());
		}

		ServiceStats    stats     = new ServiceStats();
		HashSet<String> queryName = new HashSet<String>();
		queryName.add(oid);
		//System.out.println("Query Name: " + oid);

		ndnQueryManager.execNdnDataRequest(queryName, serverIP);
		ndnQueryManager.joinAll();
		ArrayList<String> tileData = ndnQueryManager.popAllResults();

		stats.setRequestTimeMillis(System.currentTimeMillis()-startTime);
		//System.out.println("STATS: " + stats.toString());
		
		byte resp[] = tileData.toString().getBytes();
		out.write(resp, 0, resp.length);
		out.flush();
		out.close();
	}


	@CrossOrigin(origins="*")
	@RequestMapping(method=RequestMethod.POST, value="/OGB/query-service/{cid}", consumes={"application/json"}, produces={"application/json"})
	public void postRangeQueryRequest(HttpServletResponse response, @RequestBody HashMap<String, Object> params, @PathVariable String cid, @RequestHeader(value="Authorization", defaultValue="") String authToken, @RequestHeader(value="Accept-Encoding", defaultValue="") String contentHeader) throws Exception {
		
		OutputStream out = response.getOutputStream();
		if (contentHeader != null && contentHeader.length() > 0 && contentHeader.contains("gzip")) {
			response.addHeader("Content-Encoding", "gzip");
			out = new GZIPOutputStream(response.getOutputStream());
		}
		
		ObjectMapper mapper = new ObjectMapper();
		
		//System.out.println("Received token: "+authToken);
		long startTime = System.currentTimeMillis();
		User user = checkAuthToken(authToken);
		if (user == null) {
			response.setStatus(420);

			String jsonInString = mapper.writeValueAsString(new ErrorResponse(420, "Invalid authorization token").toString());
			out.write(jsonInString.getBytes());
			return;
		}
		String tid = user.getUserID().split("/")[0];
		String uid = user.getUserID().split("/")[1];

		RangeQueryParams queryParams = new RangeQueryParams();
		if (queryParams.setParams(params,tid,uid,cid) == false) {
			response.setStatus(430);
			
			String jsonInString = mapper.writeValueAsString(new ErrorResponse(430, "Invalid query params").toString());
			out.write(jsonInString.getBytes());
			return;
		}
		
		double area = 0.0;
		double AREALIMIT = 50*50*100*100;
//		System.out.println("\nQuery params: " + params);
		JSONObject obj = new JSONObject(new ObjectMapper().writeValueAsString(params));	
		String type = obj.optJSONObject("geometry").optJSONObject("$geoIntersects").optJSONObject("$geometry").optString("type");
		if(type.equalsIgnoreCase("box"))
			area = new GPSRect(queryParams.coordinatesBoxPoint.get(0), queryParams.coordinatesBoxPoint.get(1)).computeArea()*100*100;
		
		else
		{
			GeoJSONQuery    geoJSONQuery = new GeoJSONQuery(params, tid, uid, cid);
			area = geoJSONQuery.computeArea();
		}
		System.out.println("requested area size: "+area+" km^2");
		if(area >= AREALIMIT)
		{
			response.setStatus(440);
			String jsonInString = mapper.writeValueAsString(new ErrorResponse(440, "Requested area size exceeds the maximum limit").toString());
			out.write(jsonInString.getBytes());
			return;
		}

		ServiceStats    stats     = new ServiceStats();
		HashSet<String> tileNames = queryParams.tessellation(stats);
		
		//Filter tile names with the bloom filter service
		//tileNames = bfFilterService(tileNames, stats);

		//Resolve ndn query and filter the resultSet according to queryParams to reduce overdata response
		ArrayList<String> tileData = getTileData(tileNames, stats);
		long startTimePost = System.currentTimeMillis();
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
		
		byte resp[] = tileData.toString().getBytes();
		out.write(resp, 0, resp.length);
		out.flush();
		out.close();
	}


	//Mongo Api
	@CrossOrigin(origins="*")
	@RequestMapping(method = RequestMethod.POST, value="/OGB/mongo-query-service/element/{cid}", produces="application/json")
	public @ResponseBody Object postMongoElementQueryRequest(HttpServletResponse response, @RequestBody HashMap<String, Object> params, @PathVariable String cid, @RequestHeader(value="Authorization", defaultValue="") String authToken) throws Exception {

		//System.out.println("Received token: "+authToken);
		long startTime = System.currentTimeMillis();
		User user = checkAuthToken(authToken);
		if (user == null) {
			response.setStatus(420);
			return new ErrorResponse(420, "Invalid authorization token");
		}

		String oid = (String)params.get("oid");
		if (oid == null || oid.length() <= 0) {
			response.setStatus(431);
			return new ErrorResponse(431, "Invalid oid in GeoJSON!");
		}

		ServiceStats    stats     = new ServiceStats();
		HashSet<String> queryName = new HashSet<String>();
		queryName.add(oid);
		//System.out.println("Query Name: " + oid);

		ndnQueryManager.execNdnDataRequest(queryName, serverIP);
		ndnQueryManager.joinAll();
		ArrayList<String> tileData = ndnQueryManager.popAllResults();

		stats.setRequestTimeMillis(System.currentTimeMillis()-startTime);
		System.out.println("STATS: " + stats.toString());
		return tileData.toString();
	}

	@CrossOrigin(origins="*")
	@RequestMapping(method=RequestMethod.POST, value="/OGB/mongo-query-service/{cid}", consumes={"application/json"}, produces={"application/json"})
	public @ResponseBody Object postMongoRangeQueryRequest(HttpServletResponse response, @RequestBody HashMap<String, Object> params, @PathVariable String cid, @RequestHeader(value="Authorization", defaultValue="") String authToken) throws Exception {

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
		//HashSet<String> tileNames = queryParams.tessellation(stats);

		ArrayList<String> result = mongoRangeQuery(params);

		//Filter tile names with the bloom filter service
		//tileNames = bfFilterService(tileNames, stats);

		//Resolve ndn query and filter the resultSet according to queryParams to reduce overdata response
		//ArrayList<String> tileData = getTileData(tileNames, stats);
		long startTimePost=System.currentTimeMillis();
		//tileData = postProcessingResult(tileData, queryParams);
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
		//return tileData.toString();
		return result;
	}



	@SuppressWarnings("unchecked")
	private ArrayList<String> mongoRangeQuery(HashMap<String, Object> params) {

		MongoClientURI uri         = new MongoClientURI("mongodb://bv:1234@192.168.69.1/?authSource=spatial");
		MongoClient    mongoClient = new MongoClient(uri);


		try{

			MongoDatabase             database  = mongoClient.getDatabase("spatial");
			MongoCollection<Document> coll      = database.getCollection("bv");

	
			HashMap<String, Object> geomQuery 	= (HashMap<String, Object>) params.get("geometry");
			HashMap<String, Object> geomType  	= (HashMap<String, Object>) geomQuery.get("$geoIntersects");
			HashMap<String, Object> geom 	  	= (HashMap<String, Object>) geomType.get("$geometry");
			String 					type 	  	= (String) geom.get("type");
			ArrayList<ArrayList<double[]>> coordinates = (ArrayList<ArrayList<double[]>>) geom.get("coordinates");

			BasicDBObject geometryValue = new BasicDBObject("type", type); //new BasicDBObject("type","Box");
			geometryValue.append("coordinates", coordinates);
			BasicDBObject geometry  = new BasicDBObject("$geometry", geometryValue);
			BasicDBObject queryType = new BasicDBObject("$geoIntersects", geometry);
			BasicDBObject query     = new BasicDBObject("geometry.coordinates", queryType);

			//			System.out.println(query);

			System.out.println("Number of elements "+ coll.count(query));
			FindIterable<Document> fi     = coll.find(query);
			MongoCursor<Document>  cursor = fi.iterator();

			ArrayList<String> result = new ArrayList<String>();
			while (cursor.hasNext()) { 
				result.add(cursor.next().toJson());
			}

			return result;
		}
		catch(Exception e){
			System.err.println( e.getClass().getName() + ": " + e.getMessage() );

			return new ArrayList<String>();
		}
		finally {
			mongoClient.close();
		}
	}



	@SuppressWarnings("unused")
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
		/* //TODO
		 * 1) Remove additional data due tasselation mechanism 
		 * 2) Filter the tileData with the queryParams requestedProperties
		 */

		switch (queryParams.queryFunction) {
			case "$geoIntersects": 
				return postProcessingResultGeoIntersects(tileData, queryParams);
	
			default:
				System.out.println(DateTime.currentTime()+"QueryService - postProcessingResult - no queryFunction support...");
				return tileData;
		}
	}

	private ArrayList<String> postProcessingResultGeoIntersects(ArrayList<String> tileData, RangeQueryParams queryParams) {
		switch (queryParams.queryType) {

			case "Box": 
				return postProcessingResultGeoIntersectsBox(tileData, queryParams);
			case "Polygon": 
				return postProcessingResultGeoIntersectsPolygon(tileData, queryParams);
			default:
				System.out.println(DateTime.currentTime()+"QueryService - postProcessingResultGeoIntersects - no queryType support...");
				return tileData;
		}
	}

	private ArrayList<String> postProcessingResultGeoIntersectsPolygon(ArrayList<String> tileData, RangeQueryParams queryParams) {
		return tileData;		
	}
	
	private ArrayList<String> postProcessingResultGeoIntersectsBox(ArrayList<String> tileData, RangeQueryParams queryParams) {

		ArrayList<String> result = new ArrayList<String> ();  

		double sw_lat = queryParams.coordinatesBoxPoint.get(0).getLatitude();
		double sw_lon = queryParams.coordinatesBoxPoint.get(0).getLongitude();
		double ne_lat = queryParams.coordinatesBoxPoint.get(1).getLatitude();
		double ne_lon = queryParams.coordinatesBoxPoint.get(1).getLongitude();
		
		SpatialReference sr = SpatialReference.create(4326);
		Envelope boxEnvelope = new Envelope(sw_lon,sw_lat,ne_lon,ne_lat);		
		double obj_lat;
		double obj_lon;
		boolean intersects;
		
		for (String geoJSON: tileData) {
			//System.out.println("Tile Data:" + geoJSON);
			JSONObject geometryJSON = new JSONObject(geoJSON).getJSONObject("geometry");
			String type = geometryJSON.optString("type");
			intersects  = false;
			
			if (type.equals("MultiPoint"))
			{
				JSONArray jsonArray = geometryJSON.optJSONArray("coordinates");
				for (int i = 0; i < jsonArray.length(); i++) {
					JSONArray coordinate = jsonArray.getJSONArray(i);
					obj_lat = coordinate.getDouble(1);
					obj_lon = coordinate.getDouble(0);
					//System.out.println("obj_lat:"+obj_lat+" obj_lon:"+obj_lon+" sw_lat:"+sw_lat+" sw_lon:"+sw_lon+" ne_lat:"+ne_lat+" ne_lon:"+ne_lon);
					if (obj_lat>=sw_lat && obj_lat<=ne_lat && obj_lon>=sw_lon && obj_lon<=ne_lon) {
						//System.out.println("obj_lat:"+obj_lat+" obj_lon:"+obj_lon);
						intersects=true;
						break;
					}
				}

			} 
			else if (type.equals("Point")) {
				JSONArray coordinate = geometryJSON.optJSONArray("coordinates");
				obj_lat = coordinate.getDouble(1);
				obj_lon = coordinate.getDouble(0);
				if (obj_lat>=sw_lat && obj_lat<=ne_lat && obj_lon>=sw_lon && obj_lon<=ne_lon) {
					intersects=true;
				}
			} 
			else {				
				MapGeometry geometry1 = GeometryEngine.geometryFromGeoJson(geometryJSON.toString(), GeoJsonImportFlags.geoJsonImportDefaults,Geometry.Type.Unknown);
				intersects = OperatorIntersects.local().execute(geometry1.getGeometry(), boxEnvelope, sr, null);
			}
			
			if (intersects) {
				result.add(geoJSON);
			}
		}

		return result;
	}

	@SuppressWarnings("unused")
	private double computeResponseSquareArea(ArrayList<Rectangle> list) {

		double resArea = 0.0;

		for (Rectangle rect : list) {
			resArea+=rect.computeArea()*100*100;
		}

		return Utils.floor10(resArea, 2);
	}
}

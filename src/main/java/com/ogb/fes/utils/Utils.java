package com.ogb.fes.utils;


import java.math.BigDecimal;
import java.util.Random;
import java.util.regex.Pattern;

import com.ogb.fes.entity.GPSPoint;
import com.ogb.fes.entity.GPSRect;


public class Utils {
	public enum Format {
		LAT_LONG,
		LONG_LAT
	}
	
	//NDN Utilities functions
	public static String[] toStringsForNDNname(double number) {
		String res;
		if (number < 0.0f)
			res = String.format("%07.2f", number);
		else
			res = String.format("%06.2f", number);
		String result[] = new String[3];
		
		String parts[] = res.split(Pattern.quote("."));
		if (parts.length < 2) {
			parts = res.split(",");
			if (parts.length < 2) {
				System.out.println("Error!");
				parts[0] = ""+number;
				parts[1] = "00";	
			}
		}
		
		result[0] = parts[0];
		result[1] = parts[1].toCharArray()[0] +"";
		result[2] = parts[1].toCharArray()[1] +"";

		return result;
	}
	
	public static String generateNonce(int lenght) {
		char[] charset = "1234567890qwertyuiopasdfghjklzxcvbnm".toCharArray();
		
		String res = "";
		for (int i = 0; i < lenght; i++) {
			res += charset[new Random().nextInt(lenght)];
		}
		
		return res;
	}
	
	public static String gpsRectToNDNName(GPSRect rect, int precision) {
		return gpsRectToNDNName(rect, precision, Format.LONG_LAT);
	}
	
	public static String gpsRectToNDNName(GPSRect rect, int precision, Utils.Format format) {
		GPSPoint point = rect.southWest.clone();
		boolean needSign_lat = false;
		boolean needSign_lng = false;
		
		if (point.latitude < 0.0) {
			point.latitude = rect.northEst.latitude;
			needSign_lat = (point.latitude >= 0.0 ? true : false);
		}
		if (point.longitude < 0.0) {
			point.longitude = rect.northEst.longitude;
			needSign_lng = (point.longitude >= 0.0 ? true : false);
		}
		
	//	System.out.println("gpsRectToNDNName: point = " + point);
		
		return Utils.gpsPointToNDNNname(point, needSign_lat, needSign_lng, precision, format);
	}
	
	public static String gpsPointToNDNNname(GPSPoint point,  int precision, Format format) {
		return gpsPointToNDNNname(point, false, false, precision, format);
	}
	
	public static String gpsPointToNDNNname(GPSPoint point, boolean needSign_lat, boolean needSign_lng,  int precision) {
		return gpsPointToNDNNname(point, needSign_lat, needSign_lng, precision, Format.LONG_LAT);
	}
	public static String gpsPointToNDNNname(GPSPoint point, boolean needSign_lat, boolean needSign_lng, int precision, Utils.Format format) {
		String latitudeString[]  = Utils.toStringsForNDNname(point.getLatitude());
		String longitudeString[] = Utils.toStringsForNDNname(point.getLongitude());
		
		String result = "";
		
		if (format == Format.LAT_LONG)
			result +=  "/" + (needSign_lat ? "-" : "") + latitudeString[0] + "/" + (needSign_lng ? "-" : "") + longitudeString[0];
		else
			result +=  "/" + (needSign_lng ? "-" : "") + longitudeString[0] + "/" + (needSign_lat ? "-" : "") + latitudeString[0];
		
		for (int i = 1; i <= precision; i++) {
			if (format == Format.LAT_LONG)
				result += "/" + latitudeString[i] + longitudeString[i];
			else
				result += "/" + longitudeString[i] + latitudeString[i];
		}
		
		return result;
	}
	
//	public static String referredNDNNameForGeoJSON(GeoJSONContainer geoJSON) {
//		GPSPoint point  = geoJSON.getCoordinates().get(0);
//		int zoomLevel   = 0;
//		String tenantID = geoJSON.getTenantID();
//		String userID   = geoJSON.getUserID();
//		String objID    = geoJSON.getNonce();
//		String cID 		= geoJSON.getCollectionID();
//		
//		String prefix = Utils.gpsPointToNDNNname(point, zoomLevel, Utils.Format.LONG_LAT);
//		String res = "/OGB"+prefix+"/GPS_id/DATA/"+tenantID+"/"+cID+"/"+userID+"/"+objID;
//		
//		return res;
//	}	
	
	
	public static Rectangle rectFromNDNName(String ndnName) {
		return rectFromNDNName(ndnName, Format.LONG_LAT);
	}
	public static Rectangle rectFromNDNName(String ndnName, Utils.Format format) {
		ndnName = ndnName.split("/GPS_id/DATA/")[0];
		ndnName = ndnName.replace("/OGB/", "");
		
		String[] parts = ndnName.split("/");
		
		String latitudeString  = "";
		String longitudeString = "";
		
		if (format == Format.LAT_LONG) {
			latitudeString = parts[0];
			longitudeString = parts[1];
		}
		else {
			latitudeString = parts[1];
			longitudeString = parts[0];
		}
		

		if (parts.length > 2) {
			if (latitudeString.toCharArray()[latitudeString.length()-1] != '.')
				latitudeString = latitudeString + ".";
			if (longitudeString.toCharArray()[longitudeString.length()-1] != '.')
				longitudeString = longitudeString + ".";
		}

		for (int i = 2; i < parts.length; i++) {
			if (format == Format.LAT_LONG) {
				latitudeString += parts[i].toCharArray()[0];
				longitudeString += parts[i].toCharArray()[1];
			}
			else {
				latitudeString  += parts[i].toCharArray()[1];
				longitudeString += parts[i].toCharArray()[0];
			}
		}
		
		double size = Utils.floor10(1.0/Math.pow(10, parts.length-2), 2);
		
		Rectangle rect = new Rectangle(Utils.floor10(Double.parseDouble(latitudeString), 2), Utils.floor10(Double.parseDouble(longitudeString), 2), size, size);
		
		if (rect.x < 0)
			rect.x -= rect.width;
		if (rect.y < 0)
			rect.y -= rect.height;
		return rect;
	}
	
	
	//--------------Math-utilities-functions----------------//
	static Random rand = new Random(21342141);
	public static double randomBetween(double min, double max) {
		//Check that min is realy the min value
		double temp = 0;
		if (max < min) {
			temp = min;
			min  = max;
			max  = temp;
		}
		
	    double randomNum = rand.nextDouble();
	    randomNum = randomNum*(max - min) + min;
	    
	    return randomNum;
	}
	
	public static double floor10(double number, int decimal) {
		String stringNum = "";
		
		if (number >= 0) {
			number += 0.000001; //For fixing the double precision error 
			stringNum = new BigDecimal(String.valueOf(number)).setScale(decimal, BigDecimal.ROUND_FLOOR).toString();
		}
		else {
			number += 0.000001; //For fixing the double precision error 
			stringNum = new BigDecimal(String.valueOf(number)).setScale(decimal, BigDecimal.ROUND_FLOOR).toString();
		}
		//System.out.println("Floor To Decimal: " + decimal + "   " + number + " --> " + Double.parseDouble(stringNum));
		return Double.parseDouble(stringNum);
	} 

    public static double ceil10(double number, int decimal) {	
    	String stringNum = "";
		
    	if (number >= 0) {
    		number -= 0.000001; //For fixing the double precision error 
    		stringNum = new BigDecimal(String.valueOf(number)).setScale(decimal, BigDecimal.ROUND_CEILING).toString();
    	}
    	else{
    		number -= 0.000001; //For fixing the double precision error 
    		stringNum = new BigDecimal(String.valueOf(number)).setScale(decimal, BigDecimal.ROUND_CEILING).toString();
    	}
    	//System.out.println("Ceil To Decimal: " + decimal + "   " + number + " --> " + Double.parseDouble(stringNum));
		return Double.parseDouble(stringNum);
	} 
    
    
    //Rectangle utilities functions
	public static boolean valueInRange(double value, double min, double max) {
		return (value > min) && (value < max);
	}
	
    public static boolean intersects(Rectangle A, Rectangle B) {
		boolean xOverlap  = valueInRange(A.x, B.x, B.x + B.width) || valueInRange(B.x, A.x, A.x + A.width);
		boolean x2Overlap = valueInRange(A.x+A.width, B.x, B.x + B.width) || valueInRange(B.x+B.height, A.x, A.x + A.width);
		
		boolean yOverlap  = valueInRange(A.y, B.y, B.y + B.height) || valueInRange(B.y, A.y, A.y + A.height);
		boolean y2Overlap = valueInRange(A.y+A.width, B.y, B.y + B.height) || valueInRange(B.y+B.height, A.y, A.y + A.height);
		
		return (xOverlap || x2Overlap) && (yOverlap || y2Overlap);
    }
}


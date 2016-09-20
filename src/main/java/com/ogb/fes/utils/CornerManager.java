package com.ogb.fes.utils;


public class CornerManager {
	public enum Corner {
		Top,  
		Right,           
		Down,              
		Left;
	}
	

	public static boolean isCornerPresent(int cornerMask, Corner corner) {
		int  tmpCorner = cornerMask;
		long mask      = (0x1L << corner.ordinal());
		tmpCorner &= mask;
	    return (tmpCorner != 0);
	}
	
	public static boolean cornerMaskContainsCorner(int cornerMask, Corner corner){
		int  tmpCorner = cornerMask;
		long mask      = (0x1L << corner.ordinal());
		tmpCorner &= mask;
	    return (tmpCorner != 0);
	}
	
	public static String cornerToString(Corner corner) {
		if (corner == Corner.Top)
			return "Top";
		if (corner == Corner.Right)
			return "Right";
		if (corner == Corner.Down)
			return "Down";
		if (corner == Corner.Left)
			return "Left";
		
		return "None";	
	}
	public static String cornersMaskToString(int cornerMask) {
		String res = "";
		
		if (isCornerPresent(cornerMask, Corner.Top))
			res += "Top";
		if (isCornerPresent(cornerMask, Corner.Right))
			res += "Right";
		if (isCornerPresent(cornerMask, Corner.Down))
			res += "Down";
		if (isCornerPresent(cornerMask, Corner.Left))
			res += "Left";
		
		return (res.length() == 0 ? "None" : res);
	}
}

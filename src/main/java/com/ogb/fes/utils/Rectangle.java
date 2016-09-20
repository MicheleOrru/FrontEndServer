package com.ogb.fes.utils;


import java.util.Comparator;

import com.ogb.fes.entity.GPSPoint;
import com.ogb.fes.entity.GPSRect;


public class Rectangle {
	public double x;
	public double y;
	public double width;
	public double height;
	
	public double coverPercentage;
	public double cover;
	
	
	public Rectangle(){
		x      = -Double.MAX_VALUE;
		y      = -Double.MAX_VALUE;
		width  = -Double.MAX_VALUE;
		height = -Double.MAX_VALUE;
	}
	
	public Rectangle(GPSRect gpsRect) {
		super();
		
		this.x      = gpsRect.southWest.latitude;
		this.y      = gpsRect.southWest.longitude;
		this.width  = gpsRect.northEst.latitude-gpsRect.southWest.latitude;
		this.height = gpsRect.northEst.longitude-gpsRect.southWest.longitude;
		
		adjustWidthAndHeight();
	}
	
	public Rectangle(double x, double y, int zoomLevel) {
		super();
		
		this.x      = x;
		this.y      = y;
		
		if (zoomLevel == 0) {
			this.width  = 1.0;
			this.height = 1.0;
		}
		else if (zoomLevel == 1) {
			this.width  = 0.1;
			this.height = 0.1;
		}
		else if (zoomLevel == 2) {
			this.width  = 0.01;
			this.height = 0.01;
		}
		
		adjustWidthAndHeight();
	}
	
	public Rectangle(GPSPoint point, int zoomLevel) {
		this(Utils.floor10(point.latitude, zoomLevel), Utils.floor10(point.longitude, zoomLevel), zoomLevel);
		
		adjustWidthAndHeight();
	}
	
	public Rectangle(double x, double y, double width, double height) {
		super();
		
		this.x      = x;
		this.y      = y;
		this.width  = width;
		this.height = height;
		
		adjustWidthAndHeight();
	}

	public void adjustWidthAndHeight() {
		if (this.width >= 0.9 && this.width <= 1.1)
			this.width = 1.0;
		if (this.width >= 0.09 && this.width <= 0.11)
			this.width = 0.1;
		if (this.width >= 0.009 && this.width <= 0.011)
			this.width = 0.01;
		
		if (this.height >= 0.9 && this.height <= 1.1)
			this.height = 1.0;
		if (this.height >= 0.09 && this.height <= 0.11)
			this.height = 0.1;
		if (this.height >= 0.009 && this.height <= 0.011)
			this.height = 0.01;
	}
	
	
	public double getX() {
		return x;
	}
	public double getY() {
		return y;
	}
	public double getWidth() {
		return width;
	}
	public double getHeight() {
		return height;
	}

	
	public void setX(double x) {
		this.x = x;
	}
	public void setY(double y) {
		this.y = y;
	}
	public void setWidth(double width) {
		this.width = width;
	}
	public void setHeight(double height) {
		this.height = height;
	}
	
	
	public int computeResolution() {
		adjustWidthAndHeight();
		
		int resolution = -(int) Math.log10(width);
		return resolution;
	}
	
	public double computeArea() {
		return Math.abs(width*height);
	}
	
	
	public double computeIntersectionAreaPercentage(Rectangle other) {
		double area = (double)(width*height);
		
		coverPercentage = computeIntersectionArea(other)/area;
		return coverPercentage;
	}
	public double computeIntersectionArea(Rectangle other) {
		double iLeft   = Math.max((x*100),          (other.x*100));
		double iRight  = Math.min(((x+width)*100),  ((other.x+other.width)*100));
		double iTop    = Math.min(((y+height)*100), ((other.y+other.height)*100));
		double iBottom = Math.max((y*100),          (other.y*100));

		cover = Math.max(0, iRight-iLeft) * Math.max(0, iTop-iBottom);
		return cover/10000;
	}
	
	
	public GPSRect toGPSRect() {
		int level = -(int)Math.log10(width);

		GPSPoint pointNE = new GPSPoint(Utils.floor10(x,level)+Utils.floor10(width, level), Utils.floor10(y,level)+Utils.floor10(height, level));
		GPSPoint pointSW = new GPSPoint(x, y);
		GPSRect  rect    = new GPSRect(pointNE, pointSW);
		rect.adjustCoordToDecimalNumber(level);
		
		return rect;
	}
	
	@Override
	public String toString() {
		return "{X: " + x + " Y: " + y + " W: " + width + " H: "+ height +"}\n";
		//return "{X: " + x + " Y: " + y + " W: " + width + " H: "+ height + " Cover: " + cover + "}";
	}
	
	
	@Override
	protected Object clone() throws CloneNotSupportedException {
		return new Rectangle(x, y, width, height);
	}
	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(height);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(width);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(x);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(y);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Rectangle other = (Rectangle) obj;
		if (Double.doubleToLongBits(height) != Double.doubleToLongBits(other.height))
			return false;
		if (Double.doubleToLongBits(width) != Double.doubleToLongBits(other.width))
			return false;
		if (Double.doubleToLongBits(x) != Double.doubleToLongBits(other.x))
			return false;
		if (Double.doubleToLongBits(y) != Double.doubleToLongBits(other.y))
			return false;
		return true;
	}
	
	public static Comparator<Rectangle> comparator(){
		return new Comparator<Rectangle>() {
			@Override
			public int compare(Rectangle o1, Rectangle o2) {
				if (o1.cover < o2.cover)
					return 1;
				if (o1.cover > o2.cover)
					return -1;
				return 0;
			}
		};
	}
}
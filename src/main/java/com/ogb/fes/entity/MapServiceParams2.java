package com.ogb.fes.entity;


import java.util.ArrayList;

import com.ogb.fes.utils.Utils.Format;


public class MapServiceParams2 
{
	//TODO  aggiornare naming
	GPSRect           coordinates; 
    ArrayList<String> options;
    String            tenantId;
    String            command;
    String            dataType;
    int               resolution;
    int               maxTiles;
    Format            format;
    
    
    public MapServiceParams2() 
    {
    	coordinates = null;
    	options     = new ArrayList<String>();
    	resolution  = -Integer.MAX_VALUE;
    	maxTiles    = -Integer.MAX_VALUE;
	}
    
	
	public GPSRect getCoordinates() {
		return coordinates;
	}
	public ArrayList<String> getOptions() {
		return options;
	}
	public String getTenantId() {
		return tenantId;
	}
	public String getCommand() {
		return command;
	}
	public String getDataType() {
		return dataType;
	}
	public int getResolution() {
		return resolution;
	}
	public int getMaxTiles() {
		return maxTiles;
	}
	public Format getFormat() {
		return format;
	}

	public void setCoordinates(GPSRect coordinates) {
		this.coordinates = coordinates;
	}
	public void setOptions(ArrayList<String> options) {
		this.options = options;
	}
	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}
	public void setCommand(String command) {
		this.command = command;
	}
	public void setDataType(String dataType) {
		this.dataType = dataType;
	}
	public void setResolution(int resolution) {
		this.resolution = resolution;
	}
	public void setMaxTiles(int maxTiles) {
		this.maxTiles = maxTiles;
	}
	public void setFormat(Format format) {
		this.format = format;
	}


	@Override
	public String toString() {
		return "MapParams [coordinates=" + coordinates + ", options=" + options + ", resolution=" + resolution + ", threshold=" + maxTiles+"]";
	}
}

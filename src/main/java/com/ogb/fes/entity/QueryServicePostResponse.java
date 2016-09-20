package com.ogb.fes.entity;


import java.util.ArrayList;

import com.ogb.fes.filesystem.FilePair;


public class QueryServicePostResponse {
	ArrayList<GPSRect>   tilesData;
	ArrayList<GPSRect>   tilesEmpty;
	ArrayList<FilePair>  fileList;
	ServiceStats         stats;
	
	
	//Constructor
	public QueryServicePostResponse() {
		tilesEmpty = new ArrayList<GPSRect>();
		tilesData  = new ArrayList<GPSRect>();
		fileList   = new ArrayList<FilePair>();
		stats      = new ServiceStats();
	}


	//Getter Methods
	public ArrayList<GPSRect> getTilesData() {
		return tilesData;
	}
	public ArrayList<GPSRect> getTilesEmpty() {
		return tilesEmpty;
	}
	public ArrayList<FilePair> getFileList() {
		return fileList;
	}
	public ServiceStats getStats() {
		return stats;
	}
	
	
	//Setter Methods
	public void setTilesData(ArrayList<GPSRect> tilesData) {
		this.tilesData = tilesData;
	}
	public void setTilesEmpty(ArrayList<GPSRect> tilesEmpty) {
		this.tilesEmpty = tilesEmpty;
	}
	public void setFileList(ArrayList<FilePair> list) {
		this.fileList = list;
	}	
	public void setStats(ServiceStats stats) {
		this.stats = stats;
	}
}

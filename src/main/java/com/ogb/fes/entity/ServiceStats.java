package com.ogb.fes.entity;

import com.ogb.fes.utils.Utils;


public class ServiceStats {
	double tilesComputedTime;
	double ndnRequestTime;
	double bloomFilterRequestTime;
	
	int tilesCount;
	int tilesWithDataCount;
	
	double requestArea;
	double responseArea;
	
	double requestTimeMillis;
	
	double postProcessingTime;
		
	

	//Constructor
	public ServiceStats() {
		tilesComputedTime      = 0;
		ndnRequestTime         = 0;
		bloomFilterRequestTime = 0;
		
		tilesCount         = 0;
		tilesWithDataCount = 0;
		
		requestArea  = 0;
		responseArea = 0;
	}


	public double getTilesComputedTime() {
		return tilesComputedTime;
	}
	public double getNdnRequestTime() {
		return ndnRequestTime;
	}
	public double getBloomFilterRequestTime() {
		return bloomFilterRequestTime;
	}
	public int getTilesCount() {
		return tilesCount;
	}
	public int getTilesWithDataCount() {
		return tilesWithDataCount;
	}
	public double getRequestArea() {
		return requestArea;
	}
	public double getResponseArea() {
		return responseArea;
	}


	public void setTilesComputedTime(double tilesComputedTime) {
		this.tilesComputedTime = Utils.floor10(tilesComputedTime, 0);
	}
	public void setNdnRequestTime(double ndnRequestTime) {
		this.ndnRequestTime = Utils.floor10(ndnRequestTime, 0);
	}
	public void setBloomFilterRequestTime(double bloomFilterRequestTime) {
		this.bloomFilterRequestTime = Utils.floor10(bloomFilterRequestTime, 0);
	}
	public void setTilesCount(int tilesCount) {
		this.tilesCount = tilesCount;
	}
	public void setTilesWithDataCount(int tilesWithDataCount) {
		this.tilesWithDataCount = tilesWithDataCount;
	}
	public void setRequestArea(double requestArea) {
		this.requestArea = Utils.floor10(requestArea, 2);
	}
	public void setResponseArea(double responseArea) {
		this.responseArea = Utils.floor10(responseArea, 2);
	}
	
	public double getRequestTimeMillis() {
		return requestTimeMillis;
	}


	public void setRequestTimeMillis(double requestTimeMillis) {
		this.requestTimeMillis = requestTimeMillis;
	}
	
	public double getPostProcessingTime() {
		return postProcessingTime;
	}


	public void setPostProcessingTime(double postProcessingTime) {
		this.postProcessingTime = postProcessingTime;
	}



	@Override
	public String toString() {
		return "ServiceStats [requestTime=" + requestTimeMillis + ", postProcessingTime=" + postProcessingTime + ", tilesComputedTime=" + tilesComputedTime + ", ndnRequestTime=" + ndnRequestTime
				+ ", bloomFilterRequestTime=" + bloomFilterRequestTime + ", tilesCount=" + tilesCount
				+ ", tilesWithDataCount=" + tilesWithDataCount + ", requestArea=" + requestArea + ", responseArea="
				+ responseArea + "]";
	}

	
	
}

package com.ogb.fes.filesystem;


public class FilePair {
	String ndnName;
	String httpUrl;
	String fileName;
	
	
	public FilePair() {
		super();
		
		ndnName  = "";
		httpUrl  = "";
		fileName = "";
	}
	public FilePair(String ndnName, String httpUrl, String fileName) {
		this();
		
		this.ndnName  = ndnName;
		this.httpUrl  = httpUrl;
		this.fileName = fileName;
	}


	public String getNdnName() {
		return ndnName;
	}
	public String getHttpUrl() {
		return httpUrl;
	}
	public String getFileName() {
		return fileName;
	}
	

	public void setNdnName(String ndnName) {
		this.ndnName = ndnName;
	}
	public void setHttpUrl(String httpUrl) {
		this.httpUrl = httpUrl;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((httpUrl == null)  ? 0 : httpUrl.hashCode());
		result = prime * result + ((ndnName == null)  ? 0 : ndnName.hashCode());
		result = prime * result + ((fileName == null) ? 0 : fileName.hashCode());
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
		FilePair other = (FilePair) obj;
		if (httpUrl == null) {
			if (other.httpUrl != null)
				return false;
		} else if (!httpUrl.equals(other.httpUrl))
			return false;
		if (ndnName == null) {
			if (other.ndnName != null)
				return false;
		} else if (!ndnName.equals(other.ndnName))
			return false;
		if (fileName == null) {
			if (other.fileName != null)
				return false;
		} else if (!fileName.equals(other.fileName))
			return false;
		return true;
	}
}
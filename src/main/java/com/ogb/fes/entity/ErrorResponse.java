package com.ogb.fes.entity;


public class ErrorResponse {
	int    code;
	String message;
	Object data;
	
	
	public ErrorResponse() {
		code    = 400;
		message = "Generic Error";
	}
	
	public ErrorResponse(int code, String message )
	{
		this.code   = code;
		this.message = message;
	}
	public ErrorResponse(int code, String message, Object data)
	{
		this.code    = code;
		this.message = message;
		this.data    = data;
	}
	
	public int getCode() {
		return code;
	}
	public String getMessage() {
		return message;
	}
	public Object getData() {
		return data;
	}

	
	public void setMessage(String message) {
		this.message = message;
	}
    public void setCode(int code) {
		this.code = code;
	}
    public void setData(Object data) {
		this.data = data;
	}
}

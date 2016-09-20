package com.ogb.fes.ndn;


import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import com.ogb.fes.entity.RangeQueryParams;
import com.ogb.fes.utils.DateTime;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.util.Blob;
import net.named_data.jndn.util.SegmentFetcher;
import net.named_data.jndn.util.SegmentFetcher.ErrorCode;


public class NDNQueryTileResolver extends Thread implements NDNResolver, SegmentFetcher.OnComplete, SegmentFetcher.OnError {
	private HashSet<String> contentBuffers;
	private HashSet<String> ndnRequestNames;
	
	private int  resultCount;
	private int  requestTimeout;
	private Face face;
	
	private int     interestSended;
	private int     inFlightFetcher;
	private boolean eventProcessed;
	private int     INTEREST_WINDOW;
	private String serverIP;
	
	private long startTime = 0;
	private long stopTime  = 0;
	
	
	//Constructor
	public NDNQueryTileResolver(HashSet<String> requestedNames, String serverIP) {
		this.resultCount     = 0;
		this.requestTimeout  = 4000;
		this.face            = new Face(serverIP);
		this.contentBuffers  = new HashSet<String>();
		this.ndnRequestNames = requestedNames;
		this.serverIP = serverIP;
		this.interestSended    = 0;
		this.inFlightFetcher   = 0;
		this.eventProcessed    = false;
		this.INTEREST_WINDOW   = (RangeQueryParams.MAX_TILES>32 ? 32 : RangeQueryParams.MAX_TILES);
	}

	
	@Override
	public void onComplete(Blob arg0) {
		eventProcessed = true;
		resultCount++;
		inFlightFetcher--;
		//long start1 = System.currentTimeMillis();
		if (arg0.size() <= 1)
			return;
		
		ByteBuffer contentBuffer = arg0.buf();
		byte[]     content       = new byte[contentBuffer.limit()-contentBuffer.position()];
		
		for (int i = contentBuffer.position(), j=0; i < contentBuffer.limit(); ++i, j++) {
			content[j] = contentBuffer.get(i);
		}

		
		int fb_row = (int)((contentBuffer.get(0)&0xff)<<8);
		int sb_row = (int)(contentBuffer.get(1)&0xff);
		int n_rows = fb_row + sb_row;
		
		int index  = 2;
		for (int i = 0; i < n_rows; i++) {
			int fb = (int)((contentBuffer.get(index)&0xff)<<8);
			int sb = (int)contentBuffer.get(index+1)&0xff;
			int value = fb + sb;
			index +=2;
			
			byte[] contentRow = Arrays.copyOfRange(content, index, index+value);
			index += value;
			
			Data data = getData(new Blob(contentRow));
			//contentBuffers.put(data.getName().toUri(), data.getContent().buf());
			contentBuffers.add( getStringElement(data.getContent().buf()) );
			
		}
		//System.out.println("NDNResolver Tile - OnComplete Time Processing Elapsed: " + (System.currentTimeMillis()-start1)  + "ms"+" inFlight: "+inFlightFetcher);
		//System.out.println("NDNResolver - OnComplete Time Elapsed: " + (stopTime-startTime) + "ms");
	}

	@Override
	public void onError(ErrorCode arg0, String arg1) 
	{
		eventProcessed = true;
		resultCount++;
		inFlightFetcher--;
		
		//System.out.println("NDNResolver - Timeout data packet (" + resultCount + "/" + ndnRequestNames.size()+")");
		System.out.println(DateTime.currentTime()+ "NDNResolver - Error " + arg0 + " for interest " + arg1);
		
		//System.out.println("NDNResolver - OnError Time Elapsed: " + (stopTime-startTime) + "ms");
	}
	
	private Data getData(Blob b)
	{
		
		
		Data actualData = new Data();
		actualData.setContent(b);
		
		try {
			actualData.wireDecode(b);
		} 
		catch (EncodingException e) {
			System.out.println("Blob: "+b.toString());
			e.printStackTrace();
		}
		
		return actualData;
	}
	
	
	
	private void getElements() {
		try {
			startTime = System.currentTimeMillis();
			
			ArrayList<String> ndnRequestNameList = new ArrayList<String>(ndnRequestNames);
			
			//System.out.println("Launching request listy for: " + ndnRequestNameList.size());
			while (interestSended < ndnRequestNameList.size()) {
				if (inFlightFetcher < INTEREST_WINDOW) {
					String   reqName  = ndnRequestNameList.get(interestSended);
					Name     name     = new Name(reqName);
					Interest interest = new Interest(name, requestTimeout);
			
					
					SegmentFetcher.fetch(face, interest, SegmentFetcher.DontVerifySegment, this, this);
					//System.out.println("NDNResolver - Interest " +name.toUri());
					
					inFlightFetcher++;
					interestSended++;
					eventProcessed = false;
				}
				else {
					while (eventProcessed == false) {
						try {
							face.processEvents();
							Thread.yield();
						} 
						catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
			//Flush degli interest accodati nella face
			while (resultCount < ndnRequestNameList.size()) {
				try {
					face.processEvents();
					Thread.yield();
				} 
				catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			face.shutdown();
			stopTime = System.currentTimeMillis();
			
	    }
		catch (Exception e) {
			System.out.println("NDNResolver - Exception: " + e.getMessage()+"\n\n" + stackTraceToString(e.getStackTrace()));
		}
	}
	
	
	private String getStringElement(ByteBuffer contentBuffer) {
		try {
			StringBuilder resultRow = new StringBuilder();
		    if (contentBuffer != null) {
		    	for (int i = contentBuffer.position(); i < contentBuffer.limit(); ++i) {
		    		resultRow.append((char)contentBuffer.get(i));
		    	}
		    }
		    
		    return resultRow.toString();
	    }
		catch (Exception e) {
			System.out.println("NDNResolver - Exception: " + e.getMessage()+"\n\n" + stackTraceToString(e.getStackTrace()));
			
			return "";
		}
	}
	
	
	public HashSet<String> getListElements() {
		return contentBuffers;
	}
	
	public long getElapsedTime() {
		return (stopTime-startTime);
	}
	
	
	//Utils function
	private String stackTraceToString(StackTraceElement[] stakTrace) {
		StringBuilder res = new StringBuilder();
		
		for (StackTraceElement row : stakTrace) {
			res.append(row.getClassName() + "." + row.getMethodName() + "()  -- line " +  row.getLineNumber() + "\n");
		}
		
		return res.toString();
	}
	
	
	@Override
	public void run() {
		super.run();
		
		getElements();
	}
}

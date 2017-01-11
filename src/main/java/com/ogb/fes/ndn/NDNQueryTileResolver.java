package com.ogb.fes.ndn;


import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Comparator;

import com.ogb.fes.concurrent.LoopBodyArgs;
import com.ogb.fes.concurrent.Parallel;
import com.ogb.fes.entity.RangeQueryParams;
import com.ogb.fes.utils.DateTime;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.util.Blob;
import net.named_data.jndn.util.FixedWindowSegmentFetcher;
import net.named_data.jndn.util.FixedWindowSegmentFetcher.ErrorCode;


public class NDNQueryTileResolver extends Thread implements NDNResolver, FixedWindowSegmentFetcher.OnComplete, FixedWindowSegmentFetcher.OnError {
	private HashSet<String> contentBuffers;
	private HashSet<String> ndnRequestNames;
	
	private int  resultCount;
	private int  requestTimeout;
	private Face face;
	
	private int     totalQuerySent;
	private int     inFlightQueries;
	private boolean eventProcessed;
	private int     queryWindow;
	//private String  serverIP;
	
	private long startTime = 0;
	private long stopTime  = 0;
	
	
	//Constructor
	public NDNQueryTileResolver(HashSet<String> requestedNames, String serverIP) {
		this.resultCount     = 0;
		this.requestTimeout  = 4000;
		this.face            = new Face(serverIP);
		this.contentBuffers  = new HashSet<String>();
		this.ndnRequestNames = requestedNames;
		//this.serverIP        = serverIP;
		this.totalQuerySent  = 0;
		this.inFlightQueries = 0;
		this.eventProcessed  = false;
		this.queryWindow     = 4;   //(RangeQueryParams.MAX_TILES>32 ? 32 : RangeQueryParams.MAX_TILES);
	}

	
	@Override
	public void onComplete(Blob arg0) {
		eventProcessed = true;
		resultCount++;
		inFlightQueries--;
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
		inFlightQueries--;
		
		//System.out.println("NDNResolver - Timeout data packet (" + resultCount + "/" + ndnRequestNames.size()+")");
		if(!arg1.contains("Network Nack"))
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

	/*	
	private void getElementsJNI(){
		startTime = System.currentTimeMillis();

		ArrayList<String> ndnRequestNameList = new ArrayList<String>(ndnRequestNames);
		
		Collections.sort(ndnRequestNameList, new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				return o1.compareTo(o2);
			}
		});
		
		Parallel.For(0, ndnRequestNameList.size(), new LoopBodyArgs<Integer>() {
			@Override
			public void run() {	}

			@Override
			public void run(Integer i) {
				//long start = System.currentTimeMillis();
				
				String reqName = ndnRequestNameList.get(i);
				byte[] contentByte = new NDNChunkFetcher().fetchOne(8, reqName);
				
				if (contentByte.length > 2)
					onComplete(new Blob(contentByte));
				else
					onError(ErrorCode.INTEREST_TIMEOUT, reqName);
				
				long stop = System.currentTimeMillis();
				
//				if ( (stop-start) > 0) {
//					System.out.println("Elapsed Time " + (stop-start));
//					System.out.println("Request " + reqName + "\n\n");
//				}
			}
		});
	}
	
	private void getElementsArrayJNI(){
		startTime = System.currentTimeMillis();

		ArrayList<String> ndnRequestNameList = new ArrayList<String>(ndnRequestNames);
		String ndnRequestNamesArray[] = new String[ndnRequestNameList.size()];
		for (int i = 0; i < ndnRequestNameList.size(); i++)
			ndnRequestNamesArray[i] = ndnRequestNameList.get(i);
		
		byte[][] contentByte = new NDNChunkFetcher().fetch(8, ndnRequestNamesArray);
		
		for (int i = 0; i < contentByte.length; i++) {
			if (contentByte.length > 2)
				onComplete(new Blob(contentByte[i]));
			else
				onError(ErrorCode.INTEREST_TIMEOUT, ndnRequestNamesArray[i]);
			
			long stop = System.currentTimeMillis();
		};
	}
	*/	

	private void getElements() {
		try {
			startTime = System.currentTimeMillis();
			
			ArrayList<String> ndnRequestNameList = new ArrayList<String>(ndnRequestNames);
			
			Collections.sort(ndnRequestNameList, new Comparator<String>() {
				@Override
				public int compare(String o1, String o2) {
					return o1.compareTo(o2);
				}
			});
			
			//System.out.println("Launching request listy for: " + ndnRequestNameList.size());
			while (totalQuerySent < ndnRequestNameList.size()) {
				if (inFlightQueries < queryWindow) {
					String   reqName  = ndnRequestNameList.get(totalQuerySent);
					Name     name     = new Name(reqName);
					Interest interest = new Interest(name, requestTimeout);
			
					
					FixedWindowSegmentFetcher.fetch(face, interest, FixedWindowSegmentFetcher.DontVerifySegment, this, this);
					//System.out.println("NDNResolver - Interest " +name.toUri());
					
					inFlightQueries++;
					totalQuerySent++;
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
		//getElementsArrayJNI();
		//getElementsJNI();
		getElements();
	}
}

package com.ogb.fes.ndn;


import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import com.ogb.fes.concurrent.LoopBody;
import com.ogb.fes.concurrent.LoopBodyArgs;
import com.ogb.fes.concurrent.Parallel;
import com.ogb.fes.utils.DateTime;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.util.Blob;
import net.named_data.jndn.util.FixedWindowSegmentFetcher;
import net.named_data.jndn.util.FixedWindowSegmentFetcher.ErrorCode;


public class NDNQueryDataResolver extends Thread implements  NDNResolver, FixedWindowSegmentFetcher.OnComplete, FixedWindowSegmentFetcher.OnError {
	private HashSet<String> contentBuffers;
	private HashSet<String> ndnRequestNames;
	
	private int  resultCount;
	private int  requestTimeout;
	private Face face;
	
	private int     querySent;
	private int     inFlightQueries;
	private boolean eventProcessed;
	private int     queryWindow;
	
	private long startTime = 0;
	private long stopTime  = 0;
	
	//private NDNChunkFetcher ndnChunkFetcher;
	
	
	//Constructor
	public NDNQueryDataResolver(HashSet<String> requestedNames, String serverIP) {
		this.resultCount     = 0;
		this.requestTimeout  = 500;
		this.face            = new Face(serverIP);
		this.contentBuffers  = new HashSet<String>();
		this.ndnRequestNames = requestedNames;
		
		this.querySent    = 0;
		this.inFlightQueries   = 0;
		this.eventProcessed    = false;
		this.queryWindow   = 4;
		
		//ndnChunkFetcher = new NDNChunkFetcher();
	}

	
	@Override
	public void onComplete(Blob arg0) {
		eventProcessed = true;
		resultCount++;
		inFlightQueries--;

		Data data = new Data();
		data.setContent(arg0);
		//contentBuffers.put(data.getName().toUri(), data.getContent().buf());
		contentBuffers.add( getStringElement(data.getContent().buf()) );
		//System.out.println("NDNResolver - OnComplete Time Elapsed: " + (stopTime-startTime) + "ms"+"inFlight: "+inFlightFetcher);
	}

	@Override
	public void onError(ErrorCode arg0, String arg1) 
	{
		eventProcessed = true;
		resultCount++;
		inFlightQueries--;
		
		//System.out.println("NDNResolver - Timeout data packet (" + resultCount + "/" + ndnRequestNames.size()+")");
		if (!arg1.contains("Network Nack"))
			System.out.println(DateTime.currentTime()+ "NDNResolver - Error " + arg0 + " for interest " + arg1);
		
		//System.out.println("NDNResolver - OnError Time Elapsed: " + (stopTime-startTime) + "ms");
	}
	
	@SuppressWarnings("unused")
	private Data getData(Blob b)
	{
		Data actualData = new Data();
		actualData.setContent(b);
		
		try {
			actualData.wireDecode(b);
		} 
		catch (EncodingException e) {
			e.printStackTrace();
		}
		
		return actualData;
	}

	/*
	private void getElementsJNI(){
		startTime = System.currentTimeMillis();

		ArrayList<String> ndnRequestNameList = new ArrayList<String>(ndnRequestNames);

		Parallel.For(0, ndnRequestNameList.size(), new LoopBodyArgs<Integer>() {

			@Override
			public void run() {	}

			@Override
			public void run(Integer i) {
				String reqName = ndnRequestNameList.get(i);
				//System.out.println("Request: " + reqName);
				byte[] contentByte = ndnChunkFetcher.fetchOne(8, reqName);
				if (contentByte.length > 2)
					onComplete(new Blob(contentByte));
				else
					onError(ErrorCode.INTEREST_TIMEOUT, reqName);
			}
		});
	}
	
	private void getElementsArrayJNI(){
		startTime = System.currentTimeMillis();

		//ArrayList<String> ndnRequestNameList = new ArrayList<String>(ndnRequestNames);
		String ndnRequestNamesArray[] = (String[])ndnRequestNames.toArray();
		
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
			
			//System.out.println("Launching request listy for: " + ndnRequestNameList.size());
			while (querySent < ndnRequestNameList.size()) {
				if (inFlightQueries < queryWindow) {
					String   reqName  = ndnRequestNameList.get(querySent);
					Name     name     = new Name(reqName);
					Interest interest = new Interest(name, requestTimeout);
			
					
					FixedWindowSegmentFetcher.fetch(face, interest, FixedWindowSegmentFetcher.DontVerifySegment, this, this);
					//System.out.println("NDNResolver - Interest " +name.toUri());
					
					inFlightQueries++;
					querySent++;
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
			
			/*
			startTime = System.currentTimeMillis();
			
			for (String reqName : ndnRequestNames) {
				Name     name     = new Name(reqName+"/"+reqName.hashCode());
				Interest interest = new Interest(name, requestTimeout);
		
				SegmentFetcher.fetch(face, interest, SegmentFetcher.DontVerifySegment, this, this);
			}
			
			while (resultCount < ndnRequestNames.size()) {
				try {
					face.processEvents();
					Thread.sleep(5);
				} 
				catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			stopTime = System.currentTimeMillis();
			
		    face.shutdown();
			 */
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
	
	/*
	public HashMap<String, String> getStringElementList() {
		HashMap<String, String> result = new HashMap<String, String>();
		
		for (String key : contentBuffers.keySet()) {
			result.put(key, getStringElement(contentBuffers.get(key)));
		}
		
		return result;
	}
	
	
	public HashMap<String, ArrayList<String>> getListElements() {
		double start = System.nanoTime();
		System.out.println("\nNDNResolver - getListElements - Before");
		
		HashMap<String, ArrayList<String>> result = new HashMap<String, ArrayList<String>>();
		
		for (String key : getStringElementList().keySet()) {
			ArrayList<String> list = new ArrayList<String>();
			//for (String s : getStringElementList().get(key).split("\n"))
			//	list.add(s);
			list.add(getStringElementList().get(key));
			result.put(key, list);
		}
		
		double stop = System.nanoTime();
		System.out.println("NDNResolver - getListElements - After " + (stop-start)/1e9+" ms");
		
		return result;
	}
	*/
	
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

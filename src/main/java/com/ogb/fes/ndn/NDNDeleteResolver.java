package com.ogb.fes.ndn;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import com.ogb.fes.domain.User;
import com.ogb.fes.utils.DateTime;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.KeyLocator;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnTimeout;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.security.identity.IdentityManager;
import net.named_data.jndn.security.policy.ConfigPolicyManager;
import net.named_data.jndn.util.Blob;


public class NDNDeleteResolver extends Thread implements NDNResolver, OnData, OnTimeout {
	private HashSet<String> contentBuffers;
	private HashSet<String> ndnRequestNames;
	
	private KeyChain keyChain; 
	private Name     keyLocator;
	
	private int  resultCount;
	private int  requestTimeout;
	private Face face;
	
	private int     interestSended;
	private int     inFlightFetcher;
	private boolean eventProcessed;
	private int     INTEREST_WINDOW;
	
	private long startTime = 0;
	private long stopTime  = 0;
	
	
	//Constructor
	public NDNDeleteResolver(HashSet<String> requestedNames, String serverIP, KeyChain keyChain, Name keyLocator) {
		this.resultCount     = 0;
		this.requestTimeout  = 500;
		this.face            = new Face(serverIP);
		this.contentBuffers  = new HashSet<String>();
		this.ndnRequestNames = requestedNames;
		
		this.keyChain   = keyChain;
		this.keyLocator = keyLocator;
		
		this.interestSended    = 0;
		this.inFlightFetcher   = 0;
		this.eventProcessed    = false;
		this.INTEREST_WINDOW   = 32;
	}

	
	@Override
	public void onData(Interest interest, Data data) {
		eventProcessed = true;
		resultCount++;
		inFlightFetcher--;

		contentBuffers.add( getStringElement(data.getContent().buf()) );
		
		//System.out.println("NDNResolver - OnComplete Time Elapsed: " + (stopTime-startTime) + "ms");
	}

	@Override
	public void onTimeout(Interest interest)
	{
		eventProcessed = true;
		resultCount++;
		inFlightFetcher--;
		
		System.out.println(DateTime.currentTime()+ "NDNResolver - Error for interest " + interest.toUri());
		
		//System.out.println("NDNResolver - OnError Time Elapsed: " + (stopTime-startTime) + "ms");
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
					interest.setInterestLifetimeMilliseconds(500);
					// TODO add security
					//keyChain.setFace(face);
					//keyChain.sign(interest, keyLocator);
								
					face.expressInterest(interest, this, this);
					
					
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

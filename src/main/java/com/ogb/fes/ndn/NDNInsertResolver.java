package com.ogb.fes.ndn;


import java.io.DataOutputStream;

import java.net.Socket;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.couchbase.client.vbucket.ConnectionException;


public class NDNInsertResolver {
	
	private        ThreadPoolExecutor executor;
	private        NDNRepoMap         repoMap;
	public         int                successCounter = 0;
	
	private static NDNInsertResolver  sharedInstance = null;

	
    private NDNInsertResolver() {
    	super();
    	
    	
    	repoMap  = NDNRepoMap.sharedInstance();
    	//Allocate the threadpool
    	int nMaxThread=1;
  
    	executor = new ThreadPoolExecutor(nMaxThread, nMaxThread, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(1000));
    }
    
    public static NDNInsertResolver sharedInstance() {
    	
    	if (sharedInstance == null)
    		sharedInstance = new NDNInsertResolver();
    	
    	return sharedInstance;
    }
    
    
    public synchronized void addContent(NDNContentObject contentObject) {
    	
    	int maxWaitTimeMillis = 5000;
    	long start = System.currentTimeMillis();
    	
    	while (System.currentTimeMillis()<start+maxWaitTimeMillis) {
    		try {
        		executor.execute(createWork(contentObject));
        		return;
        	}
    		catch (RejectedExecutionException e) {
    			try {
					Thread.sleep(5);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
    			continue;
    		}
    	};
    	return;
    }
    
   
    public Runnable createWork(NDNContentObject contentObject){
    	return new Runnable() {
			
			@Override
			public void run() {
				Socket socket = null;
				try {
					
					//System.out.println(DateTime.currentTime()+"NDNBatchInsert - Data Prefix Name: " + contentObject.nameURI.toUri());
					socket = repoMap.getRepoSocket(contentObject.getNameURI());
					//System.out.println(DateTime.currentTime()+"NDNBatchInsert - Socket:"+ socket);
					if (socket != null && socket.isConnected())
						sendOnTCP(socket, contentObject.getSignedContent());	
				} 
				catch (ConnectionException exc) {
					if (socket != null)
						repoMap.checkAndFixSocketConnectionError(socket);
				}
				catch (Exception e) {
					System.out.println("NDNInsertResolver Exception - createWork - " + e.getMessage());
				}
			}
		};
    }
    
    
    
    private void sendOnTCP(Socket socket, byte[] value) {
    	int maxWaitTimeMillis = 10000;
    	long start = System.currentTimeMillis();
    	while (System.currentTimeMillis()<start+maxWaitTimeMillis) {
    		try {
				//System.out.println(DateTime.currentTime()+"Write on Socket START");
				DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
				dataOutputStream.write(value);
				dataOutputStream.flush();
				//System.out.println(DateTime.currentTime()+"Write on Socket END");
				//System.out.println(DateTime.currentTime()+"NDNBatchInsert - Content Object Sent!");
				return;
			} 
			catch (Exception e) {
				System.out.println("NDNInsertResolver Exception - sendOnTCP - " + e.getMessage());
				socket = repoMap.checkAndFixSocketConnectionError(socket);
				
				try {
					Thread.sleep(500);
				} catch (InterruptedException e1) {}
				continue;
			}
    	}
	}
  
}
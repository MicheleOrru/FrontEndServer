package com.ogb.fes.ndn;


import java.io.DataOutputStream;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.ogb.fes.utils.DateTime;


public class NDNInsertResolver {
	
	private        ExecutorService executor;
	private        NDNRepoMap      repoMap;
	public         int             successCounter = 0;
	
	private static NDNInsertResolver sharedInstance = null;

	
    private NDNInsertResolver() {
    	super();
    	
    	
    	repoMap  = NDNRepoMap.sharedInstance();
    	executor = Executors.newFixedThreadPool(16);
    }
    
    public static NDNInsertResolver sharedInstance() {
    	
    	if (sharedInstance == null)
    		sharedInstance = new NDNInsertResolver();
    	
    	return sharedInstance;
    }
    
    
    public synchronized void addContent(NDNContentObject contentObject) {
    	executor.execute(createWork(contentObject));
    }
    
   
    public Runnable createWork(NDNContentObject contentObject){
    	return new Runnable() {
			
			@Override
			public void run() {
				try {
					
					//System.out.println(DateTime.currentTime()+"NDNBatchInsert - Data Prefix Name: " + contentObject.nameURI.toUri());
					
					Socket socket = repoMap.getRepoSocket(contentObject.getNameURI());
//					System.out.println(DateTime.currentTime()+"NDNBatchInsert - Socket:"+ socket);
					if (socket!=null)
						sendOnTCP(socket, contentObject.getSignedContent());
				} 
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
    }
    
    
    
    private void sendOnTCP(Socket socket, byte[] value) {
		try {
			//System.out.println(DateTime.currentTime()+"Write on Socket START");
			DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
			dataOutputStream.write(value);
			dataOutputStream.flush();
			//System.out.println(DateTime.currentTime()+"Write on Socket END");
//			System.out.println(DateTime.currentTime()+"NDNBatchInsert - Content Object Sent!");
		} 
		catch (Exception e) {
			System.out.println("Exception " + e.getMessage());
		}
	}
}


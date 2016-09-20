package com.ogb.fes.ndn;


import java.io.File;
import java.io.FileWriter;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.ogb.fes.entity.ErrorResponse;
import com.ogb.fes.filesystem.FileManager;
import com.ogb.fes.service.GeoJSONProcessor;
import com.ogb.fes.utils.DateTime;
import com.ogb.fes.utils.GeoJSONContainer;


public class NDNInsertManager {
	//private        ExecutorService  executor;
	private        ThreadPoolExecutor  executor;
	
	private static NDNInsertManager sharedInstance = null;

 
    private NDNInsertManager() {
    	super();
    	
    	//Allocate the threadpool
    	int nMaxThread=16;
    	//executor = Executors.newFixedThreadPool(nMaxThread);
    	//LinkedBlockingQueue queue=new LinkedBlockingQueue(16);
    	
    	executor = new ThreadPoolExecutor(nMaxThread, nMaxThread, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue(nMaxThread));
    	
    	//Resume incomplete work (check for existing files in upload-dir and manage it all)
    	//findIncompleteWork();
    }
    
    public static NDNInsertManager sharedInstance() {
    	
    	if (sharedInstance == null)
    		sharedInstance = new NDNInsertManager();
    	
    	return sharedInstance;
    }	
    
    /*TODO support user retrieved from file name to get tid and uid
    private void findIncompleteWork() {
    	
    	//Retrieve old uploaded file (the daemon were not running or an error occurs)
    	for (File file : FileManager.getUploadedFileList()) {
    		executor.execute(createWork(file.getName()));
    	}

    	System.out.println(DateTime.currentTime()+"UploadWatchDir - All Incomplete Work Handled!");
    }
    */

    public synchronized void processInsertFile(String fileName, String tid, String uid, String cid) {
    	
    	System.out.println(DateTime.currentTime()+"processInsertFile - Work Added: " + fileName);
    	
    	int maxWaitTimeMillis = 5000;
    	long start = System.currentTimeMillis();
    	while (System.currentTimeMillis()<start+maxWaitTimeMillis) {
    		try {
        		executor.execute(createWorkFile(fileName, tid, uid, cid));
        		return;
        	}
    		catch (RejectedExecutionException e) {
    			try {
					Thread.sleep(10);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
    			continue;
    		}
    	}
    	return;
    }
    public synchronized String processInsertContent(GeoJSONContainer geoJSONContainer, String userToken) {
    	
    	//System.out.println(DateTime.currentTime()+"processInsertContent - Work For Content Added ");
    	
    	int maxWaitTimeMillis = 5000;
    	if (geoJSONContainer.check() == false) {
    		return "error:wrong geojson file";
    	}
    	long start = System.currentTimeMillis();
    	while (System.currentTimeMillis()<start+maxWaitTimeMillis) {
    		try {
        		executor.execute(createWorkContent(geoJSONContainer, userToken));
        		return "success:"+geoJSONContainer.getObjID();
        	}
    		catch (RejectedExecutionException e) {
    			try {
					Thread.sleep(5);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
    			continue;
    		}
    	};
    	return "error: system overloadred, try later";
    	
    }	

    
    
    private Runnable createWorkFile(final String fileName, String tid, String uid, String cid) {
    	Runnable worker = new Runnable()  
    	{	
			@Override
			public void run()  
			{
				try 
				{
					NDNInsertResolver batchInsert = NDNInsertResolver.sharedInstance();
					
					GeoJSONContainer        geoJSONContainer = FileManager.getUploadFileContentGeoJSON(fileName,tid,uid,cid);
					geoJSONContainer.computeObjID();
			    	if (geoJSONContainer.check() == false) {
			    		createResFile("error:wrong geojson file", fileName);
			    		return;
			    	}
			    	GeoJSONProcessor  processor      = new GeoJSONProcessor(geoJSONContainer, fileName);
					ArrayList<NDNContentObject> contentList = processor.getListOfNDNContentObjectsToInsert();
					
					//the filename contains the token of the user
					for (NDNContentObject ndnContentObject : contentList) 
					{
						batchInsert.addContent(ndnContentObject);
						//System.out.println(DateTime.currentTime()+"processInsertFile - Inserting :" + ndnContentObject.getNameURI().toUri());
					}
					
					while (new File(FileManager.UPLOAD_DIR+"/"+fileName).delete() != true) {
						Thread.sleep(5);
					}
					
					//Create temporary file for send back response to apache thread
					createResFile("success:"+geoJSONContainer.getObjID(), fileName);
					//System.out.println(DateTime.currentTime()+"processInsertFile - Work Completed!");
				} 
				catch (Exception e) 
				{
					e.printStackTrace();
				}
			}
		};
		
		return worker;
    }
    
    private Runnable createWorkContent(final GeoJSONContainer geoJSONContainer, String userToken) {
    	Runnable worker = new Runnable()  
    	{	
			@Override
			public void run()  
			{
				try 
				{					
					GeoJSONProcessor     parser      = new GeoJSONProcessor(geoJSONContainer, userToken) ;			    	
					ArrayList<NDNContentObject> contentList = parser.getListOfNDNContentObjectsToInsert();
					NDNInsertResolver batchInsert = NDNInsertResolver.sharedInstance();
					//the filename contains the token of the user
					for (NDNContentObject ndnContentObject : contentList) 
					{
						batchInsert.addContent(ndnContentObject);
						//System.out.println(DateTime.currentTime()+"processInsertContent - Inserting :" + ndnContentObject.getNameURI().toUri());
					}
					
					//System.out.println(DateTime.currentTime()+"processInsertContent - Work Completed!");
				} 
				catch (Exception e) 
				{
					e.printStackTrace();
				}
			}
		};
		
		return worker;
    }
    
    private void createResFile(String fileContent, String fileName) {
    	try {
	    	//Create result file to send back the oid to apace thread
			File       resFile    = new File(FileManager.UPLOAD_DIR+"/"+fileName+".res.temp");
			FileWriter fileWriter = new FileWriter(resFile);
			fileWriter.write(fileContent);
			fileWriter.flush();
			fileWriter.close();
			
			//Move the .res.temp file to .res
			Files.move( Paths.get(FileManager.UPLOAD_DIR+"/"+fileName+".res.temp"), Paths.get(FileManager.UPLOAD_DIR+"/"+fileName+".res"), StandardCopyOption.REPLACE_EXISTING);
    	}
    	catch(Exception e) {
    		System.out.println(DateTime.currentTime() + "processInsert - Error while creating response file");
    	}
    }
}

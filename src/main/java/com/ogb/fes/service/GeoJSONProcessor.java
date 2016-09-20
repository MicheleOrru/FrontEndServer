package com.ogb.fes.service;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import com.ogb.fes.FesApplication;
import com.ogb.fes.domain.User;
import com.ogb.fes.domainRepositories.UserRepository;
import com.ogb.fes.ndn.NDNContentObject;
import com.ogb.fes.ndn.NDNEntity;
import com.ogb.fes.ndn.NDNKeychainManager;
import com.ogb.fes.ndn.NDNEntity.COMMANDS;
import com.ogb.fes.utils.GeoJSONContainer;
import com.ogb.fes.utils.Utils;

import net.named_data.jndn.Name;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.SecurityException;


//@RestController
public class GeoJSONProcessor {
	
	private GeoJSONContainer geoJSONContainer;
	private String  userToken;
	ArrayList<NDNContentObject> contentObjectList;
	ArrayList<String>           contentObjectNameList;
	HashSet<String> nameSet;
	
	//static GeoJSONProcessor sharedGeoJSONProcessor;
	
	//@Autowired 
	public static UserRepository userRepo;
	KeyChain keyChain;
	Name	 keyLocator;
	
	public GeoJSONProcessor(GeoJSONContainer geoJSONContainer, String userToken) 
	{
		this.geoJSONContainer     	   = geoJSONContainer;
		this.userToken 		   = userToken;
		this.contentObjectList = new ArrayList<>();
		this.contentObjectNameList = new ArrayList<>();
		this.nameSet = new HashSet<String>();
		
		//if (sharedGeoJSONProcessor == null)
		//	sharedGeoJSONProcessor = this;
		
		
		User   user  = userRepo.findByToken(userToken);

		if (user != null)
			keyLocator = new Name(user.getKeyLocator());
		else
			System.out.println("ERROR Null user");
		try {
			keyChain = NDNKeychainManager.createKeychain(keyLocator, user.getPrivateKey(), user.getPublicKey());
		} 
		catch (IOException| SecurityException e) {
			e.printStackTrace();
		}
	}
	
	
	public ArrayList<NDNContentObject> getListOfNDNContentObjectsToInsert() {
		
		int maxSizePayload = 4000;
		contentObjectList = new ArrayList<>();
		
		String reference = appendGeoJSONContent();
		
		ExecutorService executor = Executors.newFixedThreadPool(16);
		
		String payload;
		if (geoJSONContainer.getFullContent().toString().length()>maxSizePayload) {
			payload=reference;
		} else {
			payload=geoJSONContainer.getFullContent().toString();
		}
		
		for (int i = 0; i < geoJSONContainer.getCoordinates().size(); i++) {
			executor.execute(createInsertWork(geoJSONContainer, i, payload));
		}
		
		executor.shutdown();
		
		while (executor.isTerminated() == false) {
			try {
				Thread.sleep(5);
			}
			catch(Exception e){
				
			}
		}
		
		
		return contentObjectList;
	}
	
	public ArrayList<String> getListOfNDNContentObjectDeleteNames() {
		
		contentObjectNameList = new ArrayList<>();
		
		ExecutorService executor = Executors.newFixedThreadPool(16);
		
		for (int i = 0; i < geoJSONContainer.getCoordinates().size(); i++) {
			executor.execute(createDeleteWork(geoJSONContainer, i));
		}
		
		executor.shutdown();
		
		while (executor.isTerminated() == false) {
			try {
				Thread.sleep(5);
			}
			catch(Exception e){
				
			}
		}
		
		
		return contentObjectNameList;
	}
	
	
	private String appendGeoJSONContent() {
	
		NDNEntity entity             = new NDNEntity(geoJSONContainer, 0, null, COMMANDS.INSERT);
		String    geoJSONContentName = entity.getGeoJSONName();
		String    content            = geoJSONContainer.getFullContent().toString();
		
		try {
			int  size = 8129;
			byte data[] = new byte[size];
			int  readByte = 0;
			int  snCounter = 0;
			int  totalReadBytes=0;
			
			for (byte b : content.getBytes()) {
				data[readByte] = b;
				readByte++;
				totalReadBytes++;
				
				if (readByte == size) {
					Name geoJSONContetObjectName=new Name(geoJSONContentName);
					geoJSONContetObjectName.appendSegment(snCounter);
					addContentObject(new NDNContentObject(data, geoJSONContetObjectName, keyChain, keyLocator,(totalReadBytes!=content.getBytes().length)));
					data = new byte[size];
					readByte = 0;
					snCounter++;
				}
			}
			if (readByte>0) {
				Name geoJSONContetObjectName=new Name(geoJSONContentName);
				geoJSONContetObjectName.appendSegment(snCounter);
				addContentObject(new NDNContentObject(data, geoJSONContetObjectName, keyChain, keyLocator,true));
			}
		} 
		catch (SecurityException e) {
			e.printStackTrace();
		}
	
		return geoJSONContentName;
	}




	private synchronized void addContentObject(NDNContentObject content) 
	{
		contentObjectList.add(content);
	}
	
	private synchronized void addContentObjectName(String name) 
	{
		contentObjectNameList.add(name);
	}
	
	
	public Runnable createInsertWork(final GeoJSONContainer geoJSON, final int index, final String payload){
    	return new Runnable() {
			
			@Override
			public void run() {
				try 
				{
					NDNEntity entity = new NDNEntity(geoJSON, index, payload, COMMANDS.INSERT);
					for(String s : entity.getDataNames()) {
						if (!nameSet.contains(s)) {
							nameSet.add(s);
							addContentObject(new NDNContentObject(payload.getBytes(), new Name(s), keyChain, keyLocator,false));
							//System.out.println(payload);
							//addContentObject(entity.getContentObject(sharedGeoJSONParser.keyChain, sharedGeoJSONParser.keyLocator)); 	
						}
					}
				} 
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
    }
	
	public Runnable createDeleteWork(final GeoJSONContainer geoJSONContainer, final int index){
    	return new Runnable() {
			
			@Override
			public void run() {
				try 
				{
					NDNEntity entity = new NDNEntity(geoJSONContainer, index, null, COMMANDS.DELETE);
					for(String s : entity.getDataNames()) {
						if (!nameSet.contains(s)) {
							nameSet.add(s);
							addContentObjectName(s);
						}
					}
				} 
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
    }
}

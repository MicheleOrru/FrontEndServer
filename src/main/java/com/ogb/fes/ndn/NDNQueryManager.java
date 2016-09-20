package com.ogb.fes.ndn;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;


public class NDNQueryManager {

	private HashMap<String, ArrayList<NDNResolver>> reqeustsMap;
	
	private static NDNQueryManager sharedManager = null;

	
	//Private Constructor
	private NDNQueryManager() {
		reqeustsMap = new HashMap<String, ArrayList<NDNResolver>>();
	}
	
	//Factory method
	public static NDNQueryManager sharedManager() {
		if (sharedManager == null)
			sharedManager = new NDNQueryManager();
		
		return sharedManager;
	}
	
	
	public void execNdnDataRequest(HashSet<String> ndnNameRequest, String serverIP) {
		String currentUser = NDNQueryManager.getResolverIdentifier();

		//Retrieve the user request list from his thread id 
		ArrayList<NDNResolver> userRequestList = reqeustsMap.get(currentUser);
		if (userRequestList == null) {
			userRequestList = new ArrayList<NDNResolver>();
			reqeustsMap.put(currentUser, userRequestList);
		}
		
		//Create a new thread for resolving user request
		NDNQueryDataResolver ndnResolver = new NDNQueryDataResolver(ndnNameRequest, serverIP);
		ndnResolver.start();
		
		//Store it in a per-user list for join all
		userRequestList.add(ndnResolver);
	}
	
	public void execNdnTileRequest(HashSet<String> ndnRequestedNames, String serverIP) {
		String currentUser = NDNQueryManager.getResolverIdentifier();

		//Retrieve the user request list from his thread id 
		ArrayList<NDNResolver> userRequestList = reqeustsMap.get(currentUser);
		if (userRequestList == null) {
			userRequestList = new ArrayList<NDNResolver>();
			reqeustsMap.put(currentUser, userRequestList);
		}
		
		//Create a new thread for resolving user request
		NDNQueryTileResolver ndnResolver = new NDNQueryTileResolver(ndnRequestedNames, serverIP);
		ndnResolver.start();
		
		//Store it in a per-user list for join all
		userRequestList.add(ndnResolver);
	}
	
	
	public long joinAll() {
		String currentUser = NDNQueryManager.getResolverIdentifier();
		ArrayList<NDNResolver> userRequestList = reqeustsMap.get(currentUser);
		
		//Unhandled exception! userRequestList == null is impossible in this scope
		if (userRequestList == null)
			return 0;
		
		long time = 0;
		//Join to all resolvers
		for (NDNResolver resolver : userRequestList) {
			try {
				if (resolver instanceof NDNQueryDataResolver)
					((NDNQueryDataResolver)resolver).join();
				else if (resolver instanceof NDNQueryTileResolver)
					((NDNQueryTileResolver)resolver).join();
				time += resolver.getElapsedTime();
			}
			catch (InterruptedException ex) {
				System.out.println("NdnManager - Resolver interrupted exception for user " + currentUser);
			}
		}
		return time;
	}

	
	public ArrayList<String> popAllResults() {
		String currentUser = NDNQueryManager.getResolverIdentifier();
		ArrayList<NDNResolver> userRequestList = reqeustsMap.get(currentUser);
		
		//Unhandled exception! userRequestList == null is impossible in this scope
		if (userRequestList == null) {
			return new ArrayList<String>();
		}
		
		ArrayList<String> resultSet = new ArrayList<String>();
		for (NDNResolver resolver : userRequestList) {
			resultSet.addAll(resolver.getListElements());
		}
		userRequestList.clear();		
		
		return resultSet;
	}
	
	public long getNDNQueryTime() {
		String currentUser = NDNQueryManager.getResolverIdentifier();
		ArrayList<NDNResolver> userRequestList = reqeustsMap.get(currentUser);
		
		//Unhandled exception! userRequestList == null is impossible in this scope
		if (userRequestList == null) {
			return 0;
		}
		
		long time = 0;
		for (NDNResolver resolver : userRequestList) {
			time += resolver.getElapsedTime();
		}
		
		return time;
	}
	
	//public String popAllResultString() {
	//	return String.join("\n", popAllResults());
	//}
	
	
	//Return a per-User unique identifier taken from the thread that serve apace request for user
	private static String getResolverIdentifier() {
		return "NdnResolver_"+Thread.currentThread().getId();
	}
}

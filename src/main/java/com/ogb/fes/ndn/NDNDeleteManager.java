package com.ogb.fes.ndn;



import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import net.named_data.jndn.KeyLocator;
import net.named_data.jndn.Name;
import net.named_data.jndn.security.KeyChain;


public class NDNDeleteManager {

	private HashMap<String, ArrayList<NDNResolver>> requestMap;
	
	private static NDNDeleteManager sharedManager = null;

	
	//Private Constructor
	private NDNDeleteManager() {
		requestMap = new HashMap<String, ArrayList<NDNResolver>>();
	}
	
	//Factory method
	public static NDNDeleteManager sharedManager() {
		if (sharedManager == null)
			sharedManager = new NDNDeleteManager();
		
		return sharedManager;
	}
	
	public void execNdnDeleteRequest(HashSet<String> ndnNames, String serverIP, KeyChain keyChain, Name keyLocator) {
		String currentUser = NDNDeleteManager.getResolverIdentifier();

		//Retrieve the user request list from his thread id 
		ArrayList<NDNResolver> userRequestList = requestMap.get(currentUser);
		if (userRequestList == null) {
			userRequestList = new ArrayList<NDNResolver>();
			requestMap.put(currentUser, userRequestList);
		}
		
		//Create a new thread for resolving user request
		NDNDeleteResolver ndnResolver = new NDNDeleteResolver(ndnNames, serverIP, keyChain, keyLocator);
		ndnResolver.start();
		
		//Store it in a per-user list for join all
		userRequestList.add(ndnResolver);
	}
	
	
	public long joinAll() {
		String currentUser = NDNDeleteManager.getResolverIdentifier();
		ArrayList<NDNResolver> userRequestList = requestMap.get(currentUser);
		
		//Unhandled exception! userRequestList == null is impossible in this scope
		if (userRequestList == null)
			return 0;
		
		long time = 0;
		//Join to all resolvers
		for (NDNResolver resolver : userRequestList) {
			try {
				if (resolver instanceof NDNDeleteResolver)
					((NDNDeleteResolver)resolver).join();
				
				time += resolver.getElapsedTime();
			}
			catch (InterruptedException ex) {
				System.out.println("NdnManager - Resolver interrupted exception for user " + currentUser);
			}
		}
		return time;
	}

	
	public ArrayList<String> popAllResults() {
		String currentUser = NDNDeleteManager.getResolverIdentifier();
		ArrayList<NDNResolver> userRequestList = requestMap.get(currentUser);
		
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
		String currentUser = NDNDeleteManager.getResolverIdentifier();
		ArrayList<NDNResolver> userRequestList = requestMap.get(currentUser);
		
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

package com.ogb.fes.service;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.continuation.Continuation;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.json.JacksonJsonParser;
import org.springframework.core.io.FileSystemResource;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ogb.fes.domain.User;
import com.ogb.fes.domainRepositories.UserRepository;
import com.ogb.fes.entity.ErrorResponse;
import com.ogb.fes.filesystem.FileManager;
import com.ogb.fes.ndn.NDNDeleteManager;
import com.ogb.fes.ndn.NDNInsertManager;
import com.ogb.fes.ndn.NDNKeychainManager;
import com.ogb.fes.ndn.NDNQueryManager;
import com.ogb.fes.net.NetManager;
import com.ogb.fes.utils.DateTime;
import com.ogb.fes.utils.GeoJSON;
import com.ogb.fes.utils.GeoJSONContainer;
import com.ogb.fes.utils.GeoJSONProcessor;
import com.ogb.fes.utils.Utils;

import net.named_data.jndn.Name;
import net.named_data.jndn.security.KeyChain;


@RestController
public class ContentService {
	
	@Autowired
	private UserRepository userRepo;
	
	static NDNDeleteManager ndnDeleteManager = NDNDeleteManager.sharedManager();
	static NDNQueryManager  ndnQueryManager  = NDNQueryManager.sharedManager();
	
	public static String    serverIP; // "127.0.0.1";
	
	
	
	@CrossOrigin(origins="*")
	@RequestMapping(method = RequestMethod.POST, value="/OGB/content/delete", produces="application/json")
	public Object removeContent(HttpServletResponse response, @RequestBody Map<String, Object> params, @RequestHeader(value="Authorization", defaultValue="") String authToken) {
		
		//System.out.println("Received token: "+authToken);
		User user = checkAuthToken(authToken);
		if (user == null) {
			response.setStatus(420);
			return new ErrorResponse(420, "Invalid authorization token");
		}
		
		try {
			String  tid = user.getUserID().split("/")[0];
			String  uid = user.getUserID().split("/")[1];
			String  oid = (String)params.get("oid");
			
			String[] oidComponents = oid.split("/");
			String   delNonce      = oidComponents[oidComponents.length-1];
			String   cid           = oidComponents[oidComponents.length-3];
			
			if (oid == null || oid.length() <= 0) {
				response.setStatus(431);
				return new ErrorResponse(431, "Invalid oid in GeoJSON!");
			}
			
						
			String geoJSONName = oid;//"/"+oid.split("GPS_id")[0]+ "GPS_id/GEOJSON/"+tid+"/"+cid+"/"+uid+"/"+oid;
			System.out.println("Delete Name: "+geoJSONName);
			
			if( !(oidComponents[oidComponents.length-4]+"/"+oidComponents[oidComponents.length-2]).equals(tid+"/"+uid))
			{
				if(!user.isSuperUser() && !user.isAdmin()) {
					response.setStatus(403);
					return new ErrorResponse(403, "User unauthorized!");
				}
			}
			else if (!user.permissionCheck())
			{
				response.setStatus(403);
				return new ErrorResponse(403, "User unauthorized!");
			}
			
			HashSet<String> queries = new HashSet<String>();
			queries.add(geoJSONName);
			ndnQueryManager.execNdnDataRequest(queries, serverIP);
			ndnQueryManager.joinAll();
			
			ArrayList<String> queryResults = ndnQueryManager.popAllResults();
			
			String tidFromOid = oidComponents[oidComponents.length-4];
			String uidFromOid = oidComponents[oidComponents.length-2];
			
			HashSet<String> deleteNames = new HashSet<String>();
			for (String stringResult : queryResults) {
				GeoJSON geoJSONContainer = new GeoJSONContainer(new JacksonJsonParser().parseMap(stringResult),tidFromOid,uidFromOid,cid);
				geoJSONContainer.setNonce(delNonce);
				
				GeoJSONProcessor geoJSONProcessor = new GeoJSONProcessor(geoJSONContainer, user.getToken());
				
				deleteNames.addAll(geoJSONProcessor.getListOfNDNContentObjectDeleteNames());
			}
			String geoJSONDeleteName = geoJSONName.split("GPS_id")[0]+"GPS_id/DELETE"+geoJSONName;
			deleteNames.add(geoJSONDeleteName);
			
			// Add Random nonce
			String nonce = Utils.generateNonce(16);
			HashSet<String> deleteNamesWithNonce = new HashSet<String>();
			for (String s : deleteNames) {
				deleteNamesWithNonce.add(s+"/"+nonce);
			}
			
			Name     keyLocator = new Name(user.getKeyLocator());
			KeyChain keyChain   = NDNKeychainManager.createKeychain(keyLocator, user.getPrivateKey(), user.getPublicKey());
	
			
			ndnDeleteManager.execNdnDeleteRequest(deleteNamesWithNonce, serverIP, keyChain, keyLocator);
			ndnDeleteManager.joinAll();
			ArrayList<String> deleteResults = ndnDeleteManager.popAllResults();
			
			return deleteResults;
		} 
		catch (Exception e) {
			response.setStatus(421);
			return new ErrorResponse(421, "Security issues: User grant not retrieved!", e.getMessage());
		}
    }
	
	
	@CrossOrigin(origins="*")
	@RequestMapping(method = RequestMethod.POST, value="/OGB/content/file-insert/{cid}", produces="application/json")
    public Object insertFileContent(HttpServletResponse response, @RequestParam("file") MultipartFile file, @PathVariable String cid, @RequestHeader(value="Authorization", defaultValue="") String authToken) {
		
		//System.out.println("Received token: "+authToken);
		User user = checkAuthToken(authToken);
		if (user == null) {
			response.setStatus(420);
			return new ErrorResponse(420, "Invalid authorization token");
		}
		
		String name = authToken;
		if (file.isEmpty() == false) {
			try {
				String tid = user.getUserID().split("/")[0];
				String uid = user.getUserID().split("/")[1];
				FileManager.saveFileToUploadDir(name, file);
				NDNInsertManager.sharedInstance().processInsertFile(name, tid, uid,cid);
				
				//System.out.println(DateTime.currentTime()+"ContentService - File <" +name+ "> successfully uploaded!");
			}
			catch (Exception e) {
				System.out.println(DateTime.currentTime()+"ContentService - Failed to upload file <" + name + "> Error: => " + e.getMessage());
				response.setStatus(407);
				return new ErrorResponse(407, "Failed to upload file!\nDetail: " + e.getLocalizedMessage());
			}
		}
		else {
			System.out.println(DateTime.currentTime()+"ContentService - Failed to upload file <" + name  + "> because the file was empty!");
			response.setStatus(407);
			return new ErrorResponse(407, "Error on upload file! Empty file!");
		}
		
		try {
			String resString = FileManager.waitForResFile(name);
			String status    = resString.split(":")[0];
			
			if (status.compareToIgnoreCase("SUCCESS") != 0) {
				response.setStatus(407);
				return new ErrorResponse(407, resString.split(":")[1]);
			}
			else { 
				response.setStatus(200);
				return "{\"oid\": \""+resString.split(":")[1]+"\"}";
			}
		} 
		catch (InterruptedException e) {
			response.setStatus(500);
			return new ErrorResponse(500, "Unable to process file! Severe internal error!");
		}
    }
	
	//TODO remove multipart file and handle GeoJSON POST data
	@CrossOrigin(origins="*")
	@RequestMapping(method = RequestMethod.POST, value="/OGB/content/insert/{cid}", produces="application/json")
    public Object insertContent(HttpServletResponse response, @RequestBody Map<String, Object> params, @PathVariable String cid, @RequestHeader(value="Authorization", defaultValue="") String authToken) {
		
		//System.out.println("Received token: "+authToken);
		User user = checkAuthToken(authToken);
		if (user == null) {
			response.setStatus(420);
			return new ErrorResponse(420, "Invalid authorization token");
		}
		
		if (!user.permissionCheck())
		{
			response.setStatus(421);
			return new ErrorResponse(421, "Invalid permission type ");
		}
		
		
		//TODO check: token's owner must be equal to geoJSON's uID
			
		if (params.isEmpty() == false) {
			try {
				String tid = user.getUserID().split("/")[0];
				String uid = user.getUserID().split("/")[1];
				
				GeoJSON geoJSONContainer = new GeoJSONContainer(new JSONObject(params), tid, uid, cid);
				//geoJSONContainer.computeNonce();
				
				//TODO change processInsertContent to return only oid
				String resString = NDNInsertManager.sharedInstance().processInsertContent(geoJSONContainer, authToken);
				String status    = resString.split(":")[0];
				
				if (status.compareToIgnoreCase("SUCCESS") != 0) {
					response.setStatus(407);
					return new ErrorResponse(407, resString.split(":")[1]);
				}
				else { 
					response.setStatus(200);
					//System.out.println(DateTime.currentTime()+"ContentService - Content successfully uploaded!");
					return "{\"oid\": \""+resString.split(":")[1]+"\"}";
				}
			}
			catch (Exception e) {
				System.out.println(DateTime.currentTime()+"ContentService - Failed to upload Content Error: => " + e.getMessage());
				response.setStatus(407);
				return new ErrorResponse(407, "Failed to upload Content! \n Detail: " + e.getLocalizedMessage());
			}
		}
		else {
			System.out.println(DateTime.currentTime()+"ContentService - Failed to upload Content! The Content is empty!");
			response.setStatus(407);
			return new ErrorResponse(407, "Error on upload Content! Empty Content!");
		}
    }
	
	@RequestMapping(method = RequestMethod.GET, value = "/OGB/content/list")
	public String getUploadFilesList(Model model) {
		
		File rootFolder = new File(FileManager.UPLOAD_DIR);
		//List<String> fileNames = Arrays.stream(rootFolder.listFiles()).map(f -> f.getName()).collect(Collectors.toList());
		model.addAttribute("files", Arrays.stream(rootFolder.listFiles()).sorted(Comparator.comparingLong(f -> -1 * f.lastModified())).map(f -> f.getName()).collect(Collectors.toList()));

		return model.toString();
	}
	
	@CrossOrigin(origins="*")
	@RequestMapping(method = RequestMethod.GET, value="/OGB/content/download", produces="application/zip")
    public void downloadFile(HttpServletResponse response, @RequestParam("fileName") String filename) throws IOException {
		
		String filePath = FileManager.UPLOAD_DIR + "/" + filename;
		System.out.println(DateTime.currentTime()+"ContentService - File Requested: " + filePath);
		
		FileSystemResource fileResponse = new FileSystemResource(new File(filePath));
		InputStream        input        = fileResponse.getInputStream();
		
		byte[] buffer = new byte[1024];
	    int bytesRead;
	    while ((bytesRead = input.read(buffer)) != -1)
	    {
	        response.getOutputStream().write(buffer, 0, bytesRead);
	    }
    }
	
	/* Retrive a static html page with a custom error message
	@ExceptionHandler(Exception.class)
	public ModelAndView handleAllException(Exception ex) {

		ModelAndView model = new ModelAndView("error/generic");
		model.addObject("errMsg", "The maximum file size allowed is 100MB");

		return model;
	}
	*/
	
	private User checkAuthToken(String authToken) {
		if (authToken.length() <= 0)
			return null;
		
		User user = userRepo.findByToken(authToken);
		if (user == null)
			user = checkTokenOnAUCServer(authToken);
		
		return user;
	}
	
	private User checkTokenOnAUCServer(String token) {
		Map<String, Object> postParams = new HashMap<String, Object>();
		postParams.put("token", token);
		Map <String, Object> aucUser = new NetManager().sendCheckToken(postParams);
		if (aucUser == null)
			return null;
		
		return new User(aucUser);
	} 
}

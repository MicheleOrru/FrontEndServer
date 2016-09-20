package com.ogb.fes.domain;


import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;


@Entity
@Table(name="user")
public class User {

	@Id
	@Column(name="user_id")
	private String userID;
	
	@Column(name="user_token")
	private String token;
	
	@Column(name="private_key", columnDefinition="LONGBLOB")
	private byte[] privateKey;
	
	@Column(name="public_key", columnDefinition="LONGBLOB")
	private byte[] publicKey;
	
	@Column(name="key_locator")
	private String keyLocator;
	
	
	public User() {
		super();
	}
	public User(String userID) {
		super();
		
		this.userID = userID;
	}
	public User(String userID, String token) {
		super();
		
		this.token  = token;
		this.userID = userID;
	}
	public User(String userID, String token, byte[] privateKey, byte[] publicKey) {
		super();
		
		this.token   	= token;
		this.userID  	= userID;
		this.privateKey = privateKey;
		this.publicKey 	= privateKey;
	}
	public User(Map<String, Object> params) {
		super();
		
		this.token      = (String)params.get("token");
		this.userID     = (String)params.get("userID");
		this.keyLocator = (String)params.get("keyLocator");
		this.privateKey = ((String)params.get("privateKey")).getBytes();
		this.publicKey  = ((String)params.get("publicKey")).getBytes();
	}
	
	
	
	public String getToken() {
		return token;
	}
	public String getUserID() {
		return userID;
	}
	public String getKeyLocator() {
		return keyLocator;
	}
	public byte[] getPrivateKey() {
		return privateKey;
	}
	public byte[] getPublicKey() {
		return publicKey;
	}
	
	
	public void setToken(String token) {
		this.token = token;
	}
	public void setUserID(String userID) {
		this.userID = userID;
	}
	public void setPrivateKey(byte[] privateKey) {
		this.privateKey = privateKey;
	}
	public void setPublicKey(byte[] publicKey) {
		this.publicKey = publicKey;
	}
	public void setKeyLocator(String keyLocator) {
		this.keyLocator = keyLocator;
	}
}

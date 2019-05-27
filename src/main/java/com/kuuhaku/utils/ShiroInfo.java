package com.kuuhaku.utils;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.*;

import java.util.ArrayList;

public class ShiroInfo {

	private String token;
	private String apiVersion;
	private String name;
	private String version;
	private String default_prefix;
	private String nomeDB;
	private JDA api;
	private ArrayList<String> developers;
	private long startTime;

	public ShiroInfo() {
		token = "NTgwNDQ3NDcxODkzNzQxNTc1.XOQ2cw.c_oRM9-gkILY4_kpcWCj0aIEmyQ";
		
		apiVersion = "3.8.3";
		
		name = "Shiro";
		version = "1.0";
		
		default_prefix = "s!";

		nomeDB = "shiro.sqlite";
		
		developers = new ArrayList<>();
		developers.add("321665807988031495"); //Reydux
	}
	
	public void setAPI(JDA api) {
		this.api = api;
	}
	
	public String getApiVersion() {
		return apiVersion;
	}

	public String getToken() {
		return token;
	}

	public String getDefaultPrefix() { return default_prefix; }

	public String getDBFileName() { return nomeDB; }

	public ArrayList<String> getDevelopers() { return developers; }
	
	public String getName() {
		return name;
	}
	
	public String getVersion() {
		return version;
	}
	
	public String getFullName() {
		return name + " v" + version;
	}
	
	public SelfUser getSelfUser() { return api.getSelfUser(); }
	
	public long getPing() {
		return api.getPing();
	}
	
	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}
	
	public User getUserById(String userId) {
		return api.getUserById(userId);
	}
}

package nl.q42.hue2.models;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Bridge implements Serializable {
	
	private long id;
	private String name;
	private String fullConfig;
	private String user;
	private long lastUsed;
	private String ip;
	private boolean access;
	
	public Bridge() {}
	
	public Bridge(String ip, String name) {
		this.ip = ip;
		this.name = name;
		this.access = true;
	}
	
	public Bridge(String ip, String name, boolean access) {
		this.ip = ip;
		this.name = name;
		this.access = access;
	}
	
	public boolean hasAccess() {
		return access;
	}
	
	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void setFullConfig(String fullConfig) {
		this.fullConfig = fullConfig;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public void setLastUsed(long lastUsed) {
		this.lastUsed = lastUsed;
	}

	public String getIp() {
		return ip;
	}
	
	public void setIp(String ip) {
		this.ip = ip;
	}
}

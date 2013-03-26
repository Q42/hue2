package nl.q42.hue2.models;

public class Bridge {

	private long id;
	private String name;
	private String fullConfig;
	private String user;
	private long lastUsed;
	
	public void setId(long id) {
		this.id = id;
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

}

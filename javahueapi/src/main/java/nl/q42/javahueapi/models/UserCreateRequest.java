package nl.q42.javahueapi.models;

public class UserCreateRequest {
	private String devicetype;
	private String username;
	
	public UserCreateRequest(String devicetype, String username) {
		this.devicetype = devicetype;
		this.username = username;
	}
	
	public String getDevicetype() {
		return devicetype;
	}
	
	public String getUsername() {
		return username;
	}
}

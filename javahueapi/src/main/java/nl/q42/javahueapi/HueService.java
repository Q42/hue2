package nl.q42.javahueapi;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import nl.q42.javahueapi.Networker.Result;
import nl.q42.javahueapi.models.BridgeError;
import nl.q42.javahueapi.models.FullConfig;
import nl.q42.javahueapi.models.Light;
import nl.q42.javahueapi.models.NupnpEntry;
import nl.q42.javahueapi.models.SimpleConfig;
import nl.q42.javahueapi.models.UserCreateRequest;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class HueService {
	
	private String bridgeIp;
	private String username;
	
	private static Gson gson = new Gson();
	
	public HueService() {
		// TODO Hardcoded defaults for now:
		bridgeIp = "192.168.1.101";
		username = "aValidUser";
	}
	
	public static List<String> getBridgeIps() throws IOException, ApiException {
		//Result result = Networker.get("http://www.meethue.com/api/nupnp");
		Result result = Networker.get("http://connected-lamps-dev.appspot.com/api/nupnp");
		
		if (result.getResponseCode() == 200) {
			Type collectionType = new TypeToken<List<NupnpEntry>>(){}.getType();
			List<NupnpEntry> entries = gson.fromJson(result.getBody(), collectionType);
			
			List<String> ips = new ArrayList<String>();
			for (NupnpEntry entry : entries) {
				ips.add(entry.internalipaddress);
			}
			return ips;
		}
		throw new ApiException(result);
	}
	
	public static SimpleConfig getSimpleConfig(String ip) throws IOException, ApiException {
		Result result = Networker.get("http://" + ip + "/api/config");
		
		if (result.getResponseCode() == 200) {
			return gson.fromJson(result.getBody(), SimpleConfig.class);
		}
		throw new ApiException(result);
	}
	
	/**
	 * Returns true if the given username is an existing user on the given bridge, false if it could not be concluded.
	 */
	public static boolean userExists(String ip, String username) throws IOException {
		if (username.length() < 10) return false;
		
		Result result = Networker.get("http://" + ip + "/api/" + URLEncoder.encode(username, "utf-8"));
		
		if (result.getResponseCode() == 200) {
			try {
				gson.fromJson(result.getBody(), FullConfig.class);
			} catch (Exception e) {
				return false;
			}
			
			return true;
		}
		
		return false;
	}
	
	/**
	 * Returns true if the user was created and false if the link button was not pressed.
	 */
	public static boolean createUser(String ip, String devicetype, String username) throws IOException, ApiException {
		Result result = Networker.post("http://" + ip + "/api", gson.toJson(new UserCreateRequest(devicetype, username)));
		
		if (result.getResponseCode() == 200) {
			Type collectionType = new TypeToken<List<Map<String, BridgeError>>>(){}.getType();
			List<Map<String, BridgeError>> error = gson.fromJson(result.getBody(), collectionType);
			
			if (error.get(0).containsKey("success")) {
				return true;
			} else if (error.get(0).get("error").type == 101) {
				return false;
			}
		}
		throw new ApiException(result);
	}
	
	
	public FullConfig getFullConfig() throws IOException, ApiException {
		Result result = Networker.get("http://" + bridgeIp + "/api/" + username + "/");
		
		if (result.getResponseCode() == 200) {
			return gson.fromJson(result.getBody(), FullConfig.class);
		}
		throw new ApiException(result); 
	}
	
	public Map<String, Light> getLights() throws IOException, ApiException {
		Result result = Networker.get("http://" + bridgeIp + "/api/" + username + "/lights");
		
		if (result.getResponseCode() == 200) {
			Type collectionType = new TypeToken<Map<String, Light>>(){}.getType();
			return gson.fromJson(result.getBody(), collectionType);
		}
		throw new ApiException(result);
	}
	
	public void turnLightOn(String id, boolean on) throws IOException, ApiException {
		Result result = Networker.put("http://" + bridgeIp + "/api/" + username + "/lights/" + id + "/state",
				"{\"on\":" + on + "}");
		if (result.getResponseCode() != 200)
			throw new ApiException(result);
	}
	
	
	public void setBridgeIp(String bridgeIp) {
		this.bridgeIp = bridgeIp;
	}
	
	public void setUsername(String username) {
		this.username = username;
	}
	
	
	@SuppressWarnings("serial")
	public static class ApiException extends Exception {
		public ApiException(Result result) {
			super("Error " + result.getResponseCode() + ": " + result.getBody());
		}
	}
}

package nl.q42.javahueapi.models;

import com.google.gson.annotations.SerializedName;

public class WhitelistItem {
	@SerializedName("last use date")
	public String lastUseDate;
	
	@SerializedName("create date")
	public String createDate;
	
	public String name;
}

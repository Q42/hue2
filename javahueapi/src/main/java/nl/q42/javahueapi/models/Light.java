package nl.q42.javahueapi.models;

import java.io.Serializable;
import java.util.Map;

@SuppressWarnings("serial")
public class Light implements Serializable {
	public State state;
	
	public String type;
	
	public String name;
	
	public String modelid;
	
	public String swversion;
	
	public Map<String, String> pointsymbol;
}

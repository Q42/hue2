package nl.q42.javahueapi.models;

import java.io.Serializable;
import java.util.List;

@SuppressWarnings("serial")
public class Group implements Serializable {
	public List<String> lights;
	
	public String name;
}

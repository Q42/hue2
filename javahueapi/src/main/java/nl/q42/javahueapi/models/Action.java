package nl.q42.javahueapi.models;

import java.io.Serializable;

/**
 * Represents a group action
 */
@SuppressWarnings("serial")
public class Action implements Serializable {
	public boolean on;
	
	public int bri;
	
	public int hue;
	
	public int sat;
	
	public float[] xy;
	
	public int ct;
	
	public String effect;
	
	public String colormode;
}

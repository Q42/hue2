package nl.q42.javahueapi.models;

import java.io.Serializable;

/**
 * Represents a light state.
 */
@SuppressWarnings("serial")
public class State implements Serializable {
	public boolean on;
	
	public int bri;
	
	public int hue;
	
	public int sat;
	
	public double[] xy;
	
	public int ct;
	
	public String alert;
	
	public String effect;
	
	public String colormode;
	
	public boolean reachable;
}

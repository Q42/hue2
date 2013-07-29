package nl.q42.hue2.models;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Preset implements Serializable {
	public String light;	
	public float[] xy;
	public int brightness;
}

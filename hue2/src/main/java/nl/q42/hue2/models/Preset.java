package nl.q42.hue2.models;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Preset implements Serializable {
	public int id;
	public String light;	
	public float[] xy;
	public int brightness;
	
	public Preset() {}
	
	public Preset(int id, String light, float[] xy, int brightness) {
		this.id = id;
		this.light = light;
		this.xy = xy;
		this.brightness = brightness;
	}
	
	@Override
	public boolean equals(Object other) {
		return other instanceof Preset && ((Preset) other).id == id;
	}
}

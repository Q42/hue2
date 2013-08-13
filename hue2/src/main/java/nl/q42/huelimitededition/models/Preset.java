package nl.q42.huelimitededition.models;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Preset implements Serializable {
	public int id;
	public String light;
	public String group;
	public String color_mode;
	public float[] xy;
	public float ct;
	public int brightness;
	
	public Preset() {}
	
	public Preset(int id, String light, String group, float[] xy, int brightness) {
		this.id = id;
		this.light = light;
		this.group = group;
		this.color_mode = "xy";
		this.xy = xy;
		this.brightness = brightness;
	}
	
	public Preset(int id, String light, String group, float ct, int brightness) {
		this.id = id;
		this.light = light;
		this.group = group;
		this.color_mode = "ct";
		this.ct = ct;
		this.brightness = brightness;
	}
	
	@Override
	public boolean equals(Object other) {
		return other instanceof Preset && ((Preset) other).id == id;
	}
}

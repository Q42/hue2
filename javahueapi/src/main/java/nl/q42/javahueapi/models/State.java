package nl.q42.javahueapi.models;

import java.io.Serializable;

/**
 * Represents a light state.
 */
@SuppressWarnings("serial")
public class State extends Action implements Serializable {
	public String alert;
	
	public boolean reachable;
}

package nl.q42.hue2;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.SecureRandom;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nl.q42.hue2.models.Bridge;
import nl.q42.javahueapi.models.Light;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.util.Base64;

public class Util {
	public static String quickMatch(String pattern, String target) {
		Matcher m = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(target);
		if (m.find()) {
			return m.group(1);
		} else {
			return null;
		}
	}
	
	/**
	 * Returns unique app installation identifier, used to identify this device
	 */
	public static String getDeviceIdentifier(Context ctx) {
		SharedPreferences prefs = ctx.getApplicationContext().getSharedPreferences("app_prefs", 0);
		
		if (prefs.contains("uuid")) {
			return prefs.getString("uuid", null);
		} else {
			String uuid = generateUUID();
			
			SharedPreferences.Editor ed = prefs.edit();
			ed.putString("uuid", uuid);
			ed.commit();
			
			return uuid;
		}
	}
	
	public static void setLastBridge(Context ctx, Bridge b) {
		if (b == null) {
			SharedPreferences.Editor prefs = ctx.getApplicationContext().getSharedPreferences("app_prefs", 0).edit();
			prefs.remove("lastBridge");
			prefs.commit();
			return;
		}
		
		// Serialize bridge object and store it
		try {
			ByteArrayOutputStream binStr = new ByteArrayOutputStream();
			ObjectOutputStream objStr = new ObjectOutputStream(binStr);
			objStr.writeObject(b);
			objStr.close();
			
			SharedPreferences.Editor prefs = ctx.getApplicationContext().getSharedPreferences("app_prefs", 0).edit();
			prefs.putString("lastBridge", Base64.encodeToString(binStr.toByteArray(), Base64.DEFAULT));
			prefs.commit();
		} catch (IOException e) {
			// This should never happen
			e.printStackTrace();
		}
	}
	
	public static Bridge getLastBridge(Context ctx) {
		SharedPreferences prefs = ctx.getApplicationContext().getSharedPreferences("app_prefs", 0);
		
		if (prefs.contains("lastBridge")) {
			try {
				byte[] data = Base64.decode(prefs.getString("lastBridge", null), Base64.DEFAULT);
				ByteArrayInputStream binStr =  new ByteArrayInputStream( data); 
				ObjectInputStream objStr = new ObjectInputStream(binStr);
				Bridge b = (Bridge) objStr.readObject();
				objStr.close();
				return b;
			} catch (Exception e) {
				// Should never happen
				e.printStackTrace();
			}
		}
		
		return null;
	}
	
	private static String generateUUID() {
		SecureRandom rand = new SecureRandom();
		UUID uuid = new UUID(rand.nextLong(), rand.nextLong());
		return uuid.toString();
	}
	
	public static String getDeviceName() {
		  String manufacturer = Build.MANUFACTURER;
		  String model = Build.MODEL;
		  
		  if (model.startsWith(manufacturer)) {
			  return model.substring(0, 1).toUpperCase() + model.substring(1);
		  } else {
			  return manufacturer.substring(0, 1).toUpperCase() + manufacturer.substring(1) + " " + model;
		  }
	}
	
	/**
	 * Color conversion helper functions
	 */
	public static int getRGBColor(Light light) {
		if (!light.state.on) {
			return Color.BLACK;
		}
		
		// Convert HSV color to RGB
		if (light.state.colormode.equals("hs")) {
			float[] components = new float[] {
				(float) light.state.hue / 65535.0f * 360.0f,
				(float) light.state.sat / 255.0f,
				1.0f // Ignore brightness for more clear color view, hue is most important anyway
			};
			
			return Color.HSVToColor(components);
		} else if (light.state.colormode.equals("xy")) {
			float[] points = new float[] { (float) light.state.xy[0], (float) light.state.xy[1] };
			return PHUtilitiesImpl.colorFromXY(points, light.modelid);
		} else if (light.state.colormode.equals("ct")) {
			return temperatureToColor(1000000 / light.state.ct);
		} else {
			return Color.WHITE;
		}
	}
	
	// Adapted from: http://www.tannerhelland.com/4435/convert-temperature-rgb-algorithm-code/
	public static int temperatureToColor(long tmpKelvin) {
		double tmpCalc;
		int r, g, b;
		
		// Temperature must fall between 1000 and 40000 degrees
		tmpKelvin = Math.min(40000, Math.max(tmpKelvin, 1000));
		
		// All calculations require tmpKelvin / 100, so only do the conversion once
		tmpKelvin /= 100;
		
		// Calculate each color in turn
		
		// First: red
		if (tmpKelvin <= 66) {
			r = 255;
		} else {
			// Note: the R-squared value for this approximation is .988
			tmpCalc = tmpKelvin - 60;
			tmpCalc = 329.698727446 * Math.pow(tmpCalc, -0.1332047592);
			r = (int) tmpCalc;
			r = Math.min(255, Math.max(r, 0));
		}
		
		// Second: green
		if (tmpKelvin <= 66) {
			// Note: the R-squared value for this approximation is .996
			tmpCalc = tmpKelvin;
			tmpCalc = 99.4708025861 * Math.log(tmpCalc) - 161.1195681661;
			g = (int) tmpCalc;
			g = Math.min(255, Math.max(g, 0));
		} else {
			// Note: the R-squared value for this approximation is .987
			tmpCalc = tmpKelvin - 60;
			tmpCalc = 288.1221695283 * Math.pow(tmpCalc, -0.0755148492);
			g = (int) tmpCalc;
			g = Math.min(255, Math.max(g, 0));
		}
		
		// Third: blue
		if (tmpKelvin >= 66) {
			b = 255;
		} else if (tmpKelvin <= 19) {
			b = 0;
		} else {
			// Note: the R-squared value for this approximation is .998
			tmpCalc = tmpKelvin - 10;
			tmpCalc = 138.5177312231 * Math.log(tmpCalc) - 305.0447927307;
			b = (int) tmpCalc;
			b = Math.min(255, Math.max(b, 0));
		}
		
		return Color.rgb(r, g, b);
	}
}

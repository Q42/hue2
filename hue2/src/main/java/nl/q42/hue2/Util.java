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
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
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
	
	public static void showErrorDialog(Context ctx, int title, int message) {
		new AlertDialog.Builder(ctx)
			.setTitle(title)
			.setMessage(message)
			.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			}).create().show();
	}
}

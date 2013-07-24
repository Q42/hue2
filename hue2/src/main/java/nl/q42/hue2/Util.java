package nl.q42.hue2;

import java.security.SecureRandom;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;

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

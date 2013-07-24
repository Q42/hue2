package nl.q42.hue2;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.provider.Settings;

public class Util {
	public static String quickMatch(String pattern, String target) {
		Matcher m = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(target);
		if (m.find()) {
			return m.group(1);
		} else {
			return null;
		}
	}
	
	public static String getDeviceIdentifier(Context ctx) {
		// TODO: This may return null, so add a fallback like generating a random key and storing it
		return Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID);
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

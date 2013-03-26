package nl.q42.hue2.activities;

import java.util.List;

import nl.q42.hue2.R;
import nl.q42.javahueapi.HueService;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

public class LinkActivity extends Activity {
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_link);
		
		// TODO look at www.meethue.com/api/nupnp
		// TODO look at upnp
		
		
		new AsyncTask<Void, Void, List<String>>() {
			@Override
			protected List<String> doInBackground(Void... params) {
				Log.d("hue2", "Looking for bridges:");
				return HueService.getBridgeIps();
			}
			
			@Override
			protected void onPostExecute(List<String> ips) {
				for (String ip : ips) {
					Log.d("hue2", ip);
				}
			}
		}.execute();
	}
}

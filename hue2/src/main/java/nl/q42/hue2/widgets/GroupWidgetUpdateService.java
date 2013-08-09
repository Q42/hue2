package nl.q42.hue2.widgets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import nl.q42.hue2.R;
import nl.q42.hue2.Util;
import nl.q42.javahueapi.HueService;
import nl.q42.javahueapi.models.FullConfig;
import nl.q42.javahueapi.models.Group;
import nl.q42.javahueapi.models.Light;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.SparseArray;
import android.view.View;
import android.widget.RemoteViews;

public class GroupWidgetUpdateService extends Service {
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {		
		// Ignore if screen is off
		PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
		if (!powerManager.isScreenOn()) {
			stopSelf();
			return START_NOT_STICKY;
		}
		
		final AppWidgetManager widgetManager = AppWidgetManager.getInstance(getApplicationContext());
		final int[] widgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		
		// Fetch group states and update UI of all widgets
		new AsyncTask<Void, Void, SparseArray<FullConfig>>() {
			@Override
			protected SparseArray<FullConfig> doInBackground(Void... params) {
				// Check if WiFi is connected at all before wasting time
				ConnectivityManager manager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
				NetworkInfo wifi = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
				
				if (!wifi.isConnected()) {
					return null;
				}
				
				// Fetch current state of all bridges used in widgets (usually just one)				
				HashMap<String, FullConfig> ipConfigs = new HashMap<String, FullConfig>();
				SparseArray<FullConfig> bridgeConfigs = new SparseArray<FullConfig>();
				
				for (int wid : widgetIds) {
					if (!prefs.contains("widget_" + wid + "_ip")) continue;
					
					String ip = prefs.getString("widget_" + wid + "_ip", null);
					
					try {
						if (!ipConfigs.containsKey(ip)) {
							ipConfigs.put(ip, new HueService(ip, Util.getDeviceIdentifier(getApplicationContext())).getFullConfig());
						}
						
						bridgeConfigs.put(wid, ipConfigs.get(ip));
					} catch (Exception e) {
						e.printStackTrace();
						// Ignore network error here and move on to next widget, will be handled later
					}
				}
					
				return bridgeConfigs;
			}
			
			@Override
			protected void onPostExecute(SparseArray<FullConfig> configs) {
				for (int id : widgetIds) {
					RemoteViews views = new RemoteViews(getPackageName(), R.layout.widget_group);
					
					if (configs.get(id) != null) {
						// Update widget UI
						FullConfig cfg = configs.get(id);
						String widgetGroup = prefs.getString("widget_" + id + "_id", null);
						
						updateWidget(widgetIds, id, views, cfg.groups.get(widgetGroup), cfg.lights, cfg.config.ipaddress);
					} else {
						// Replace content with loading spinner
						views.setViewVisibility(R.id.widget_group_spinner, View.VISIBLE);
						views.setViewVisibility(R.id.widget_group_content, View.GONE);
					}
					
					widgetManager.updateAppWidget(id, views);
				}
				
				stopSelf();
			}
		}.execute();
		
		return START_NOT_STICKY;
	}
	
	private void updateWidget(int[] widgetIds, int id, RemoteViews views, Group group, Map<String, Light> lights, String ip) {
		// Replace loading spinner with content
		views.setViewVisibility(R.id.widget_group_spinner, View.GONE);
		views.setViewVisibility(R.id.widget_group_content, View.VISIBLE);
		
		// Handle exception of "all lights" group
		if (group == null) {
			group = new Group();
			group.name = getString(R.string.widget_group_config_all_lights);
			group.lights = new ArrayList<String>();
			group.lights.addAll(lights.keySet());
		}
		
		// Determine current group on/off state and light color
		int lightsOn = 0, totalRed = 0, totalGreen = 0, totalBlue = 0;
		
		for (String lid : group.lights) {
			Light light = lights.get(lid);
			if (light.state.on) {
				lightsOn++;
				
				int col = Util.getRGBColor(light);
				totalRed += Color.red(col);
				totalGreen += Color.green(col);
				totalBlue += Color.blue(col);
			}
		}
		
		// Light is considered on if at least one of its lights is on
		// This creates behaviour where toggling a group with one light on turns that light off, which seems
		// more reasonable than turning the remaining lights on.
		boolean groupOn = lightsOn > 0;
		int averageColor = groupOn ? Color.rgb(totalRed / lightsOn, totalGreen / lightsOn, totalBlue / lightsOn) : Color.rgb(101, 101, 101);
		
		// Update views
		views.setTextViewText(R.id.widget_group_name, group.name);
		views.setInt(R.id.widget_group_indicator, "setBackgroundColor", averageColor);
	}
	
	// Unused, but has to be implemented
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}

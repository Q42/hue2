package nl.q42.hue2.widgets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import nl.q42.hue2.R;
import nl.q42.hue2.Util;
import nl.q42.javahueapi.HueService;
import nl.q42.javahueapi.models.FullConfig;
import nl.q42.javahueapi.models.Light;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.RemoteViews;

public class WidgetUpdateService extends Service {
	public static class WidgetButton {
		public int widget;
		public int button;
		
		public WidgetButton(int widget, int button) {
			this.widget = widget;
			this.button = button;
		}
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		final AppWidgetManager widgetManager = AppWidgetManager.getInstance(getApplicationContext());
		final int[] widgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		
		// Fetch light states and update UI of all widgets
		new AsyncTask<Void, Void, Map<String, FullConfig>>() {
			@Override
			protected Map<String, FullConfig> doInBackground(Void... params) {
				// Fetch current state of all bridges used in widgets (usually just one)				
				try {
					HashMap<String, FullConfig> bridgeConfigs = new HashMap<String, FullConfig>();
					
					for (int wid : widgetIds) {
						String ip = prefs.getString("widget_" + wid + "_ip", null);
						
						if (!bridgeConfigs.containsKey(ip)) {
							bridgeConfigs.put(ip, new HueService(ip, Util.getDeviceIdentifier(getApplicationContext())).getFullConfig());
						}
					}
					
					return bridgeConfigs;
				} catch (Exception e) {
					// TODO: Handle network error
					return null;
				}
			}
			
			@Override
			protected void onPostExecute(Map<String, FullConfig> configs) {
				if (configs == null) return;
				
				for (int id : widgetIds) {
					// Build map of lights in this widget
					FullConfig cfg = configs.get(prefs.getString("widget_" + id + "_ip", null));
					HashMap<String, Light> lights = new HashMap<String, Light>();
					Set<String> widgetLights = prefs.getStringSet("widget_" + id + "_ids", null);
					
					for (String lid : widgetLights) {
						lights.put(lid, cfg.lights.get(lid));
					}
					
					// Update widget UI
					RemoteViews views = new RemoteViews(getPackageName(), R.layout.widget);
					
					updateWidget(widgetIds, id, views, lights, cfg.config.ipaddress);
					
					widgetManager.updateAppWidget(id, views);
				}
				
				stopSelf();
			}
		}.execute();
		
		return START_NOT_STICKY;
	}
	
	private void updateWidget(int[] widgetIds, int id, RemoteViews views, HashMap<String, Light> lights, String ip) {
		// Used to preserve light name order
		ArrayList<String> sids = Util.getSortedLights(lights);
		
		// Replace loading spinner with content
		views.setViewVisibility(R.id.widget_spinner, View.GONE);
		views.setViewVisibility(R.id.widget_content, View.VISIBLE);
		
		// Update views
		for (int i = 0; i < 6; i++) {
			int idButton = getResources().getIdentifier("widget_light" + (i + 1) + "_button", "id", getPackageName());
			int idName = getResources().getIdentifier("widget_light" + (i + 1) + "_name", "id", getPackageName());
			int idColor = getResources().getIdentifier("widget_light" + (i + 1) + "_color", "id", getPackageName());
			int idIndicator = getResources().getIdentifier("widget_light" + (i + 1) + "_indicator", "id", getPackageName());
			
			if (sids.size() > i) {
				views.setViewVisibility(idButton, View.VISIBLE);
				views.setOnClickPendingIntent(idButton, createToggleIntent(WidgetUpdateService.this, widgetIds, ip, sids.get(i), id, i + 1));
				views.setTextViewText(idName, lights.get(sids.get(i)).name);
				views.setTextColor(idName, lights.get(sids.get(i)).state.on ? Color.WHITE : Color.rgb(101, 101, 101));
				views.setInt(idColor, "setBackgroundColor", Util.getRGBColor(lights.get(sids.get(i))));
				views.setInt(idIndicator, "setBackgroundResource", lights.get(sids.get(i)).state.on ? R.drawable.appwidget_settings_ind_on_c_holo : R.drawable.appwidget_settings_ind_off_c_holo);
			} else {
				views.setViewVisibility(idButton, View.GONE);
			}
		}
	}
	
	private PendingIntent createToggleIntent(Context context, int[] widgetIds, String ip, String light, int widget, int button) {
		// This is needed so that intents are not re-used with wrong extras data
		int requestCode = ip.hashCode() + light.hashCode();
		
		Intent intent = new Intent(context, WidgetToggleService.class);
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds);
		intent.putExtra("ip", ip);
		intent.putExtra("light", light);
		intent.putExtra("widget", widget);
		intent.putExtra("button", button);
		PendingIntent pendingIntent = PendingIntent.getService(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		
		return pendingIntent;
	}

	// Unused, but has to be implemented
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}

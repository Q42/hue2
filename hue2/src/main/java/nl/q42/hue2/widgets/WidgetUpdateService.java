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
				
				for (int i : widgetIds) {
					// Build map of lights in this widget
					Map<String, Light> bridgeLights = configs.get(prefs.getString("widget_" + i + "_ip", null)).lights;
					HashMap<String, Light> lights = new HashMap<String, Light>();
					Set<String> widgetLights = prefs.getStringSet("widget_" + i + "_ids", null);
					
					for (String lid : widgetLights) {
						lights.put(lid, bridgeLights.get(lid));
					}
					
					// Update widget UI
					RemoteViews views = new RemoteViews(getPackageName(), R.layout.widget);
					
					updateWidget(widgetIds, views, lights);
					
					widgetManager.updateAppWidget(i, views);
				}
				
				stopSelf();
			}
		}.execute();
		
		return START_NOT_STICKY;
	}
	
	// TODO: Clean up code
	private void updateWidget(int[] widgetIds, RemoteViews views, HashMap<String, Light> lights) {
		// Used to preserve light name order
		ArrayList<String> sids = Util.getSortedLights(lights);
		
		// Swap loading spinner with content
		views.setViewVisibility(R.id.widget_spinner, View.GONE);
		views.setViewVisibility(R.id.widget_content, View.VISIBLE);
		
		// Only show as much views as used
		views.setViewVisibility(R.id.widget_light1_button, sids.size() >= 1 ? View.VISIBLE : View.GONE);
		views.setViewVisibility(R.id.widget_light2_button, sids.size() >= 2 ? View.VISIBLE : View.GONE);
		views.setViewVisibility(R.id.widget_light3_button, sids.size() >= 3 ? View.VISIBLE : View.GONE);
		views.setViewVisibility(R.id.widget_light4_button, sids.size() >= 4 ? View.VISIBLE : View.GONE);
		
		// Update views
		if (sids.size() >= 1) {
			views.setOnClickPendingIntent(R.id.widget_light1_button, createToggleIntent(WidgetUpdateService.this, widgetIds, sids.get(0)));
			views.setTextViewText(R.id.widget_light1_name, lights.get(sids.get(0)).name);
			views.setTextColor(R.id.widget_light1_name, lights.get(sids.get(0)).state.on ? Color.WHITE : Color.rgb(101, 101, 101));
			views.setInt(R.id.widget_light1_color, "setBackgroundColor", Util.getRGBColor(lights.get(sids.get(0))));
			views.setInt(R.id.widget_light1_indicator, "setBackgroundResource", lights.get(sids.get(0)).state.on ? R.drawable.appwidget_settings_ind_on_c_holo : R.drawable.appwidget_settings_ind_off_c_holo);
		}
		
		if (sids.size() >= 2) {
			views.setOnClickPendingIntent(R.id.widget_light2_button, createToggleIntent(WidgetUpdateService.this, widgetIds, sids.get(1)));
			views.setTextViewText(R.id.widget_light2_name, lights.get(sids.get(1)).name);
			views.setTextColor(R.id.widget_light2_name, lights.get(sids.get(1)).state.on ? Color.WHITE : Color.rgb(101, 101, 101));
			views.setInt(R.id.widget_light2_color, "setBackgroundColor", Util.getRGBColor(lights.get(sids.get(1))));
			views.setInt(R.id.widget_light2_indicator, "setBackgroundResource", lights.get(sids.get(1)).state.on ? R.drawable.appwidget_settings_ind_on_c_holo : R.drawable.appwidget_settings_ind_off_c_holo);
		}
		
		if (sids.size() >= 3) {
			views.setOnClickPendingIntent(R.id.widget_light3_button, createToggleIntent(WidgetUpdateService.this, widgetIds, sids.get(2)));
			views.setTextViewText(R.id.widget_light3_name, lights.get(sids.get(2)).name);
			views.setTextColor(R.id.widget_light3_name, lights.get(sids.get(2)).state.on ? Color.WHITE : Color.rgb(101, 101, 101));
			views.setInt(R.id.widget_light3_color, "setBackgroundColor", Util.getRGBColor(lights.get(sids.get(2))));
			views.setInt(R.id.widget_light3_indicator, "setBackgroundResource", lights.get(sids.get(2)).state.on ? R.drawable.appwidget_settings_ind_on_c_holo : R.drawable.appwidget_settings_ind_off_c_holo);
		}
		
		if (sids.size() >= 4) {
			views.setOnClickPendingIntent(R.id.widget_light4_button, createToggleIntent(WidgetUpdateService.this, widgetIds, sids.get(3)));
			views.setTextViewText(R.id.widget_light4_name, lights.get(sids.get(3)).name);
			views.setTextColor(R.id.widget_light4_name, lights.get(sids.get(3)).state.on ? Color.WHITE : Color.rgb(101, 101, 101));
			views.setInt(R.id.widget_light4_color, "setBackgroundColor", Util.getRGBColor(lights.get(sids.get(3))));
			views.setInt(R.id.widget_light4_indicator, "setBackgroundResource", lights.get(sids.get(3)).state.on ? R.drawable.appwidget_settings_ind_on_c_holo : R.drawable.appwidget_settings_ind_off_c_holo);
		}
	}
	
	private PendingIntent createToggleIntent(Context context, int[] widgetIds, String light) {
		// This is needed so that intents are not re-used with wrong extras data
		int requestCode = (int) (System.currentTimeMillis() + light.hashCode());
		
		Intent intent = new Intent(context, WidgetToggleService.class);
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds);
		intent.putExtra("light", light);
		PendingIntent pendingIntent = PendingIntent.getService(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		
		return pendingIntent;
	}

	// Unused, but has to be implemented
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}

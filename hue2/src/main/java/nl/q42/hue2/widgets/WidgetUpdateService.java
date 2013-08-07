package nl.q42.hue2.widgets;

import java.util.Map;

import nl.q42.hue2.R;
import nl.q42.hue2.Util;
import nl.q42.javahueapi.HueService;
import nl.q42.javahueapi.models.Light;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.IBinder;
import android.view.View;
import android.widget.RemoteViews;

public class WidgetUpdateService extends Service {
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		final AppWidgetManager widgetManager = AppWidgetManager.getInstance(getApplicationContext());
		final int[] widgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
		
		// Fetch current light state and update UI
		new AsyncTask<Void, Void, Map<String, Light>>() {
			@Override
			protected Map<String, Light> doInBackground(Void... params) {
				HueService service = new HueService("192.168.1.101", "aValidUser");
				
				try {
					return service.getFullConfig().lights;
				} catch (Exception e) {
					// TODO: Handle network error
					return null;
				}
			}
			
			@Override
			protected void onPostExecute(Map<String, Light> lights) {
				if (lights == null) return;
				
				for (int i = 0; i < widgetIds.length; i++) {
					int widgetId = widgetIds[i];
					
					RemoteViews views = new RemoteViews(getPackageName(), R.layout.widget);
					
					// Swap loading spinner with content
					views.setViewVisibility(R.id.widget_spinner, View.GONE);
					views.setViewVisibility(R.id.widget_content, View.VISIBLE);
					
					// Add event handlers
					views.setOnClickPendingIntent(R.id.widget_light1_button, createToggleIntent(WidgetUpdateService.this, widgetIds, "1"));
					views.setOnClickPendingIntent(R.id.widget_light2_button, createToggleIntent(WidgetUpdateService.this, widgetIds, "2"));
					views.setOnClickPendingIntent(R.id.widget_light3_button, createToggleIntent(WidgetUpdateService.this, widgetIds, "3"));
					views.setOnClickPendingIntent(R.id.widget_light4_button, createToggleIntent(WidgetUpdateService.this, widgetIds, "4"));
					
					// Update views
					views.setTextViewText(R.id.widget_light1_name, lights.get("1").name);
					views.setTextColor(R.id.widget_light1_name, lights.get("1").state.on ? Color.WHITE : Color.rgb(101, 101, 101));
					views.setInt(R.id.widget_light1_color, "setBackgroundColor", Util.getRGBColor(lights.get("1")));
					views.setInt(R.id.widget_light1_indicator, "setBackgroundResource", lights.get("1").state.on ? R.drawable.appwidget_settings_ind_on_c_holo : R.drawable.appwidget_settings_ind_off_c_holo);
					
					views.setTextViewText(R.id.widget_light2_name, lights.get("2").name);
					views.setTextColor(R.id.widget_light2_name, lights.get("2").state.on ? Color.WHITE : Color.rgb(101, 101, 101));
					views.setInt(R.id.widget_light2_color, "setBackgroundColor", Util.getRGBColor(lights.get("2")));
					views.setInt(R.id.widget_light2_indicator, "setBackgroundResource", lights.get("2").state.on ? R.drawable.appwidget_settings_ind_on_c_holo : R.drawable.appwidget_settings_ind_off_c_holo);
					
					views.setTextViewText(R.id.widget_light3_name, lights.get("3").name);
					views.setTextColor(R.id.widget_light3_name, lights.get("3").state.on ? Color.WHITE : Color.rgb(101, 101, 101));
					views.setInt(R.id.widget_light3_color, "setBackgroundColor", Util.getRGBColor(lights.get("3")));
					views.setInt(R.id.widget_light3_indicator, "setBackgroundResource", lights.get("3").state.on ? R.drawable.appwidget_settings_ind_on_c_holo : R.drawable.appwidget_settings_ind_off_c_holo);
					
					views.setTextViewText(R.id.widget_light4_name, lights.get("4").name);
					views.setTextColor(R.id.widget_light4_name, lights.get("4").state.on ? Color.WHITE : Color.rgb(101, 101, 101));
					views.setInt(R.id.widget_light4_color, "setBackgroundColor", Util.getRGBColor(lights.get("4")));
					views.setInt(R.id.widget_light4_indicator, "setBackgroundResource", lights.get("4").state.on ? R.drawable.appwidget_settings_ind_on_c_holo : R.drawable.appwidget_settings_ind_off_c_holo);
					
					widgetManager.updateAppWidget(widgetId, views);
				}
				
				stopSelf();
			}
		}.execute();
		
		return START_STICKY;
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

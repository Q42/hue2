package nl.q42.hue2.widgets;

import nl.q42.hue2.R;
import nl.q42.hue2.Util;
import nl.q42.javahueapi.HueService;
import nl.q42.javahueapi.models.Light;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.IBinder;
import android.widget.RemoteViews;

public class WidgetService extends Service {
	@Override
	public int onStartCommand(final Intent intent, int flags, int startId) {
		final AppWidgetManager widgetManager = AppWidgetManager.getInstance(getApplicationContext());
		final int[] widgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
		final String id = intent.getStringExtra("light");
		
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				try {
					HueService service = new HueService("192.168.1.101", "aValidUser");
					
					// Toggle light
					Light light = service.getLightDetails(id);
					light.state.on = !light.state.on;
					service.turnLightOn(id, light.state.on);
					
					int lightColor = Util.getRGBColor(light);
					
					// Update widgets
					for (int wid : widgetIds) {
						RemoteViews views = new RemoteViews(getPackageName(), R.layout.widget);
						
						if (id.equals("1")) {
							views.setTextColor(R.id.widget_light1_name, light.state.on ? Color.WHITE : Color.rgb(101, 101, 101));
							views.setInt(R.id.widget_light1_color, "setBackgroundColor", lightColor);
							views.setInt(R.id.widget_light1_indicator, "setBackgroundResource", light.state.on ? R.drawable.appwidget_settings_ind_on_c_holo : R.drawable.appwidget_settings_ind_off_c_holo);
						} else if (id.equals("2")) {
							views.setTextColor(R.id.widget_light2_name, light.state.on ? Color.WHITE : Color.rgb(101, 101, 101));
							views.setInt(R.id.widget_light2_color, "setBackgroundColor", lightColor);
							views.setInt(R.id.widget_light2_indicator, "setBackgroundResource", light.state.on ? R.drawable.appwidget_settings_ind_on_c_holo : R.drawable.appwidget_settings_ind_off_c_holo);
						} else if (id.equals("3")) {
							views.setTextColor(R.id.widget_light3_name, light.state.on ? Color.WHITE : Color.rgb(101, 101, 101));
							views.setInt(R.id.widget_light3_color, "setBackgroundColor", lightColor);
							views.setInt(R.id.widget_light3_indicator, "setBackgroundResource", light.state.on ? R.drawable.appwidget_settings_ind_on_c_holo : R.drawable.appwidget_settings_ind_off_c_holo);
						} else {
							views.setTextColor(R.id.widget_light4_name, light.state.on ? Color.WHITE : Color.rgb(101, 101, 101));
							views.setInt(R.id.widget_light4_color, "setBackgroundColor", lightColor);
							views.setInt(R.id.widget_light4_indicator, "setBackgroundResource", light.state.on ? R.drawable.appwidget_settings_ind_on_c_holo : R.drawable.appwidget_settings_ind_off_c_holo);
						}
						
						widgetManager.updateAppWidget(wid, views);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				return null;
			}
			
			@Override
			protected void onPostExecute(Void result) {
				stopSelf();
			}
		}.execute();
		
		return START_STICKY;
	}
	
	// Not used, but required to be implemented
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}

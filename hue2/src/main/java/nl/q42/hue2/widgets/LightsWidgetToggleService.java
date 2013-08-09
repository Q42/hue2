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

public class LightsWidgetToggleService extends Service {
	@Override
	public int onStartCommand(final Intent intent, int flags, int startId) {
		final AppWidgetManager widgetManager = AppWidgetManager.getInstance(getApplicationContext());
		final String ip = intent.getStringExtra("ip");
		final String id = intent.getStringExtra("light");
		final int widget = intent.getIntExtra("widget", -1);
		final int button = intent.getIntExtra("button", -1);
		
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				try {
					HueService service = new HueService(ip, Util.getDeviceIdentifier(getApplicationContext()));
					
					// Toggle light
					Light light = service.getLightDetails(id);
					if (!light.state.reachable) return null; // Ignore command if light is unreachable anyway
					
					light.state.on = !light.state.on;
					service.turnLightOn(id, light.state.on);
					
					// Update button
					RemoteViews views = new RemoteViews(getPackageName(), R.layout.widget_lights);
					
					int idName = getResources().getIdentifier("widget_lights_light" + button + "_name", "id", getPackageName());
					int idColor = getResources().getIdentifier("widget_lights_light" + button + "_color", "id", getPackageName());
					int idIndicator = getResources().getIdentifier("widget_lights_light" + button + "_indicator", "id", getPackageName());
					
					views.setTextViewText(idName, light.name);
					views.setTextColor(idName, light.state.on ? Color.WHITE : Color.rgb(101, 101, 101));
					views.setInt(idColor, "setBackgroundColor", Util.getRGBColor(light));
					views.setInt(idIndicator, "setBackgroundResource", light.state.on ? R.drawable.appwidget_settings_ind_on_c_holo : R.drawable.appwidget_settings_ind_off_c_holo);
					
					widgetManager.updateAppWidget(widget, views);
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
		
		return START_NOT_STICKY;
	}
	
	// Not used, but required to be implemented
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}

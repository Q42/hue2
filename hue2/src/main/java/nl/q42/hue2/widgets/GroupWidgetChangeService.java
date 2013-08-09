package nl.q42.hue2.widgets;

import java.util.ArrayList;

import nl.q42.hue2.R;
import nl.q42.hue2.Util;
import nl.q42.hue2.models.Preset;
import nl.q42.javahueapi.HueService;
import nl.q42.javahueapi.models.FullConfig;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.widget.RemoteViews;

public class GroupWidgetChangeService extends Service {
	@SuppressWarnings("unchecked")
	@Override
	public int onStartCommand(final Intent intent, int flags, int startId) {
		final AppWidgetManager widgetManager = AppWidgetManager.getInstance(getApplicationContext());
		final int[] widgetIds = widgetManager.getAppWidgetIds(new ComponentName(this, GroupWidgetProvider.class));
		final String ip = intent.getStringExtra("ip");
		final String id = intent.getStringExtra("group");
		final boolean on = intent.getBooleanExtra("on", false);
		final Preset preset = (Preset) intent.getSerializableExtra("preset");
		final ArrayList<Preset> presets = (ArrayList<Preset>) intent.getSerializableExtra("presets");
		final int widget = intent.getIntExtra("widget", -1);
		
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				try {
					HueService service = new HueService(ip, Util.getDeviceIdentifier(getApplicationContext()));
					
					// Toggle group
					if (preset == null) {
						service.turnGroupOn(id, on);
					} else {
						if (preset.color_mode.equals("xy")) {
							service.setGroupXY(id, preset.xy, preset.brightness);
						} else {
							service.setGroupCT(id, (int) preset.ct, preset.brightness);
						}
					}
					
					FullConfig cfg = service.getFullConfig();
					
					// Update button
					RemoteViews views = new RemoteViews(getPackageName(), R.layout.widget_group);
					
					GroupWidgetUpdateService.updateWidget(GroupWidgetChangeService.this, widgetIds, widget, views, id, cfg.groups.get(id), presets, cfg.lights, ip);
					
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

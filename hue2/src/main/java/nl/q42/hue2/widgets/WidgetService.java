package nl.q42.hue2.widgets;

import nl.q42.hue2.R;
import nl.q42.javahueapi.HueService;
import nl.q42.javahueapi.models.FullConfig;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.IBinder;
import android.view.View;
import android.widget.RemoteViews;

public class WidgetService extends Service {
	@Override
	public int onStartCommand(final Intent intent, int flags, int startId) {
		final AppWidgetManager widgetManager = AppWidgetManager.getInstance(getApplicationContext());
		final int[] widgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
		
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				try {
					HueService service = new HueService("192.168.1.101", "aValidUser");
					
					FullConfig cfg = service.getFullConfig();
					boolean newState = !cfg.lights.get("1").state.on;
					
					service.turnGroupOn("0", newState);
					
					// Update widgets
					for (int id : widgetIds) {
						RemoteViews views = new RemoteViews(getPackageName(), R.layout.widget);
						views.setViewVisibility(R.id.widget_indicator, newState ? View.VISIBLE : View.GONE);
						views.setTextColor(R.id.widget_name, newState ? Color.WHITE : Color.rgb(101, 101, 101));
						
						widgetManager.updateAppWidget(id, views);
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

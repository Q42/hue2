package nl.q42.hue2.widgets;

import nl.q42.hue2.R;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class WidgetProvider extends AppWidgetProvider {
	// TODO: Create configuration page instead of hardcoding lights
	// TODO: Handle disconnect, unreachable lights and lights no longer existing
	// TODO: Progress bar for initialization (as initialLayout)
	public void onUpdate(Context context, AppWidgetManager widgetManager, int[] widgetIds) {
		for (int i = 0; i < widgetIds.length; i++) {
			int widgetId = widgetIds[i];
			
			RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
			
			// Add event handlers
			views.setOnClickPendingIntent(R.id.widget_light1_button, createIntent(context, widgetIds, "1"));
			views.setOnClickPendingIntent(R.id.widget_light2_button, createIntent(context, widgetIds, "2"));
			views.setOnClickPendingIntent(R.id.widget_light3_button, createIntent(context, widgetIds, "3"));
			views.setOnClickPendingIntent(R.id.widget_light4_button, createIntent(context, widgetIds, "4"));
			
			// TODO: Show actual state
			// TODO: Periodically update state if screen is on, so light details get is no longer required
			views.setTextViewText(R.id.widget_light1_name, "Light 1");
			views.setInt(R.id.widget_light1_color, "setBackgroundColor", android.graphics.Color.RED);
			views.setTextViewText(R.id.widget_light2_name, "Light 2");
			views.setInt(R.id.widget_light2_color, "setBackgroundColor", android.graphics.Color.GREEN);
			views.setTextViewText(R.id.widget_light3_name, "Light 3");
			views.setInt(R.id.widget_light3_color, "setBackgroundColor", android.graphics.Color.BLUE);
			views.setTextViewText(R.id.widget_light4_name, "Light 4");
			views.setInt(R.id.widget_light4_color, "setBackgroundColor", android.graphics.Color.BLACK);
			
			widgetManager.updateAppWidget(widgetId, views);
		}
	}
	
	private PendingIntent createIntent(Context context, int[] widgetIds, String light) {
		Intent intent = new Intent(context, WidgetService.class);
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds);
		intent.putExtra("light", light);
		PendingIntent pendingIntent = PendingIntent.getService(context, light.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
		
		return pendingIntent;
	}
}

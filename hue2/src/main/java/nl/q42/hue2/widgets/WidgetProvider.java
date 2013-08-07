package nl.q42.hue2.widgets;

import nl.q42.hue2.R;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class WidgetProvider extends AppWidgetProvider {
	public void onUpdate(Context context, AppWidgetManager widgetManager, int[] widgetIds) {		
		for (int i = 0; i < widgetIds.length; i++) {
			int widgetId = widgetIds[i];
			
			Intent intent = new Intent(context, WidgetService.class);
			intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds);
			PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
			
			// Toggle light on clicking widget
			RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
			views.setOnClickPendingIntent(R.id.widget_container, pendingIntent);
			
			widgetManager.updateAppWidget(widgetId, views);
		}
	}
}

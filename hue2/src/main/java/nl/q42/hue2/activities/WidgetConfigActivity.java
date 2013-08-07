package nl.q42.hue2.activities;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

// TODO: Allow selection of up to 4 (5?) lights and use those in widget
public class WidgetConfigActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Bundle extras = getIntent().getExtras();
		final int widgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
		
		Button but = new Button(this);
		setContentView(but);
		
		but.setText("CREATE");
		
		but.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent result = new Intent();
				result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
				setResult(RESULT_OK, result);
				finish();
			}
		});
	}
}

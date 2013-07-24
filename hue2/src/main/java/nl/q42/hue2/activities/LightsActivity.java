package nl.q42.hue2.activities;

import java.util.Map;

import nl.q42.hue2.R;
import nl.q42.hue2.Util;
import nl.q42.hue2.models.Bridge;
import nl.q42.javahueapi.HueService;
import nl.q42.javahueapi.models.Light;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class LightsActivity extends Activity {
	private Bridge bridge;
	private HueService service;
	private Map<String, Light> lights;
	
	private ImageButton refreshButton;
	private ProgressBar loadingSpinner;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_lights);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		// Set up loading UI elements		
		ActionBar ab = getActionBar();
		ab.setCustomView(R.layout.loader);
		ab.setDisplayShowCustomEnabled(true);
		
		RelativeLayout loadingLayout = (RelativeLayout) ab.getCustomView();

		loadingSpinner = (ProgressBar) loadingLayout.findViewById(R.id.loader_spinner);
		refreshButton = (ImageButton) loadingLayout.findViewById(R.id.loader_refresh);
		
		// Set up from bridge info
		// TODO: Save instance state
		bridge = (Bridge) getIntent().getSerializableExtra("bridge");
		service = new HueService(bridge.getIp(), Util.getDeviceIdentifier(this));
		
		setTitle(bridge.getName());
		
		// Loading lights
		getLights();
	}
	
	private void getLights() {
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				try {
					lights = service.getLights();
				} catch (Exception e) {
					// TODO: Handle network errors
				}
				return null;
			}

			@Override
			protected void onPostExecute(Void params) {
				populateList();
			}
		}.execute();
	}
	
	private void populateList() {
		ViewGroup container = (ViewGroup) findViewById(R.id.lights_list);
		View lastView = null;
		
		for (final String id : lights.keySet()) {
			lastView = getLayoutInflater().inflate(R.layout.lights_light, container, false);
			Light light = lights.get(id);
			
			((TextView) lastView.findViewById(R.id.lights_light_name)).setText(light.name);
			
			container.addView(lastView);
		}
		
		if (lastView != null) {
			lastView.findViewById(R.id.lights_light_divider).setVisibility(View.INVISIBLE);
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			Intent searchIntent = new Intent(this, LinkActivity.class);
			searchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(searchIntent);
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}
}

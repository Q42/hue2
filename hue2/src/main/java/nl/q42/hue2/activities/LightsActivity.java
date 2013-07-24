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
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Switch;
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
					// getLights() returns no state info
					lights = service.getFullConfig().lights;
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
			
			// Convert HSV color to RGB
			float[] components = new float[] {
				(float) light.state.hue / (float) 65535.0f * 360.0f,
				(float) light.state.sat / 255.0f,
				(float) light.state.bri / 255.0f
			};
			int color = Color.HSVToColor(components);
			
			// If a light is off, display the color as black (state seems to be unreliable then anyway)
			if (!light.state.on) {
				color = 0;
			}
			
			// Display info in UI
			lastView.findViewById(R.id.lights_light_color).setBackgroundColor(color);
			((TextView) lastView.findViewById(R.id.lights_light_name)).setText(light.name);
			((Switch) lastView.findViewById(R.id.lights_light_switch)).setChecked(light.state.on);
			
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

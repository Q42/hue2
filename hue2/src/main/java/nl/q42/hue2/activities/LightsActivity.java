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
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

public class LightsActivity extends Activity {
	private Bridge bridge;
	private HueService service;
	private Map<String, Light> lights; // TODO: Update this state instead of manipulating switches directly
	
	private LinearLayout resultContainer;
	private ImageButton refreshButton;
	private ProgressBar loadingSpinner;
	
	private boolean switchingAll = false; // TODO: Remove monkey patching
	
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
		
		resultContainer = (LinearLayout) findViewById(R.id.lights_result_container);
		
		setEventHandlers();
		
		// Set up from bridge info
		// TODO: Save instance state
		bridge = (Bridge) getIntent().getSerializableExtra("bridge");
		service = new HueService(bridge.getIp(), Util.getDeviceIdentifier(this));
		
		// Save bridge to reconnect later
		Util.setLastBridge(this, bridge);
		
		setTitle(bridge.getName());
		
		// Loading lights
		getLights();
	}
	
	private void setEventHandlers() {
		// All lights pseudo group
		final Switch switchAll = (Switch) findViewById(R.id.lights_all_switch);
		switchAll.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton view, final boolean checked) {
				new AsyncTask<Void, Void, Boolean>() {
					@Override
					protected void onPreExecute() {
						switchAll.setEnabled(false);
					}
					
					@Override
					protected Boolean doInBackground(Void... params) {
						try {
							service.turnAllOn(checked);
							return true;
						} catch (Exception e) {
							// TODO: Handle network error
							return false;
						}
					}
					
					@Override
					protected void onPostExecute(Boolean result) {
						switchAll.setEnabled(true);
						
						if (result) {
							ViewGroup lightViews = (ViewGroup) findViewById(R.id.lights_list);
							
							switchingAll = true;
							for (int i = 0; i < lightViews.getChildCount(); i++) {
								((Switch) lightViews.getChildAt(i).findViewById(R.id.lights_light_switch)).setChecked(checked);
							}
							switchingAll = false;
						} else {
							// Revert switch
							switchAll.setChecked(!checked);
						}
					}
				}.execute();
			}
		});
	}
	
	private void getLights() {
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected void onPreExecute() {
				resultContainer.setVisibility(View.INVISIBLE);
			}
			
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
				resultContainer.setVisibility(View.VISIBLE);
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
			final float[] components = new float[] {
				(float) light.state.hue / (float) 65535.0f * 360.0f,
				(float) light.state.sat / 255.0f,
				255.0f // Ignore brightness for more clear color view, hue is most important anyway
			};
			int color = Color.HSVToColor(components);
			
			// If a light is off, display the color as black (state seems to be unreliable then anyway)
			if (!light.state.on) {
				color = 0;
			}
			
			// Set up UI
			((TextView) lastView.findViewById(R.id.lights_light_name)).setText(light.name);
			
			final View colorView = lastView.findViewById(R.id.lights_light_color);
			colorView.setBackgroundColor(color);
			
			final Switch switchView = (Switch) lastView.findViewById(R.id.lights_light_switch);
			switchView.setChecked(light.state.on);
			switchView.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton view, final boolean checked) {
					if (switchingAll) {
						colorView.setBackgroundColor(checked ? Color.HSVToColor(components) : Color.BLACK);
						return;
					}
					
					new AsyncTask<Void, Void, Boolean>() {
						@Override
						protected void onPreExecute() {
							switchView.setEnabled(false);
						}
						
						@Override
						protected Boolean doInBackground(Void... params) {
							try {
								service.turnLightOn(id, checked);
								return true;
							} catch (Exception e) {
								// TODO: Handle network error
								return false;
							}
						}
						
						@Override
						protected void onPostExecute(Boolean result) {
							switchView.setEnabled(true);
							
							if (result) {
								if (checked) {
									// TODO: Refresh color?
									colorView.setBackgroundColor(Color.HSVToColor(components));
								} else {
									colorView.setBackgroundColor(Color.BLACK);
								}
							} else {
								// Revert switch
								switchView.setChecked(!checked);
							}
						}
					}.execute();
				}
			});
			
			container.addView(lastView);
		}
		
		if (lastView != null) {
			lastView.findViewById(R.id.lights_light_divider).setVisibility(View.INVISIBLE);
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			Util.setLastBridge(this, null);
			
			Intent searchIntent = new Intent(this, LinkActivity.class);
			searchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(searchIntent);
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}
}

package nl.q42.hue2.activities;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import nl.q42.hue.dialogs.ErrorDialog;
import nl.q42.hue.dialogs.ErrorDialog.ErrorDialogCallback;
import nl.q42.hue2.R;
import nl.q42.hue2.Util;
import nl.q42.hue2.models.Bridge;
import nl.q42.hue2.views.FeedbackSwitch;
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
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

// TODO: State system
// 1. User clicks switch
// 2. UI is disabled
// 3. Request is sent
// 4a. If successful, the state is updated and the UI is updated and enabled
// 4b. If failed, show network error and re-enable UI

public class LightsActivity extends Activity {
	private Bridge bridge;
	private HueService service;
	
	private Map<String, Light> lights = new HashMap<String, Light>();
	private Map<String, View> lightViews = new HashMap<String, View>();
	
	private LinearLayout resultContainer;
	private LinearLayout resultList;
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
		
		resultContainer = (LinearLayout) findViewById(R.id.lights_result_container);
		resultList = (LinearLayout) findViewById(R.id.lights_list);
		
		setEventHandlers();
		
		// Set up from bridge info
		// TODO: Save instance state
		bridge = (Bridge) getIntent().getSerializableExtra("bridge");
		service = new HueService(bridge.getIp(), Util.getDeviceIdentifier(this));
		
		// Save bridge to reconnect later
		Util.setLastBridge(this, bridge);
		
		setTitle(bridge.getName());
		
		// Loading lights
		refreshState();
	}
	
	private void setEventHandlers() {
		refreshButton.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				refreshState();
			}
		});
		
		// All lights pseudo group
		final FeedbackSwitch switchAll = (FeedbackSwitch) findViewById(R.id.lights_all_switch);
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
							return false;
						}
					}
					
					@Override
					protected void onPostExecute(Boolean result) {
						switchAll.setEnabled(true);
						
						if (result) {
							ViewGroup lightViews = (ViewGroup) findViewById(R.id.lights_list);
							
							for (int i = 0; i < lightViews.getChildCount(); i++) {
								((FeedbackSwitch) lightViews.getChildAt(i).findViewById(R.id.lights_light_switch)).setChecked(checked, true);
							}
						} else {
							// Revert switch
							switchAll.setChecked(!checked, true);
							
							ErrorDialog.showNetworkError(getFragmentManager());
						}
					}
				}.execute();
			}
		});
	}
	
	/**
	 * Enable/disable all switches (use while executing actions or refreshing state)
	 */
	private Timer indicatorTimer = new Timer();
	private void setActivityIndicator(boolean enabled, boolean forced) {		
		if (enabled) {
			// Tasks shorter than 300 ms don't warrant a visual loading indicator
			indicatorTimer = new Timer();
			indicatorTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					refreshButton.post(new Runnable() {
						@Override
						public void run() {
							refreshButton.setVisibility(View.GONE);
							loadingSpinner.setVisibility(View.VISIBLE);
						}
					});
				}
			}, forced ? 0 : 300);
		} else {
			indicatorTimer.cancel();
			
			refreshButton.setVisibility(View.VISIBLE);
			loadingSpinner.setVisibility(View.GONE);
		}
	}
	
	/**
	 * Reflect local lights state in UI
	 */
	private void refreshViews() {
		for (String key : lightViews.keySet()) {
			View view = lightViews.get(key);
			Light light = lights.get(key);
			
			((TextView) view.findViewById(R.id.lights_light_name)).setText(light.name);
			
			// Set background of light icon to light color
			final View colorView = view.findViewById(R.id.lights_light_color);
			colorView.setBackgroundColor(getRGBColor(light));
			
			// Set switch
			((FeedbackSwitch) view.findViewById(R.id.lights_light_switch)).setChecked(light.state.on, true);
		}
	}
	
	/**
	 * Download fresh copy of light state from bridge
	 */
	private void refreshState() {		
		new AsyncTask<Void, Void, Boolean>() {
			@Override
			protected void onPreExecute() {
				// Empty state
				lights.clear();
				lightViews.clear();
				resultList.removeAllViews();
				
				resultContainer.setVisibility(View.INVISIBLE);
				
				setActivityIndicator(true, true);
			}
			
			@Override
			protected Boolean doInBackground(Void... params) {
				try {
					// getLights() returns no state info
					lights = service.getFullConfig().lights;
					return true;
				} catch (Exception e) {
					return false;
				}
			}

			@Override
			protected void onPostExecute(Boolean success) {
				if (success) {
					populateList();
					resultContainer.setVisibility(View.VISIBLE);
				} else {
					// Being able to retrieve the light list is critical, so if this fails we go back to the bridge selection activity
					ErrorDialog.show(getFragmentManager(), R.string.dialog_connection_title, R.string.dialog_network_error, new ErrorDialogCallback() {
						@Override
						public void onClose() {
							Util.setLastBridge(LightsActivity.this, null);
							
							Intent searchIntent = new Intent(LightsActivity.this, LinkActivity.class);
							searchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
							startActivity(searchIntent);
						}
					});
				}
				
				setActivityIndicator(false, true);
			}
		}.execute();
	}
	
	private void populateList() {
		ViewGroup container = (ViewGroup) findViewById(R.id.lights_list);
		View lastView = null;
		
		for (final String id : lights.keySet()) {
			Light light = lights.get(id);
			
			// Create view
			lastView = addLightView(container, id, light);
			
			// Associate view with light
			lightViews.put(id, lastView);
		}
		
		if (lastView != null) {
			lastView.findViewById(R.id.lights_light_divider).setVisibility(View.INVISIBLE);
		}
		
		// Populate UI with state
		refreshViews();
	}
	
	private int getRGBColor(Light light) {
		// Convert HSV color to RGB
		final float[] components = new float[] {
			(float) light.state.hue / (float) 65535.0f * 360.0f,
			(float) light.state.sat / 255.0f,
			1.0f // Ignore brightness for more clear color view, hue is most important anyway
		};
		
		int color = light.state.on ? Color.HSVToColor(components) : Color.BLACK;
		
		return color;
	}
	
	private View addLightView(ViewGroup container, final String id, final Light light) {
		View view = getLayoutInflater().inflate(R.layout.lights_light, container, false);
		
		// Set switch event handler
		final FeedbackSwitch switchView = (FeedbackSwitch) view.findViewById(R.id.lights_light_switch);
		switchView.setChecked(light.state.on, true);
		switchView.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton view, final boolean checked) {					
				new AsyncTask<Void, Void, Boolean>() {
					@Override
					protected void onPreExecute() {
						setActivityIndicator(true, false);
						switchView.setEnabled(false);
					}
					
					@Override
					protected Boolean doInBackground(Void... params) {
						try {
							service.turnLightOn(id, checked);
							return true;
						} catch (Exception e) {
							return false;
						}
					}
					
					@Override
					protected void onPostExecute(Boolean result) {
						setActivityIndicator(false, false);
						switchView.setEnabled(true);
						
						// Toggle successful
						// TODO: Refresh color state when turned on
						if (result) {
							lights.get(id).state.on = !lights.get(id).state.on;
						} else {
							ErrorDialog.showNetworkError(getFragmentManager());
						}
						
						refreshViews();
					}
				}.execute();
			}
		});
		
		container.addView(view);
		
		return view;
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

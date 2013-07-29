package nl.q42.hue2.activities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import nl.q42.hue.dialogs.ColorDialog;
import nl.q42.hue.dialogs.ErrorDialog;
import nl.q42.hue.dialogs.PresetRemoveDialog;
import nl.q42.hue2.PHUtilitiesImpl;
import nl.q42.hue2.PresetsDataSource;
import nl.q42.hue2.R;
import nl.q42.hue2.Util;
import nl.q42.hue2.models.Bridge;
import nl.q42.hue2.models.Preset;
import nl.q42.hue2.views.ColorButton;
import nl.q42.hue2.views.FeedbackSwitch;
import nl.q42.javahueapi.HueService;
import nl.q42.javahueapi.models.Light;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class LightsActivity extends Activity {
	// It takes extremely long for the server to update its data, so this interval is reasonable
	private final static long REFRESH_INTERVAL = 5000;
	
	private Bridge bridge;
	private HueService service;
	
	private HashMap<String, Light> lights = new HashMap<String, Light>();
	private HashMap<String, View> lightViews = new HashMap<String, View>();
	
	private LinearLayout resultContainer;
	private LinearLayout resultList;
	private ImageButton refreshButton;
	private ProgressBar loadingSpinner;
	
	// Database operations are simple, so they can be run in UI thread
	private PresetsDataSource datasource;
	private Map<String, List<Preset>> presets;
	
	private Timer refreshTimer = new Timer();
	
	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_lights);
		
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
		
		// Open color preset database
		datasource = new PresetsDataSource(this);
		datasource.open();
		
		// Check if bridge info was passed
		if (getIntent().hasExtra("bridge")) {
			bridge = (Bridge) getIntent().getSerializableExtra("bridge");
			Util.setLastBridge(this, bridge);
		} else if (Util.getLastBridge(this) != null) {
			bridge = Util.getLastBridge(this);
		} else {
			// No last bridge saved and no passed bridge, return to bridge search activity
			Intent searchIntent = new Intent(this, LinkActivity.class);
			searchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(searchIntent);
			return;
		}
		
		// Load presets (fast enough to do on UI thread for now)
		presets = datasource.getAllPresets(bridge);
		
		// Set up bridge info
		service = new HueService(bridge.getIp(), Util.getDeviceIdentifier(this));
		setTitle(bridge.getName());
		
		// Loading lights
		if (savedInstanceState == null) {
			refreshState(true);
		} else {
			lights = (HashMap<String, Light>) savedInstanceState.getSerializable("lights");
			populateList();
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.lights, menu);
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_bridges) {
			Util.setLastBridge(this, null);
			
			Intent searchIntent = new Intent(this, LinkActivity.class);
			searchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(searchIntent);
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	protected void onSaveInstanceState(Bundle state) {
		super.onSaveInstanceState(state);
		
		state.putSerializable("lights", lights);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		datasource.close();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		refreshTimer.cancel();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		startRefreshTimer();
	}
	
	private void startRefreshTimer() {
		refreshTimer = new Timer();
		refreshTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				resultContainer.post(new Runnable() {
					@Override
					public void run() {
						refreshState(false);
					}
				});
			}
		}, REFRESH_INTERVAL, REFRESH_INTERVAL);
	}
	
	private void setEventHandlers() {
		refreshButton.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				refreshState(true);
			}
		});
		
		// All lights pseudo group buttons
		OnClickListener listener = new OnClickListener() {
			@Override
			public void onClick(final View v) {
				final boolean checked = v.getId() == R.id.lights_all_on;
				
				new AsyncTask<Void, Void, Boolean>() {
					@Override
					protected void onPreExecute() {
						setActivityIndicator(true, false);
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
						setActivityIndicator(false, false);
						
						// Toggle successful
						if (result) {
							for (String id : lights.keySet()) {
								lights.get(id).state.on = checked;
							}
						} else {
							ErrorDialog.showNetworkError(getFragmentManager());
						}
						
						refreshViews();
					}
				}.execute();
			}
		};
		
		findViewById(R.id.lights_all_on).setOnClickListener(listener);
		findViewById(R.id.lights_all_off).setOnClickListener(listener);
	}
	
	/**
	 * Enable/disable all switches (use while executing actions or refreshing state)
	 */
	private Timer indicatorTimer = new Timer();
	private void setActivityIndicator(boolean enabled, boolean forced) {		
		if (enabled) {
			// Tasks shorter than 300 ms don't warrant a visual loading indicator
			if (!forced) {
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
				}, 300);
			} else {
				refreshButton.setVisibility(View.GONE);
				loadingSpinner.setVisibility(View.VISIBLE);
			}
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
		for (final String id : lightViews.keySet()) {
			View view = lightViews.get(id);
			Light light = lights.get(id);
			
			((TextView) view.findViewById(R.id.lights_light_name)).setText(light.name);
			
			// Set background of light icon to light color
			final View colorView = view.findViewById(R.id.lights_light_color);
			colorView.setBackgroundColor(Util.getRGBColor(light));
			
			// Set switch
			((FeedbackSwitch) view.findViewById(R.id.lights_light_switch)).setCheckedCode(light.state.on);
			
			// Add preset buttons - if there are any presets	
			if (presets.containsKey(id)) {
				LinearLayout presetsView = (LinearLayout) view.findViewById(R.id.lights_light_presets);
				presetsView.removeAllViews();
				
				for (final Preset preset : presets.get(id)) {
					ColorButton presetBut = (ColorButton) getLayoutInflater().inflate(R.layout.lights_preset_button, presetsView, false);
					
					presetBut.setColor(PHUtilitiesImpl.colorFromXY(preset.xy, light.modelid));
					
					presetBut.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							setLightColor(id, preset.xy, preset.brightness);
						}
					});
					
					presetBut.setOnLongClickListener(new OnLongClickListener() {
						@Override
						public boolean onLongClick(View v) {
							PresetRemoveDialog.newInstance(preset, lights.get(id)).show(getFragmentManager(), "dialog_remove_preset");
							return true;
						}
					});
					
					presetsView.addView(presetBut);
				}
			}
			
			if (presets.containsKey(id) && presets.get(id).size() > 0) {
				view.findViewById(R.id.lights_light_scroller).setVisibility(View.VISIBLE);
			} else {
				view.findViewById(R.id.lights_light_scroller).setVisibility(View.GONE);
			}
		}
	}
	
	/**
	 * Download fresh copy of light state from bridge
	 */
	private void refreshState(final boolean flush) {		
		new AsyncTask<Void, Void, Boolean>() {
			@Override
			protected void onPreExecute() {
				// Empty state
				if (flush) {
					lights.clear();
					lightViews.clear();
					resultList.removeAllViews();
					
					resultContainer.setVisibility(View.INVISIBLE);
					
					setActivityIndicator(true, true);
				}
			}
			
			@Override
			protected Boolean doInBackground(Void... params) {
				try {
					// getLights() returns no state info
					lights = new HashMap<String, Light>(service.getFullConfig().lights);
					return true;
				} catch (Exception e) {
					return false;
				}
			}

			@Override
			protected void onPostExecute(Boolean success) {
				if (success) {
					if (flush) {
						populateList();
						resultContainer.setVisibility(View.VISIBLE);
					} else {
						refreshViews();
					}
				} else if (flush) {
					// Being able to retrieve the light list is critical, so if this fails we go back to the bridge selection activity
					ErrorDialog.show(getFragmentManager(), R.string.dialog_connection_title, R.string.dialog_network_error);
				}
				
				setActivityIndicator(false, true);
			}
		}.execute();
	}
	
	private void populateList() {
		ViewGroup container = (ViewGroup) findViewById(R.id.lights_list);
		View lastView = null;
		
		// Sort lights by id
		ArrayList<String> lightIds = new ArrayList<String>();
		for (String id : lights.keySet()) {
			lightIds.add(id);
		}
		Collections.sort(lightIds);
		
		for (final String id : lightIds) {
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
	
	private View addLightView(ViewGroup container, final String id, final Light light) {
		View view = getLayoutInflater().inflate(R.layout.lights_light, container, false);
		
		// Set color picker event handler
		view.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				ColorDialog dialog = ColorDialog.newInstance(id, lights.get(id)); // Make sure to get the latest data
				dialog.show(getFragmentManager(), "dialog_color");
			}
		});
		
		// Set switch event handler
		final FeedbackSwitch switchView = (FeedbackSwitch) view.findViewById(R.id.lights_light_switch);
		switchView.setCheckedCode(light.state.on);
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
						if (result) {
							lights.get(id).state.on = checked;
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
	
	public void addColorPreset(final String id, final float[] xy, final int bri) {
		int db_id = datasource.insertPreset(bridge.getSerial(), id, xy, bri);
		
		if (!presets.containsKey(id)) {
			presets.put(id, new ArrayList<Preset>());
		}
		presets.get(id).add(new Preset(db_id, id, xy, bri));
		
		refreshViews();
	}
	
	public void removeColorPreset(Preset preset) {
		datasource.removePreset(preset);
		
		presets.get(preset.light).remove(preset);
		
		refreshViews();
	}
	
	public void setLightColor(final String id, final float[] xy, final int bri) {		
		new AsyncTask<Void, Void, Boolean>() {
			@Override
			protected void onPreExecute() {
				setActivityIndicator(true, false);
			}
			
			@Override
			protected Boolean doInBackground(Void... params) {
				try {					
					service.setLightXY(id, xy, bri);
					return true;
				} catch (Exception e) {
					return false;
				}
			}
			
			@Override
			protected void onPostExecute(Boolean result) {
				setActivityIndicator(false, false);
				
				// Toggle successful
				if (result) {
					Light light = lights.get(id);
					light.state.on = true;
					light.state.colormode = "xy";
					light.state.xy = new double[] { xy[0], xy[1] };
					light.state.bri = bri;
				} else {
					ErrorDialog.showNetworkError(getFragmentManager());
				}
				
				refreshViews();
			}
		}.execute();
	}
}
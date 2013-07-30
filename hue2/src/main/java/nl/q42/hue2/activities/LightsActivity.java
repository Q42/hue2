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
import nl.q42.hue.dialogs.GroupRemoveDialog;
import nl.q42.hue.dialogs.LightEditDialog;
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
import nl.q42.javahueapi.models.FullConfig;
import nl.q42.javahueapi.models.Group;
import nl.q42.javahueapi.models.Light;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
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
	private HashMap<String, ArrayList<View>> lightViews = new HashMap<String, ArrayList<View>>();
	
	private HashMap<String, Group> groups = new HashMap<String, Group>();
	private HashMap<String, View> groupViews = new HashMap<String, View>();
	
	private LinearLayout resultContainer;
	private LinearLayout groupResultList;
	private LinearLayout lightResultList;
	private ImageButton refreshButton;
	private ProgressBar loadingSpinner;
	
	// Database operations are simple, so they can be run in UI thread
	private PresetsDataSource datasource;
	private Map<String, List<Preset>> lightPresets;
	private Map<String, List<Preset>> groupPresets;
	
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
		groupResultList = (LinearLayout) findViewById(R.id.lights_groups_list);
		lightResultList = (LinearLayout) findViewById(R.id.lights_lights_list);
		
		refreshButton.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				refreshState(true);
			}
		});
		
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
		lightPresets = datasource.getLightPresets(bridge);
		groupPresets = datasource.getGroupPresets(bridge);
		
		// Set up bridge info
		service = new HueService(bridge.getIp(), Util.getDeviceIdentifier(this));
		setTitle(bridge.getName());
		
		// Loading lights
		if (savedInstanceState == null) {
			refreshState(true);
		} else {
			lights = (HashMap<String, Light>) savedInstanceState.getSerializable("lights");
			groups = (HashMap<String, Group>) savedInstanceState.getSerializable("groups");
			populateViews();
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
		state.putSerializable("groups", groups);
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
	 * Reflect local lights and groups state in UI
	 */
	private void refreshViews() {
		if (configurationChanged()) {
			repopulateViews();
		} else {
			refreshGroups();
			refreshLights();
		}
	}
	
	/**
	 * Force all views to not just be refreshed, but be recreated
	 */
	private void repopulateViews() {
		lightViews.clear();
		lightResultList.removeAllViews();
		
		groupViews.clear();
		groupResultList.removeAllViews();
		
		populateViews();
	}
	
	/**
	 * Check if groups were modified externally
	 */
	private boolean configurationChanged() {
		// Cross-check groups
		for (String id : groups.keySet()) if (!groupViews.containsKey(id)) return true;
		for (String id : groupViews.keySet()) if (!groups.containsKey(id)) return true;
		
		// Cross-check lights
		for (String id : lights.keySet()) if (!lightViews.containsKey(id)) return true;
		for (String id : lightViews.keySet()) if (!lights.containsKey(id)) return true;
		
		return false;
	}
	
	private void refreshGroups() {
		for (final String id : groupViews.keySet()) {
			View view = groupViews.get(id);
			Group group = groups.get(id);
			
			((TextView) view.findViewById(R.id.lights_group_name)).setText(group.name);
			
			// Add preset buttons - if there are any presets	
			if (groupPresets.containsKey(id)) {
				LinearLayout presetsView = (LinearLayout) view.findViewById(R.id.lights_group_presets);
				presetsView.removeAllViews();
				
				for (final Preset preset : groupPresets.get(id)) {
					ColorButton presetBut = (ColorButton) getLayoutInflater().inflate(R.layout.lights_preset_button, presetsView, false);
					
					presetBut.setColor(PHUtilitiesImpl.colorFromXY(preset.xy, null));
					
					presetBut.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							setGroupColor(id, preset.xy, preset.brightness);
						}
					});
					
					presetBut.setOnLongClickListener(new OnLongClickListener() {
						@Override
						public boolean onLongClick(View v) {
							PresetRemoveDialog.newInstance(preset).show(getFragmentManager(), "dialog_remove_preset");
							return true;
						}
					});
					
					presetsView.addView(presetBut);
				}
			}
			
			if (groupPresets.containsKey(id) && groupPresets.get(id).size() > 0) {
				view.findViewById(R.id.lights_group_scroller).setVisibility(View.VISIBLE);
			} else {
				view.findViewById(R.id.lights_group_scroller).setVisibility(View.GONE);
			}
		}
	}
	
	private void refreshLights() {
		for (final String id : lightViews.keySet()) {
			ArrayList<View> views = lightViews.get(id);
			Light light = lights.get(id);
			
			for (View view : views) {
				view.setEnabled(light.state.reachable);
				
				TextView nameView = (TextView) view.findViewById(R.id.lights_light_name);
				nameView.setText(light.name);
				nameView.setTextColor(light.state.reachable ? Color.WHITE : Color.GRAY);
				
				// Set background of light icon to light color
				final View colorView = view.findViewById(R.id.lights_light_color);
				colorView.setBackgroundColor(light.state.reachable ? Util.getRGBColor(light) : Color.BLACK);
				
				// Set switch
				FeedbackSwitch switchView = (FeedbackSwitch) view.findViewById(R.id.lights_light_switch);
				switchView.setEnabled(light.state.reachable);
				switchView.setCheckedCode(light.state.reachable && light.state.on);
				
				// Add preset buttons - if there are any presets	
				if (lightPresets.containsKey(id)) {
					LinearLayout presetsView = (LinearLayout) view.findViewById(R.id.lights_light_presets);
					presetsView.removeAllViews();
					
					for (final Preset preset : lightPresets.get(id)) {
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
				
				if (lightPresets.containsKey(id) && lightPresets.get(id).size() > 0) {
					view.findViewById(R.id.lights_light_scroller).setVisibility(View.VISIBLE);
				} else {
					view.findViewById(R.id.lights_light_scroller).setVisibility(View.GONE);
				}
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
					lightResultList.removeAllViews();
					
					groups.clear();
					groupViews.clear();
					groupResultList.removeAllViews();
					
					resultContainer.setVisibility(View.INVISIBLE);
					
					setActivityIndicator(true, true);
				}
			}
			
			@Override
			protected Boolean doInBackground(Void... params) {
				try {
					FullConfig cfg = service.getFullConfig();
					
					lights = new HashMap<String, Light>(cfg.lights);
					groups = new HashMap<String, Group>(cfg.groups);
					
					// Add pseudo group with all lights
					Group allGroup = new Group();
					allGroup.name = getString(R.string.lights_group_all);
					allGroup.lights = new ArrayList<String>(lights.keySet());
					groups.put("0", allGroup);
					
					return true;
				} catch (Exception e) {
					return false;
				}
			}

			@Override
			protected void onPostExecute(Boolean success) {
				if (success) {
					if (flush) {
						populateViews();
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
	
	private void populateViews() {
		populateGroupList();
		populateGroups();
		refreshViews();
	}
	
	private void populateGroupList() {
		View lastView = null;
		
		// Sort groups by id
		ArrayList<String> groupIds = new ArrayList<String>();
		for (String id : groups.keySet()) {
			groupIds.add(id);
		}
		Collections.sort(groupIds);
		
		for (final String id : groupIds) {
			Group group = groups.get(id);
			
			// Create view
			lastView = addGroupListView(groupResultList, id, group);
			
			// Associate view with group
			groupViews.put(id, lastView);
		}
		
		if (lastView != null) {
			lastView.findViewById(R.id.lights_group_divider).setVisibility(View.INVISIBLE);
		}
	}
	
	private void populateGroups() {
		// Sort groups by id
		ArrayList<String> groupIds = new ArrayList<String>();
		for (String id : groups.keySet()) {
			groupIds.add(id);
		}
		Collections.sort(groupIds);
		
		// Build sorted list of all lights 
		ArrayList<String> otherLights = new ArrayList<String>();
		for (String id : lights.keySet()) {
			otherLights.add(id);
		}
		Collections.sort(otherLights);
		
		// For each group, add a header and the lights		
		for (final String id : groupIds) {
			Group group = groups.get(id);
			if (id.equals("0")) continue;
			
			otherLights.removeAll(group.lights);
			
			// Create view
			addGroupView(groupResultList, id, group);
		}
		
		// Create group with any remaining lights
		if (otherLights.size() > 0) {
			Group otherGroup = new Group();
			otherGroup.lights = otherLights;
			
			if (otherLights.size() == lights.size()) {
				otherGroup.name = getString(R.string.lights_group_other_only);
			} else {
				otherGroup.name = getString(R.string.lights_group_other);
			}
			
			addGroupView(groupResultList, null, otherGroup);
		}
	}
	
	private View addGroupView(ViewGroup container, final String id, final Group group) {
		View view = getLayoutInflater().inflate(R.layout.lights_group_container, container, false);
		
		((TextView) view.findViewById(R.id.lights_group_container_title)).setText(group.name);
		LinearLayout lightList = (LinearLayout) view.findViewById(R.id.lights_group_container_list);
		
		// Sort lights in group by id
		ArrayList<String> lightIds = new ArrayList<String>();
		for (String lid : group.lights) {
			lightIds.add(lid);
		}
		Collections.sort(lightIds);
		
		// Create and add view for all lights
		View lastView = null;
		
		for (final String lid : lightIds) {
			Light light = lights.get(lid);
			
			// Create view
			lastView = addLightView(lightList, lid, light);
			
			// Associate view with light
			if (!lightViews.containsKey(lid)) {
				lightViews.put(lid, new ArrayList<View>());
			}
			lightViews.get(lid).add(lastView);
		}
		
		if (lastView != null) {
			lastView.findViewById(R.id.lights_light_divider).setVisibility(View.INVISIBLE);
		}
		
		container.addView(view);
		
		return view;
	}
	
	private View addGroupListView(ViewGroup container, final String id, final Group group) {
		View view = getLayoutInflater().inflate(R.layout.lights_group, container, false);
		
		// Set color picker event handler
		view.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				ColorDialog dialog = ColorDialog.newInstance(id, groups.get(id)); // Make sure to get the latest data
				dialog.show(getFragmentManager(), "dialog_color");
			}
		});
		
		// Set group remove handler
		if (!id.equals("0")) {
			view.setOnLongClickListener(new OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					GroupRemoveDialog dialog = GroupRemoveDialog.newInstance(id, groups.get(id));
					dialog.show(getFragmentManager(), "dialog_remove_group");
					
					return true;
				}
			});
		}
		
		// Set on/off button event handlers
		OnClickListener listener = new OnClickListener() {
			@Override
			public void onClick(final View v) {
				final boolean checked = v.getId() == R.id.lights_group_on;
				
				new AsyncTask<Void, Void, Boolean>() {
					@Override
					protected void onPreExecute() {
						setActivityIndicator(true, false);
					}
					
					@Override
					protected Boolean doInBackground(Void... params) {
						try {
							service.turnGroupOn(id, checked);
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
							for (String lid : groups.get(id).lights) {
								lights.get(lid).state.on = checked;
							}
						} else {
							ErrorDialog.showNetworkError(getFragmentManager());
						}
						
						refreshViews();
					}
				}.execute();
			}
		};
		
		view.findViewById(R.id.lights_group_on).setOnClickListener(listener);
		view.findViewById(R.id.lights_group_off).setOnClickListener(listener);
		
		container.addView(view);
		
		return view;
	}
	
	private View addLightView(ViewGroup container, final String id, final Light light) {
		View view = getLayoutInflater().inflate(R.layout.lights_light, container, false);
		
		// Set color picker event handler
		view.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {				
				ColorDialog dialog = ColorDialog.newInstance(id, lights.get(id));
				dialog.show(getFragmentManager(), "dialog_color");
			}
		});
		
		// Set name changing event handler
		view.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				LightEditDialog dialog = LightEditDialog.newInstance(id, lights.get(id));
				dialog.show(getFragmentManager(), "dialog_edit_light");
				
				return true;
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
	
	public void addLightPreset(final String id, final float[] xy, final int bri) {
		int db_id = datasource.insertPreset(bridge.getSerial(), id, null, xy, bri);
		
		if (!lightPresets.containsKey(id)) {
			lightPresets.put(id, new ArrayList<Preset>());
		}
		lightPresets.get(id).add(new Preset(db_id, id, null, xy, bri));
		
		refreshViews();
	}
	
	public void addGroupPreset(final String id, final float[] xy, final int bri) {
		int db_id = datasource.insertPreset(bridge.getSerial(), null, id, xy, bri);
		
		if (!groupPresets.containsKey(id)) {
			groupPresets.put(id, new ArrayList<Preset>());
		}
		groupPresets.get(id).add(new Preset(db_id, null, id, xy, bri));
		
		refreshViews();
	}
	
	public void removeColorPreset(Preset preset) {
		datasource.removePreset(preset);
		
		if (preset.light != null) {
			lightPresets.get(preset.light).remove(preset);
		} else {
			groupPresets.get(preset.group).remove(preset);
		}
		
		refreshViews();
	}
	
	public void setGroupColor(final String id, final float[] xy, final int bri) {
		new AsyncTask<Void, Void, Boolean>() {
			@Override
			protected void onPreExecute() {
				setActivityIndicator(true, false);
			}
			
			@Override
			protected Boolean doInBackground(Void... params) {
				try {					
					service.setGroupXY(id, xy, bri);
					return true;
				} catch (Exception e) {
					return false;
				}
			}
			
			@Override
			protected void onPostExecute(Boolean result) {
				setActivityIndicator(false, false);
				
				// Set successful, update state
				if (result) {
					for (String lid : groups.get(id).lights) {
						Light light = lights.get(lid);
						
						light.state.on = true;
						light.state.colormode = "xy";
						light.state.xy = new double[] { xy[0], xy[1] };
						light.state.bri = bri;
					}
				} else {
					ErrorDialog.showNetworkError(getFragmentManager());
				}
				
				refreshViews();
			}
		}.execute();
	}
	
	public void removeGroup(final String id) {
		new AsyncTask<Void, Void, Boolean>() {
			@Override
			protected void onPreExecute() {
				setActivityIndicator(true, true);
			}
			
			@Override
			protected Boolean doInBackground(Void... params) {
				try {					
					service.removeGroup(id);					
					return true;
				} catch (Exception e) {
					return false;
				}
			}
			
			@Override
			protected void onPostExecute(Boolean result) {
				setActivityIndicator(false, true);
				
				// Set successful, update state
				if (result) {
					groups.remove(id);
					
					groupPresets.remove(id);
					datasource.removePresetsGroup(id);
					
					repopulateViews();
				} else {
					ErrorDialog.showNetworkError(getFragmentManager());
				}
			}
		}.execute();
	}
	
	public void setLightName(final String id, final String name) {
		new AsyncTask<Void, Void, Boolean>() {
			@Override
			protected void onPreExecute() {
				setActivityIndicator(true, false);
			}
			
			@Override
			protected Boolean doInBackground(Void... params) {
				try {					
					service.setLightName(id, name);
					return true;
				} catch (Exception e) {
					return false;
				}
			}
			
			@Override
			protected void onPostExecute(Boolean result) {
				setActivityIndicator(false, false);
				
				// Set successful, update state
				if (result) {
					lights.get(id).name = name;
				} else {
					ErrorDialog.showNetworkError(getFragmentManager());
				}
				
				refreshViews();
			}
		}.execute();
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
				
				// Set successful, update state
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

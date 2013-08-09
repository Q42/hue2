package nl.q42.hue2.activities;

import java.util.HashMap;

import nl.q42.hue2.R;
import nl.q42.hue2.Util;
import nl.q42.hue2.models.Bridge;
import nl.q42.hue2.widgets.GroupWidgetProvider;
import nl.q42.javahueapi.HueService;
import nl.q42.javahueapi.models.Group;
import android.app.ActionBar;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class GroupWidgetConfigActivity extends Activity {
	private Bridge bridge;
	private HueService service;
	
	private RelativeLayout loader, abLoader;
	private LinearLayout content;
	private RadioGroup groupsList;
	private Button createButton;
	
	private HashMap<String, Group> groups = new HashMap<String, Group>();
	
	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_widget_group_config);
		
		ActionBar ab = getActionBar();
		ab.setCustomView(R.layout.loader);
		ab.setDisplayShowCustomEnabled(true);
		
		abLoader = (RelativeLayout) ab.getCustomView();
		abLoader.findViewById(R.id.loader_refresh).setVisibility(View.GONE);
		abLoader.findViewById(R.id.loader_spinner).setVisibility(View.VISIBLE);
		
		loader = (RelativeLayout) findViewById(R.id.widget_group_config_loader);
		content = (LinearLayout) findViewById(R.id.widget_group_config_content);
		groupsList = (RadioGroup) findViewById(R.id.widget_group_config_groups);
		
		createButton = (Button) findViewById(R.id.widget_group_config_create);
		createButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				createWidget();
			}
		});
		
		// Select a group from the bridge currently used in the main app
		bridge = Util.getLastBridge(this);
		
		if (bridge == null) {
			Toast.makeText(this, getString(R.string.widget_config_error_bridge), Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
		
		service = new HueService(bridge.getIp(), Util.getDeviceIdentifier(this));
		
		// Load list of groups
		if (savedInstanceState == null) {
			loadGroups();
		} else {
			groups = (HashMap<String, Group>) savedInstanceState.getSerializable("groups");
			addGroups();
		}
		
		// Make sure this is set unless the create button is pressed
		setResult(RESULT_CANCELED);
	}
	
	@Override
	protected void onSaveInstanceState(Bundle state) {
		super.onSaveInstanceState(state);
		
		state.putSerializable("groups", groups);
	}
	
	private void createWidget() {
		String groupId = ((RadioButton) groupsList.findViewById(groupsList.getCheckedRadioButtonId())).getHint().toString();
		
		Bundle extras = getIntent().getExtras();
		int widgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
		
		AppWidgetManager widgetManager = AppWidgetManager.getInstance(this);
		ComponentName widget = new ComponentName(getPackageName(), GroupWidgetConfigActivity.class.getName());
		int[] widgetIds = widgetManager.getAppWidgetIds(widget);
		
		// Store the configuration for this widget
		SharedPreferences.Editor prefsEdit = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
		prefsEdit.putString("widget_" + widgetId + "_ip", bridge.getIp());
		prefsEdit.putString("widget_" + widgetId + "_id", groupId);
		prefsEdit.commit();
		
		// Send initial update request
		Intent initialUpdate = new Intent(this, GroupWidgetProvider.class);
		initialUpdate.setAction("android.appwidget.action.APPWIDGET_UPDATE");
		initialUpdate.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds);
		sendBroadcast(initialUpdate);
		
		Intent result = new Intent();
		result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
		setResult(RESULT_OK, result);
		finish();
	}
	
	private void loadGroups() {
		new AsyncTask<Void, Void, Boolean>() {
			@Override
			protected Boolean doInBackground(Void... params) {
				try {
					groups = new HashMap<String, Group>(service.getGroups());
				} catch (Exception e) {
					return false;
				}
				
				return true;
			}
			
			@Override
			protected void onPostExecute(Boolean result) {
				if (result) {					
					addGroups();
				} else {
					Toast.makeText(
							GroupWidgetConfigActivity.this,
							getString(R.string.widget_config_error_connection),
							Toast.LENGTH_SHORT).show();
					
					finish();
				}
			}
		}.execute();
	}
	
	private void addGroups() {
		View lastView = null;
		
		for (String id : Util.getSortedGroups(groups)) {
			RadioButton rb = (RadioButton) getLayoutInflater().inflate(R.layout.widget_group_config_group, groupsList, false);
			
			if (id.equals("0")) {
				rb.setChecked(true);
				rb.setText(R.string.widget_group_config_all_lights);
			} else {
				rb.setText(groups.get(id).name);
			}
			
			// Put id in unused property
			rb.setId(id.hashCode());
			rb.setHint(id);
			
			groupsList.addView(rb);
			
			// Divider has to be added separately, because RadioGroup dislikes nested RadioButtons
			lastView = getLayoutInflater().inflate(R.layout.widget_group_config_divider, groupsList, false);
			groupsList.addView(lastView);
		}
		
		if (lastView != null) {
			lastView.setVisibility(View.GONE);
		}
		
		abLoader.findViewById(R.id.loader_spinner).setVisibility(View.GONE);
		loader.setVisibility(View.GONE);
		content.setVisibility(View.VISIBLE);
	}
}

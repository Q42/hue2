package nl.q42.hue2.activities;

import java.util.ArrayList;
import java.util.HashMap;

import nl.q42.hue2.PHUtilitiesImpl;
import nl.q42.hue2.R;
import nl.q42.hue2.Util;
import nl.q42.hue2.dialogs.GroupLightDialog;
import nl.q42.hue2.dialogs.GroupRemoveDialog;
import nl.q42.hue2.views.ColorButton;
import nl.q42.hue2.views.HueSlider;
import nl.q42.hue2.views.SatBriSlider;
import nl.q42.hue2.views.TempSlider;
import nl.q42.javahueapi.models.Group;
import nl.q42.javahueapi.models.Light;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class GroupActivity extends Activity {
	private Group group;
	private String id;
	private HashMap<String, Light> lights;
	
	private EditText nameView;
	private Button lightsButton;
	private LinearLayout colorPicker;
	private HueSlider hueSlider;
	private SatBriSlider satBriSlider;
	private TempSlider tempSlider;
	private ColorButton presetColorView;
	
	private String colorMode;
	
	@SuppressWarnings("unchecked")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		ActionBar ab = getActionBar();
		ab.setDisplayHomeAsUpEnabled(true);
		ab.setDisplayShowTitleEnabled(false);
		
		// Group details
		group = (Group) getIntent().getSerializableExtra("group");
		id = getIntent().getStringExtra("id");
		lights = (HashMap<String, Light>) getIntent().getSerializableExtra("lights");
		
		if (group == null) {
			group = new Group();
			group.lights = new ArrayList<String>();
		}
		
		// UI setup
		setContentView(R.layout.activity_group);
		
		((TextView) findViewById(R.id.group_header)).setText(id == null ? R.string.group_new : R.string.group_group);
		
		nameView = (EditText) findViewById(R.id.group_name);
		lightsButton = (Button) findViewById(R.id.group_lights);
		colorPicker = (LinearLayout) findViewById(R.id.group_color_picker);
		hueSlider = (HueSlider) findViewById(R.id.group_color_hue);
		satBriSlider = (SatBriSlider) findViewById(R.id.group_color_sat_bri);
		tempSlider = (TempSlider) findViewById(R.id.group_color_temp);
		
		hueSlider.setSatBriSlider(satBriSlider);
		tempSlider.setSliders(hueSlider, satBriSlider);
		
		presetColorView = (ColorButton) findViewById(R.id.group_preset_color);
		
		// Set listeners for color slider interaction to record last used color mode (hue/sat or temperature)
		// and to send preview requests
		tempSlider.setOnTouchListener(getColorModeListener("ct"));
		hueSlider.setOnTouchListener(getColorModeListener("xy"));
		satBriSlider.setOnTouchListener(getColorModeListener("xy"));
		
		// Save preset button
		findViewById(R.id.group_save_preset).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				saveGroup(true);
				finish();
			}
		});
		
		// Create group button
		findViewById(R.id.group_create).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				saveGroup(false);
				finish();
			}
		});
		
		// If this is the all lights pseudo group, only the color can be changed
		if ("0".equals(id)) {
			nameView.setEnabled(false);
			lightsButton.setEnabled(false);
		} else if (id == null) {
			// Or if a new group is being created, no color can be set yet
			colorPicker.setVisibility(View.GONE);
		}
		
		// Add lights button event handler
		lightsButton.setText(getLightsList());
		lightsButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				GroupLightDialog.newInstance(lights, group).show(getFragmentManager(), "dialog_lights");
			}
		});
		
		// Switch buttons when creating group
		if (id == null) {
			findViewById(R.id.group_save).setVisibility(View.GONE);
			findViewById(R.id.group_create).setVisibility(View.VISIBLE);
		}
		
		if (savedInstanceState == null) {
			nameView.setText(id == null ? "" : group.name);
		}
		
		colorMode = "xy";
		updatePresetPreview();
	}
	
	@Override
	public void onBackPressed() {
		if (id != null) saveGroup(false);
		super.onBackPressed();
	}
	
	private OnTouchListener getColorModeListener(final String mode) {
		return new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {				
				colorMode = mode;
				updatePresetPreview();
				return false;
			}
		};
	}
	
	private void updatePresetPreview() {
		if (colorMode.equals("ct")) {
			presetColorView.setColor(Util.temperatureToColor(1000000 / (int) tempSlider.getTemp()));
		} else {
			float[] xy = PHUtilitiesImpl.calculateXY(satBriSlider.getResultColor(), null);
			presetColorView.setColor(PHUtilitiesImpl.colorFromXY(xy, null));
		}
	}
	
	private String getLightsList() {
		String lightsStr = "";
		for (int i = 0; i < group.lights.size(); i++) {
			lightsStr += lights.get(group.lights.get(i)).name;
			if (i < group.lights.size() - 1) lightsStr += ", ";
		}
		return lightsStr;
	}
	
	// Called by GroupRemoveDialog after confirmation
	public void removeGroup() {
		Intent result = new Intent();
		result.putExtra("id", id);
		result.putExtra("remove", true);
		
		setResult(RESULT_OK, result);
		finish();
	}
	
	// Called by GroupLightDialog after confirmation
	public void setLights(ArrayList<String> lights) {
		group.lights = lights;
		lightsButton.setText(getLightsList());
	}
	
	private void saveGroup(boolean addPreset) {
		ArrayList<String> groupLights = new ArrayList<String>();
		groupLights.addAll(group.lights);
		
		if (id != null) {
			float[] xy = PHUtilitiesImpl.calculateXY(satBriSlider.getResultColor(), null);
			int bri = (int) (satBriSlider.getBrightness() * 255.0f);
			int ct = (int) tempSlider.getTemp();
			
			Intent result = new Intent();
			result.putExtra("id", id);
			result.putExtra("name", nameView.getText().toString().trim());
			result.putExtra("lights", groupLights);
			result.putExtra("mode", colorMode);
			result.putExtra("xy", xy);
			result.putExtra("ct", ct);
			result.putExtra("bri", bri);
			
			if (addPreset) result.putExtra("addPreset", true);
			
			// If the color sliders registered touch events, we know the color has been changed (easier than conversion and checking)
			result.putExtra("colorChanged", hueSlider.hasUserSet() || satBriSlider.hasUserSet() || tempSlider.hasUserSet());
			
			setResult(RESULT_OK, result);
		} else {
			Intent result = new Intent();
			result.putExtra("name", nameView.getText().toString().trim());
			result.putExtra("lights", groupLights);
			
			setResult(RESULT_OK, result);
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.group, menu);
	    
	    // Pseudo group with all lights (or a new group) cannot be removed
	    menu.findItem(R.id.menu_delete_group).setVisible(id != null && !id.equals("0"));
	    
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_delete_group) {
			GroupRemoveDialog.newInstance().show(getFragmentManager(), "dialog_remove_group");
			return true;
		} else if (item.getItemId() == R.id.menu_cancel) {
			finish();			
			return true;
		} else if (item.getItemId() == android.R.id.home) {
			if (id != null) saveGroup(false);
			finish();
			
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}
}

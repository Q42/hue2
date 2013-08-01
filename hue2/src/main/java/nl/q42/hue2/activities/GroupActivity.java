package nl.q42.hue2.activities;

import nl.q42.hue2.PHUtilitiesImpl;
import nl.q42.hue2.R;
import nl.q42.hue2.dialogs.GroupRemoveDialog;
import nl.q42.hue2.views.HueSlider;
import nl.q42.hue2.views.SatBriSlider;
import nl.q42.hue2.views.TempSlider;
import nl.q42.javahueapi.models.Group;
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

public class GroupActivity extends Activity {
	private Group group;
	private String id;
	
	private EditText nameView;
	private Button lightsButton;
	private LinearLayout colorPicker;
	private HueSlider hueSlider;
	private SatBriSlider satBriSlider;
	private TempSlider tempSlider;
	
	private String colorMode;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Group details
		group = (Group) getIntent().getSerializableExtra("group");
		id = getIntent().getStringExtra("id");
		
		// UI setup
		setContentView(R.layout.activity_group);
		setTitle(id == null ? getString(R.string.group_new) : group.name);
		
		nameView = (EditText) findViewById(R.id.group_name);
		lightsButton = (Button) findViewById(R.id.group_lights);
		colorPicker = (LinearLayout) findViewById(R.id.group_color_picker);
		hueSlider = (HueSlider) findViewById(R.id.group_color_hue);
		satBriSlider = (SatBriSlider) findViewById(R.id.group_color_sat_bri);
		tempSlider = (TempSlider) findViewById(R.id.group_color_temp);
		
		hueSlider.setSatBriSlider(satBriSlider);
		tempSlider.setSliders(hueSlider, satBriSlider);
		
		// Set listeners for color slider interaction to record last used color mode (hue/sat or temperature)
		// and to send preview requests
		tempSlider.setOnTouchListener(getColorModeListener("ct"));
		hueSlider.setOnTouchListener(getColorModeListener("xy"));
		satBriSlider.setOnTouchListener(getColorModeListener("xy"));
		
		// Fill in current name/color in UI or restore previous
		if (savedInstanceState == null) {
			nameView.setText(id == null ? "" : group.name);
		}
		
		// If this is the all lights pseudo group, only the color can be changed
		if ("0".equals(id)) {
			nameView.setEnabled(false);
			lightsButton.setEnabled(false);
		} else if (id == null) {
			// Or if a new group is being created, no color can be set yet
			colorPicker.setVisibility(View.GONE);
		}
		
		// Add cancel event handler
		findViewById(R.id.group_cancel).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		
		// Add save/create event handler
		Button applyButton = (Button) findViewById(R.id.group_apply);
		applyButton.setText(id == null ? R.string.group_create : R.string.group_save);
		applyButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (id != null) {
					float[] xy = PHUtilitiesImpl.calculateXY(satBriSlider.getResultColor(), null);
					int bri = (int) (satBriSlider.getBrightness() * 255.0f);
					int ct = (int) tempSlider.getTemp();
					
					Intent result = new Intent();
					result.putExtra("id", id);
					result.putExtra("name", nameView.getText().toString().trim());
					result.putExtra("mode", colorMode);
					result.putExtra("xy", xy);
					result.putExtra("ct", ct);
					result.putExtra("bri", bri);
					
					// If the color sliders registered touch events, we know the color has been changed (easier than conversion and checking)
					result.putExtra("colorChanged", hueSlider.hasUserSet() || satBriSlider.hasUserSet() || tempSlider.hasUserSet());
					
					setResult(RESULT_OK, result);
					finish();
				} else {
					Intent result = new Intent();
					result.putExtra("name", nameView.getText().toString().trim());
					
					setResult(RESULT_OK, result);
					finish();
				}
			}
		});
	}
	
	private OnTouchListener getColorModeListener(final String mode) {
		return new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {				
				colorMode = mode;
				return false;
			}
		};
	}
	
	// Called by GroupRemoveDialog after confirmation
	public void removeGroup() {
		Intent result = new Intent();
		result.putExtra("id", id);
		result.putExtra("remove", true);
		
		setResult(RESULT_OK, result);
		finish();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.group, menu);
	    
	    // TODO: Make presets work for groups being created
	    menu.findItem(R.id.menu_add_preset).setVisible(id != null);
	    
	    // Pseudo group with all lights (or a new group) cannot be removed
	    menu.findItem(R.id.menu_delete_group).setVisible(id != null && !id.equals("0"));
	    
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_add_preset) {
			float[] xy = PHUtilitiesImpl.calculateXY(satBriSlider.getResultColor(), null);
			int bri = (int) (satBriSlider.getBrightness() * 255.0f);
			
			Intent result = new Intent();
			result.putExtra("addPreset", true);
			result.putExtra("id", id);
			result.putExtra("xy", xy);
			result.putExtra("bri", bri);
			
			setResult(RESULT_OK, result);
			finish();
			
			return true;
		} else if (item.getItemId() == R.id.menu_delete_group) {
			GroupRemoveDialog.newInstance().show(getFragmentManager(), "dialog_remove_group");
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}
}

package nl.q42.hue2.activities;

import java.util.Timer;
import java.util.TimerTask;

import nl.q42.hue2.PHUtilitiesImpl;
import nl.q42.hue2.R;
import nl.q42.hue2.Util;
import nl.q42.hue2.views.HueSlider;
import nl.q42.hue2.views.SatBriSlider;
import nl.q42.hue2.views.TempSlider;
import nl.q42.javahueapi.HueService;
import nl.q42.javahueapi.models.Light;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.EditText;

public class LightActivity extends Activity {
	private final static int PREVIEW_INTERVAL = 500;
	
	private Light light;
	private String id;
	private HueService service;
	
	private EditText nameView;
	private HueSlider hueSlider;
	private SatBriSlider satBriSlider;
	private TempSlider tempSlider;
	
	private String colorMode;
	
	private Timer colorPreviewTimer = new Timer();
	private boolean previewNeeded = false; // Set to true when color slider is moved
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Light details
		light = (Light) getIntent().getSerializableExtra("light");
		id = getIntent().getStringExtra("id");
		service = (HueService) getIntent().getSerializableExtra("service");
		
		// UI setup
		setContentView(R.layout.activity_light);
		setTitle(light.name);
		
		nameView = (EditText) findViewById(R.id.light_name);
		hueSlider = (HueSlider) findViewById(R.id.light_color_hue);
		satBriSlider = (SatBriSlider) findViewById(R.id.light_color_sat_bri);
		tempSlider = (TempSlider) findViewById(R.id.light_color_temp);
		
		hueSlider.setSatBriSlider(satBriSlider);
		tempSlider.setSliders(hueSlider, satBriSlider);
		
		// Set listeners for color slider interaction to record last used color mode (hue/sat or temperature)
		// and to send preview requests
		tempSlider.setOnTouchListener(getColorModeListener("ct"));
		hueSlider.setOnTouchListener(getColorModeListener("xy"));
		satBriSlider.setOnTouchListener(getColorModeListener("xy"));
		
		// Fill in current name/color in UI or restore previous
		if (savedInstanceState == null) {
			nameView.setText(light.name);
			
			if (light.state.colormode.equals("ct")) {
				tempSlider.setTemp(light.state.ct);
			} else {
				float hsv[] = new float[3];
				Color.colorToHSV(Util.getRGBColor(light), hsv);
				hueSlider.setHue(hsv[0]);
				satBriSlider.setSaturation(hsv[1]);
				satBriSlider.setBrightness(light.state.bri / 255.0f);
			}
		}
		
		// Add cancel event handler
		findViewById(R.id.light_cancel).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				restoreColor();
				finish();
			}
		});
		
		// Add save event handler
		findViewById(R.id.light_save).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				float[] xy = PHUtilitiesImpl.calculateXY(satBriSlider.getResultColor(), light.modelid);
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
			}
		});
	}
	
	@Override
	public void onBackPressed() {
		restoreColor();
		super.onBackPressed();
	}
	
	private void restoreColor() {
		float[] xy;
		if (light.state.colormode.equals("xy")) {
			xy = new float[] { (float) light.state.xy[0], (float) light.state.xy[1] };
		} else {
			xy = PHUtilitiesImpl.calculateXY(Util.getRGBColor(light), light.modelid);
		}
		
		Intent result = new Intent();
		result.putExtra("id", id);
		result.putExtra("name", light.name);
		
		// Original mode may have been hs, but convert that to xy
		result.putExtra("mode", light.state.colormode.equals("ct") ? "ct" : "xy");
		
		result.putExtra("xy", xy);
		result.putExtra("ct", light.state.ct);
		result.putExtra("bri", light.state.bri);
		result.putExtra("colorChanged", true);
		
		setResult(RESULT_OK, result);
	}
	
	@Override
	public void onPause() {
		super.onPause();
		colorPreviewTimer.cancel();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		// Start timer that sends requests for color previews
		colorPreviewTimer = new Timer();
		colorPreviewTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				if (previewNeeded) {
					previewNeeded = false;
					
					try {
						float[] xy = PHUtilitiesImpl.calculateXY(satBriSlider.getResultColor(), light.modelid);
						int bri = (int) (satBriSlider.getBrightness() * 255.0f);
						int ct = (int) tempSlider.getTemp();
						
						if (colorMode.equals("ct")) {
							service.setLightCT(id, ct, bri);
						} else {
							service.setLightXY(id, xy, bri);
						}
					} catch (Exception e) {
						// Don't report exceptions since previewing is a non-essential feature
					}
				}
			}
		}, 0, PREVIEW_INTERVAL);
	}
	
	private OnTouchListener getColorModeListener(final String mode) {
		return new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				colorMode = mode;
				previewNeeded = true;
				return false;
			}
		};
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.light, menu);
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_add_preset) {
			float[] xy = PHUtilitiesImpl.calculateXY(satBriSlider.getResultColor(), light.modelid);
			int bri = (int) (satBriSlider.getBrightness() * 255.0f);
			
			Intent result = new Intent();
			result.putExtra("addPreset", true);
			result.putExtra("id", id);
			result.putExtra("xy", xy);
			result.putExtra("bri", bri);
			
			setResult(RESULT_OK, result);
			finish();
			
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}
}

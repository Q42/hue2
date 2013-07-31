package nl.q42.hue2.activities;

import nl.q42.hue2.PHUtilitiesImpl;
import nl.q42.hue2.R;
import nl.q42.hue2.Util;
import nl.q42.hue2.views.HueSlider;
import nl.q42.hue2.views.SatBriSlider;
import nl.q42.hue2.views.TempSlider;
import nl.q42.javahueapi.models.Light;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;

public class LightActivity extends Activity {
	private Light light;
	private String id;
	
	private EditText nameView;
	private HueSlider hueSlider;
	private SatBriSlider satBriSlider;
	private TempSlider tempSlider;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Light details
		light = (Light) getIntent().getSerializableExtra("light");
		id = getIntent().getStringExtra("id");
		
		// UI setup
		setContentView(R.layout.activity_light);
		setTitle(light.name);
		
		nameView = (EditText) findViewById(R.id.light_name);
		hueSlider = (HueSlider) findViewById(R.id.light_color_hue);
		satBriSlider = (SatBriSlider) findViewById(R.id.light_color_sat_bri);
		tempSlider = (TempSlider) findViewById(R.id.light_color_temp);
		
		hueSlider.setSatBriSlider(satBriSlider);
		tempSlider.setSliders(hueSlider, satBriSlider);
		
		findViewById(R.id.light_cancel).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				setResult(RESULT_CANCELED);
				finish();
			}
		});
		
		findViewById(R.id.light_save).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String model = light != null ? light.modelid : null;
				float[] xy = PHUtilitiesImpl.calculateXY(satBriSlider.getResultColor(), model);
				int bri = (int) (satBriSlider.getBrightness() * 255.0f);
				
				Intent result = new Intent();
				result.putExtra("id", id);
				result.putExtra("name", nameView.getText().toString().trim());
				result.putExtra("xy", xy);
				result.putExtra("bri", bri);
				
				// If the color sliders registered touch events, we know the color has been changed (easier than conversion and checking)
				result.putExtra("colorChanged", hueSlider.hasUserSet() || satBriSlider.hasUserSet() || tempSlider.hasUserSet());
				
				setResult(RESULT_OK, result);
				finish();	
			}
		});
		
		// Fill in current name/color in UI or restore previous
		if (savedInstanceState == null) {
			nameView.setText(light.name);
			
			float hsv[] = new float[3];
			Color.colorToHSV(Util.getRGBColor(light), hsv);
			hueSlider.setHue(hsv[0]);
			satBriSlider.setSaturation(hsv[1]);
			satBriSlider.setBrightness(light.state.bri / 255.0f);
		}
	}
}

package nl.q42.hue2.dialogs;

import nl.q42.hue2.PHUtilitiesImpl;
import nl.q42.hue2.R;
import nl.q42.hue2.Util;
import nl.q42.hue2.activities.LightsActivity;
import nl.q42.hue2.views.HueSlider;
import nl.q42.hue2.views.SatBriSlider;
import nl.q42.hue2.views.TempSlider;
import nl.q42.javahueapi.models.Group;
import nl.q42.javahueapi.models.Light;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

/**
 * Dialog for setting colors and creating presets (for both lights and groups)
 */
public class ColorDialog extends DialogFragment {
	public static ColorDialog newInstance(String id, Light light) {
		ColorDialog dialog = new ColorDialog();
		
		Bundle args = new Bundle();
		args.putSerializable("id", id);
		args.putSerializable("light", light);
		dialog.setArguments(args);
		
		return dialog;
	}
	
	public static ColorDialog newInstance(String id, Group group) {
		ColorDialog dialog = new ColorDialog();
		
		Bundle args = new Bundle();
		args.putSerializable("id", id);
		dialog.setArguments(args);
		
		return dialog;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		View layout = getActivity().getLayoutInflater().inflate(R.layout.dialog_color, null);
		
		// Link sliders to each other
		final HueSlider hueSlider = (HueSlider) layout.findViewById(R.id.color_hue);
		final SatBriSlider satBriSlider = (SatBriSlider) layout.findViewById(R.id.color_sat_bri);
		hueSlider.setSatBriSlider(satBriSlider);
		final TempSlider tempSlider = (TempSlider) layout.findViewById(R.id.color_temp);
		tempSlider.setSliders(hueSlider, satBriSlider);
		
		final Light light = (Light) getArguments().getSerializable("light");
		
		// Fill in current color if editing single light
		if (light != null) {
			float hsv[] = new float[3];
			Color.colorToHSV(Util.getRGBColor(light), hsv);
			hueSlider.setHue(hsv[0]);
			satBriSlider.setSaturation(hsv[1]);
			satBriSlider.setBrightness(light.state.bri / 255.0f);
		}
		
		return new AlertDialog.Builder(getActivity())
			.setTitle(R.string.dialog_color_picker_title)
			.setView(layout)
			
			// Positive - Add a preset
			.setPositiveButton(R.string.dialog_color_add, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String model = light != null ? light.modelid : null;
					float[] xy = PHUtilitiesImpl.calculateXY(satBriSlider.getResultColor(), model);
					int bri = (int) (satBriSlider.getBrightness() * 255.0f);
					
					if (light == null) {
						((LightsActivity) getActivity()).addGroupPreset(getArguments().getString("id"), xy, bri);
					} else {
						((LightsActivity) getActivity()).addLightPreset(getArguments().getString("id"), xy, bri);
					}
				}
			})
			
			// Neutral - Set color instantly
			.setNeutralButton(R.string.dialog_color_set, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String model = light != null ? light.modelid : null;
					float[] xy = PHUtilitiesImpl.calculateXY(satBriSlider.getResultColor(), model);
					int bri = (int) (satBriSlider.getBrightness() * 255.0f);
					
					if (light == null) {
						((LightsActivity) getActivity()).setGroupColor(getArguments().getString("id"), xy, bri);
					} else {
						((LightsActivity) getActivity()).setLightColorXY(getArguments().getString("id"), xy, bri);
					}
				}
			})
			
			// Negative - Cancel
			.setNegativeButton(R.string.dialog_cancel, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
				}
			})
			
			.create();
	}
}

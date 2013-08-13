package nl.q42.huelimitededition.dialogs;

import nl.q42.huelimitededition.PHUtilitiesImpl;
import nl.q42.huelimitededition.R;
import nl.q42.huelimitededition.Util;
import nl.q42.huelimitededition.activities.LightsActivity;
import nl.q42.huelimitededition.models.Preset;
import nl.q42.javahueapi.models.Light;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;

public class PresetRemoveDialog extends DialogFragment {
	public static PresetRemoveDialog newInstance(Preset preset, Light light) {
		PresetRemoveDialog dialog = new PresetRemoveDialog();
		
		Bundle args = new Bundle();
		args.putSerializable("preset", preset);
		args.putSerializable("light", light);
		dialog.setArguments(args);
		
		return dialog;
	}
	
	public static PresetRemoveDialog newInstance(Preset preset) {
		PresetRemoveDialog dialog = new PresetRemoveDialog();
		
		Bundle args = new Bundle();
		args.putSerializable("preset", preset);
		dialog.setArguments(args);
		
		return dialog;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final Preset preset = (Preset) getArguments().getSerializable("preset");
		final Light light = (Light) getArguments().getSerializable("light");
		
		int size = (int) (getResources().getDisplayMetrics().density * 64.0f);
		Bitmap bm = Bitmap.createBitmap(size, size, Config.ARGB_8888);
		
		if (preset.color_mode.equals("xy")) {
			bm.eraseColor(PHUtilitiesImpl.colorFromXY(preset.xy, light != null ? light.modelid : null));
		} else {
			bm.eraseColor(Util.temperatureToColor(1000000 / (int) preset.ct));
		}
		
		return new AlertDialog.Builder(getActivity())
			.setTitle(R.string.dialog_remove_preset_title)
			.setMessage(R.string.dialog_remove_preset)
			.setPositiveButton(R.string.dialog_yes, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					((LightsActivity) getActivity()).removeColorPreset(preset);
				}
			})
			.setIcon(new BitmapDrawable(bm))
			.setNegativeButton(R.string.dialog_no, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
				}
			})
			.create();
	}
}

package nl.q42.hue.dialogs;

import nl.q42.hue2.R;
import nl.q42.hue2.views.HueSlider;
import nl.q42.hue2.views.SatBriSlider;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;

public class ColorDialog extends DialogFragment {
	public static ColorDialog newInstance() {
		// TODO: More arguments
		return new ColorDialog();
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		View layout = getActivity().getLayoutInflater().inflate(R.layout.dialog_color, null);
		
		((HueSlider) layout.findViewById(R.id.color_hue)).setSatBriSlider((SatBriSlider) layout.findViewById(R.id.color_sat_bri));
		
		return new AlertDialog.Builder(getActivity())
			.setTitle(R.string.dialog_color_picker_title)
			.setView(layout)
			.setPositiveButton(R.string.dialog_apply, new Dialog.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO: Set color
				}
			})
			.setNegativeButton(R.string.dialog_cancel, new Dialog.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
				}
			})
			.create();
	}
}

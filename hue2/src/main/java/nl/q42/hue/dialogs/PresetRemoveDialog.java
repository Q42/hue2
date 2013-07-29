package nl.q42.hue.dialogs;

import nl.q42.hue2.R;
import nl.q42.hue2.activities.LightsActivity;
import nl.q42.hue2.models.Preset;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;

public class PresetRemoveDialog extends DialogFragment {
	private Preset preset;
	
	public static PresetRemoveDialog newInstance(Preset preset) {
		PresetRemoveDialog dialog = new PresetRemoveDialog();
		dialog.setPreset(preset);
		return dialog;
	}
	
	public void setPreset(Preset preset) {
		this.preset = preset;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		return new AlertDialog.Builder(getActivity())
			.setTitle(R.string.dialog_remove_preset_title)
			.setMessage(R.string.dialog_remove_preset)
			.setPositiveButton(R.string.dialog_yes, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					((LightsActivity) getActivity()).removeColorPreset(preset);
				}
			})
			.setNegativeButton(R.string.dialog_no, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
				}
			})
			.create();
	}
}

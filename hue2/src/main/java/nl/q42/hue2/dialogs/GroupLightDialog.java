package nl.q42.hue2.dialogs;

import java.util.HashMap;

import nl.q42.hue2.R;
import nl.q42.javahueapi.models.Light;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.LinearLayout;

public class GroupLightDialog extends DialogFragment {
	public static GroupLightDialog newInstance(HashMap<String, Light> lights) {
		GroupLightDialog dialog = new GroupLightDialog();
		
		Bundle args = new Bundle();
		args.putSerializable("lights", lights);
		dialog.setArguments(args);
		
		return dialog;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final HashMap<String, Light> lights = (HashMap<String, Light>) getArguments().getSerializable("lights");
		
		LinearLayout layout = (LinearLayout) getActivity().getLayoutInflater().inflate(R.layout.dialog_lights, null);
		
		// Create checkbox for each light
		for (String id : lights.keySet()) {
			CheckBox lightView = new CheckBox(getActivity());
			lightView.setText(lights.get(id).name);
			lightView.setPadding(0, 20, 0, 20);
			layout.addView(lightView);
		}
		
		return new AlertDialog.Builder(getActivity())
			.setTitle(getString(R.string.dialog_lights_title))
			.setView(layout)
			.setPositiveButton(R.string.dialog_ok, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO: Return selected lights
					dialog.dismiss();
				}
			})
			.setNegativeButton(R.string.dialog_cancel, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
				}
			})
			.create();
	}
}

package nl.q42.hue.dialogs;

import nl.q42.hue2.R;
import nl.q42.hue2.activities.LightsActivity;
import nl.q42.javahueapi.models.Light;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class LightEditDialog extends DialogFragment {
	public static LightEditDialog newInstance(String id, Light light) {
		LightEditDialog dialog = new LightEditDialog();
		
		Bundle args = new Bundle();
		args.putString("id", id);
		args.putSerializable("light", light);
		dialog.setArguments(args);
		
		return dialog;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final String id = getArguments().getString("id");
		final Light light = (Light) getArguments().getSerializable("light");
		
		View layout = getActivity().getLayoutInflater().inflate(R.layout.dialog_light, null);
		
		final EditText nameView = (EditText) layout.findViewById(R.id.dialog_light_name);
		nameView.setText(light.name);
		
		return new AlertDialog.Builder(getActivity())
			.setTitle(R.string.dialog_light_title)
			.setView(layout)
			.setPositiveButton(R.string.dialog_save, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					((LightsActivity) getActivity()).setLightName(id, nameView.getText().toString().trim());
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

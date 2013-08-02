package nl.q42.hue2.dialogs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import nl.q42.hue2.R;
import nl.q42.hue2.activities.GroupActivity;
import nl.q42.javahueapi.models.Group;
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
	private HashMap<String, CheckBox> lightViews = new HashMap<String, CheckBox>();
	private ArrayList<String> lightIds = new ArrayList<String>();
	
	public static GroupLightDialog newInstance(HashMap<String, Light> lights, Group group) {
		GroupLightDialog dialog = new GroupLightDialog();
		
		Bundle args = new Bundle();
		args.putSerializable("group", group);
		args.putSerializable("lights", lights);
		dialog.setArguments(args);
		
		return dialog;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final HashMap<String, Light> lights = (HashMap<String, Light>) getArguments().getSerializable("lights");
		final Group group = (Group) getArguments().getSerializable("group");
		
		LinearLayout layout = (LinearLayout) getActivity().getLayoutInflater().inflate(R.layout.dialog_lights, null);
		
		// Ordered list of lights
		lightIds.addAll(lights.keySet());
		Collections.sort(lightIds);
		
		// Create checkbox for each light
		for (String id : lightIds) {
			CheckBox lightView = new CheckBox(getActivity());
			lightView.setPadding(0, 20, 0, 20);
			
			lightView.setId(id.hashCode()); // Save instance state properly
			lightView.setText(lights.get(id).name);
			lightView.setChecked(group.lights.contains(id));
			
			layout.addView(lightView);
			lightViews.put(id, lightView);
		}
		
		return new AlertDialog.Builder(getActivity())
			.setTitle(getString(R.string.dialog_lights_title))
			.setView(layout)
			.setPositiveButton(R.string.dialog_ok, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					((GroupActivity) getActivity()).setLights(getCheckedLights());
					
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
	
	private ArrayList<String> getCheckedLights() {
		ArrayList<String> lights = new ArrayList<String>();
		
		for (String id : lightIds) {
			if (lightViews.get(id).isChecked()) {
				lights.add(id);
			}
		}
		
		return lights;
	}
}

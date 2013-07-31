package nl.q42.hue2.dialogs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

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
import android.widget.LinearLayout;
import android.widget.ToggleButton;

public class GroupCreateDialog extends DialogFragment {
	private HashMap<String, ToggleButton> lightViews = new HashMap<String, ToggleButton>();
	
	public static GroupCreateDialog newInstance(HashMap<String, Light> lights) {
		GroupCreateDialog dialog = new GroupCreateDialog();
		
		Bundle args = new Bundle();
		args.putSerializable("lights", lights);
		dialog.setArguments(args);
		
		return dialog;
	}
	
	public ArrayList<String> getSelectedLights() {
		ArrayList<String> lightsChecked = new ArrayList<String>();
		
		for (String id : lightViews.keySet()) {
			if (lightViews.get(id).isChecked()) {
				lightsChecked.add(id);
			}
		}
		
		return lightsChecked;
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		outState.putSerializable("selection", getSelectedLights());
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final HashMap<String, Light> lights = (HashMap<String, Light>) getArguments().getSerializable("lights");
		
		View layout = getActivity().getLayoutInflater().inflate(R.layout.dialog_group_create, null);
		
		final EditText nameView = (EditText) layout.findViewById(R.id.dialog_group_create_name);
		
		// Create sorted list of light ids
		ArrayList<String> lightIds = new ArrayList<String>();
		for (String lid : lights.keySet()) {
			lightIds.add(lid);
		}
		Collections.sort(lightIds);
		
		// Add toggle button for each light
		LinearLayout lightList = (LinearLayout) layout.findViewById(R.id.dialog_group_create_lights);
		
		for (String id : lightIds) {
			Light light = lights.get(id);
			
			ToggleButton tb = (ToggleButton) getActivity().getLayoutInflater().inflate(R.layout.group_button, lightList, false);
			
			tb.setSaveEnabled(false);
			
			tb.setTextOn(light.name);
			tb.setTextOff(light.name);
			tb.setText(light.name);
			
			lightViews.put(id, tb);
			lightList.addView(tb);
		}
		
		// Restore state
		if (savedInstanceState != null) {
			ArrayList<String> selection = (ArrayList<String>) savedInstanceState.getSerializable("selection");
			for (String id : selection) {
				lightViews.get(id).setChecked(true);
			}
		}
		
		return new AlertDialog.Builder(getActivity())
			.setTitle(R.string.dialog_group_create_title)
			.setView(layout)
			.setPositiveButton(R.string.dialog_create, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {					
					// Send request to create
					((LightsActivity) getActivity()).createGroup(nameView.getText().toString().trim(), getSelectedLights());
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

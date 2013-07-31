package nl.q42.hue2.dialogs;

import nl.q42.hue2.R;
import nl.q42.hue2.activities.LightsActivity;
import nl.q42.javahueapi.models.Group;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;

// TODO: Merge these into a GroupEditDialog that is used for creation, modification and removal
public class GroupRemoveDialog extends DialogFragment {
	public static GroupRemoveDialog newInstance(String id, Group group) {
		GroupRemoveDialog dialog = new GroupRemoveDialog();
		
		Bundle args = new Bundle();
		args.putString("id", id);
		args.putSerializable("group", group);
		dialog.setArguments(args);
		
		return dialog;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final String id = getArguments().getString("id");
		final Group group = (Group) getArguments().getSerializable("group");
		
		return new AlertDialog.Builder(getActivity())
			.setTitle(group.name)
			.setMessage(R.string.dialog_remove_group)
			.setPositiveButton(R.string.dialog_yes, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					((LightsActivity) getActivity()).removeGroup(id);
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

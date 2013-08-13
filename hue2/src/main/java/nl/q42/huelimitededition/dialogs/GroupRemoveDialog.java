package nl.q42.huelimitededition.dialogs;

import nl.q42.huelimitededition.R;
import nl.q42.huelimitededition.activities.GroupActivity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;

public class GroupRemoveDialog extends DialogFragment {
	public static GroupRemoveDialog newInstance() {
		return new GroupRemoveDialog();
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		return new AlertDialog.Builder(getActivity())
			.setMessage(R.string.dialog_remove_group)
			.setPositiveButton(R.string.dialog_yes, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					((GroupActivity) getActivity()).removeGroup();
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

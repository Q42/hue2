package nl.q42.hue2.dialogs;

import nl.q42.hue2.R;
import nl.q42.hue2.activities.LinkActivity;
import nl.q42.hue2.models.Bridge;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.View;

public class BridgeLinkDialog extends DialogFragment {
	public static BridgeLinkDialog newInstance(Bridge b) {
		BridgeLinkDialog dialog = new BridgeLinkDialog();
		
		Bundle args = new Bundle();
		args.putSerializable("bridge", b);
		dialog.setArguments(args);
		
		return dialog;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final Bridge bridge = (Bridge) getArguments().getSerializable("bridge");
		
		View layout = getActivity().getLayoutInflater().inflate(R.layout.dialog_link, null);
		
		((LinkActivity) getActivity()).startLinkChecker(bridge, this);
		
		return new AlertDialog.Builder(getActivity())
			.setTitle(bridge.getName())
			.setView(layout)
			.setNegativeButton(R.string.dialog_cancel, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
				}
			})
			.create();
	}
	
	@Override
	public void onCancel(DialogInterface dialog) {
		((LinkActivity) getActivity()).stopLinkChecker();
	}
}

package nl.q42.hue2.dialogs;

import nl.q42.hue2.R;
import nl.q42.hue2.activities.LinkActivity;
import nl.q42.hue2.models.Bridge;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class BridgeInfoDialog extends DialogFragment {
	public static BridgeInfoDialog newInstance(Bridge b) {
		BridgeInfoDialog dialog = new BridgeInfoDialog();
		
		Bundle args = new Bundle();
		args.putSerializable("bridge", b);
		dialog.setArguments(args);
		
		return dialog;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {		
		final Bridge bridge = (Bridge) getArguments().getSerializable("bridge");
		
		View layout = getActivity().getLayoutInflater().inflate(R.layout.dialog_bridge, null);
		((TextView) layout.findViewById(R.id.dialog_bridge_ip)).setText(bridge.getIp());
		((TextView) layout.findViewById(R.id.dialog_bridge_mac)).setText(serialToMAC(bridge.getSerial()));
		((TextView) layout.findViewById(R.id.dialog_bridge_software)).setText(bridge.getSoftware());
		
		return new AlertDialog.Builder(getActivity())
			.setTitle(bridge.getName())
			.setView(layout)
			.setPositiveButton(R.string.dialog_bridge_connect, new Dialog.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (bridge.hasAccess()) {
						((LinkActivity) getActivity()).connectToBridge(bridge);
					} else {
						((LinkActivity) getActivity()).showLinkDialog(bridge);		
					}
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
	
	private String serialToMAC(String serial) {
		String mac = "";
		
		for (int i = 0; i < serial.length(); i += 2) {
			mac += serial.substring(i, i + 2);
			if (i < serial.length() - 2) mac += ":";
		}
		
		return mac.toUpperCase();
	}
}

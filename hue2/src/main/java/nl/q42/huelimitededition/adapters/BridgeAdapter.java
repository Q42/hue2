package nl.q42.huelimitededition.adapters;

import nl.q42.huelimitededition.R;
import nl.q42.huelimitededition.models.Bridge;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class BridgeAdapter extends ArrayAdapter<Bridge> {
	private Context context;

	public BridgeAdapter(Context context) {
		super(context, R.layout.link_bridge);
		this.context = context;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView;
		if (convertView == null) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = inflater.inflate(R.layout.link_bridge, parent, false);
		}
		
		Bridge b = getItem(position);
		
		((TextView) view.findViewById(R.id.link_bridge_name)).setText(b.getName());
		
		// Set description based on bridge connectivity
		((TextView) view.findViewById(R.id.link_bridge_description)).setText(b.hasAccess() ? "Ready to connect" : "New bridge");
		
		return view;
	}
}

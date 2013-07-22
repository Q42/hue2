package nl.q42.hue2.activities;

import info.chees.androidhueapi.AndroidHueService;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import nl.q42.hue2.BridgesDataSource;
import nl.q42.hue2.R;
import nl.q42.hue2.models.Bridge;
import nl.q42.javahueapi.HueService;
import nl.q42.javahueapi.models.Light;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

	private final static String STATE_LIGHTS = "stateLights";

	private BridgesDataSource datasource;
	
	private Map<String, Light> lights;

	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		datasource = new BridgesDataSource(this);
	    datasource.open();

	    List<Bridge> bridges = datasource.getAllBridges();
	    if (bridges.size() == 0) {
	    	Intent i = new Intent(this, LinkActivity.class);
	    	startActivity(i);
	    } else {
			if (savedInstanceState != null) {
				lights = (Map<String, Light>) savedInstanceState.getSerializable(STATE_LIGHTS);
				createViews();
			} else {
				getLights();
			}
	    }
	}
	
	private void createViews() {
		ViewGroup container = (ViewGroup) findViewById(R.id.activity_main_lights);
		for (final String id : lights.keySet()) {
			Light l = lights.get(id);
			View view = createEntityView(id, l);
			container.addView(view);
		}
	}

	private void getLights() {
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				HueService hueService = new HueService(); // TODO
				try {
					lights = hueService.getLights();
				} catch (Exception e) {
					e.printStackTrace();
					// TODO 
				}
				return null;
			}

			@Override
			protected void onPostExecute(Void params) {
				createViews();
			}
		}.execute();
	}

	private View createEntityView(final String id, Light l) {
		View view = getLayoutInflater().inflate(R.layout.entity, null);
		TextView nameView = (TextView) view.findViewById(R.id.entity_name);
		nameView.setText(l.name);

		Button on = (Button) view.findViewById(R.id.entity_on);
		on.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				AndroidHueService.turnLightOn(id, true);
			}
		});

		Button off = (Button) view.findViewById(R.id.entity_off);
		off.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				AndroidHueService.turnLightOn(id, false);
			}
		});
		return view;
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable(STATE_LIGHTS, (Serializable) lights);
	}

	@Override
	protected void onResume() {
		datasource.open();
		super.onResume();
	}

	@Override
	protected void onPause() {
		datasource.close();
		super.onPause();
	}
}

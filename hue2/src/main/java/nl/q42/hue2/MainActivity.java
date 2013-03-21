package nl.q42.hue2;

import info.chees.androidhueapi.AndroidHueService;

import java.io.Serializable;
import java.util.Map;

import nl.q42.javahueapi.HueService;
import nl.q42.javahueapi.models.Light;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

	private final static String STATE_LIGHTS = "stateLights";
	
	private Map<String, Light> lights;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        /*
        final Button button = (Button) findViewById(R.id.button1);
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d("hue2", "clicked");
				AndroidHueService.turnLightOn(true);
			}
		});
		*/
        
		getLights();
    }

	private void getLights() {
		new AsyncTask<Void, Void, Map<String, Light>>() {
			@Override
			protected Map<String, Light> doInBackground(Void... params) {
				return HueService.getLights();
			}
			
			@Override
			protected void onPostExecute(Map<String, Light> lights) {
				ViewGroup container = (ViewGroup) findViewById(R.id.activity_main_lights);
				for (final String id : lights.keySet()) {
					Light l = lights.get(id);
					View view = createEntityView(id, l);
					container.addView(view);
				}
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
		// TODO
		outState.putSerializable(STATE_LIGHTS, (Serializable) lights);
	}
}

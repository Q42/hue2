package nl.q42.hue2.activities;

import java.util.ArrayList;
import java.util.List;

import nl.q42.hue2.R;
import nl.q42.hue2.adapters.BridgeAdapter;
import nl.q42.hue2.models.Bridge;
import nl.q42.javahueapi.HueService;
import nl.q42.javahueapi.models.SimpleConfig;

import org.teleal.cling.android.AndroidUpnpService;
import org.teleal.cling.android.AndroidUpnpServiceImpl;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.meta.RemoteDevice;
import org.teleal.cling.registry.Registry;
import org.teleal.cling.registry.RegistryListener;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

public class LinkActivity extends Activity {
	
	private int bridgeCount;
	private BridgeAdapter adapter;
	
	// TODO: upnp test
	// TODO: Hue bridge gives invalid service id, which prevents it from being reported! (see http://hue-ip/description.xml)
	private AndroidUpnpService upnpService;
	
	private ServiceConnection serviceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			upnpService = (AndroidUpnpService) service;
			upnpService.getRegistry().addListener(registryListener);
			upnpService.getControlPoint().search();
		}
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			upnpService = null;
		}
	};
	
	private RegistryListener registryListener = new RegistryListener() {
		@Override
		public void remoteDeviceDiscoveryStarted(Registry register, RemoteDevice device) {}
		
		@Override
		public void remoteDeviceDiscoveryFailed(Registry register, RemoteDevice device, Exception exc) {}
		
		@Override
		public void remoteDeviceAdded(Registry register, RemoteDevice device) {
			Log.d("hue2", "Friendly name: " + device.getDetails().getFriendlyName());
			Log.d("hue2", "Man: " + device.getDetails().getManufacturerDetails().getManufacturer());
			Log.d("hue2", "Desc: " + device.getDetails().getModelDetails().getModelDescription());
			Log.d("hue2", "Name: " + device.getDetails().getModelDetails().getModelName());
		}
		
		@Override
		public void remoteDeviceRemoved(Registry register, RemoteDevice device) {}
		
		@Override
		public void remoteDeviceUpdated(Registry register, RemoteDevice device) {}

		@Override
		public void localDeviceAdded(Registry arg0, LocalDevice arg1) {}

		@Override
		public void localDeviceRemoved(Registry arg0, LocalDevice arg1) {}
		
		@Override
		public void beforeShutdown(Registry arg0) {}

		@Override
		public void afterShutdown() {}
	};
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.activity_link);
		
		ListView bridgesView = (ListView) findViewById(R.id.link_bridges);
		adapter = new BridgeAdapter(this);
		bridgesView.setAdapter(adapter);
		bridgesView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Bridge b = adapter.getItem(position);
				Log.d("hue2", b.getIp());
				
				// TODO: Allow user to select bridge and view its lights
			}
		});
		
		// TODO: upnp test
		getApplicationContext().bindService(
	            new Intent(this, AndroidUpnpServiceImpl.class),
	            serviceConnection,
	            Context.BIND_AUTO_CREATE
	        );
		
		getBridgeIps();
		// TODO handle rotates of the screen
	}
	
	@Override
    protected void onDestroy() {
        super.onDestroy();
        if (upnpService != null) {
            upnpService.getRegistry().removeListener(registryListener);
        }
        getApplicationContext().unbindService(serviceConnection);
    }
	
	private void getBridgeIps() {
		setProgressBarIndeterminateVisibility(true); 
		new AsyncTask<Void, Void, List<String>>() {
			@Override
			protected List<String> doInBackground(Void... params) {
				Log.d("hue2", "Looking for bridges:");
				try {
					return HueService.getBridgeIps();
				} catch (Exception e) {
					e.printStackTrace();
				}
				// TODO show a toast?
				return new ArrayList<String>();
			}
			
			@Override
			protected void onPostExecute(List<String> ips) {
				bridgeCount = ips.size();
				if (bridgeCount == 0)
					setProgressBarIndeterminateVisibility(false);
				else {
					for (String ip : ips) {
						Log.d("hue2", ip);
						getSimpleConfig(ip);
					}
				}
			}
		}.execute();
	}
	
	private void getSimpleConfig(final String ip) {
		new AsyncTask<Void, Void, SimpleConfig>() {
			@Override
			protected SimpleConfig doInBackground(Void... params) {
				try {
					return HueService.getSimpleConfig(ip);
				} catch (Exception e) {
					e.printStackTrace();
				}
				return null;
			}
			
			@Override
			protected void onPostExecute(SimpleConfig simpleConfig) {
				// TODO check if this view is still alive?
				if (simpleConfig != null)
					adapter.add(new Bridge(ip, simpleConfig.name));
				checkProgress();
			}
		}.execute();
	}
		
	private void checkProgress() {
		bridgeCount--;
		if (bridgeCount <= 0) {
			TextView status = (TextView) findViewById(R.id.link_status);
			status.setText(R.string.link_status_select);
			setProgressBarIndeterminateVisibility(false);
			// TODO show a refresh button
		}
	}
}

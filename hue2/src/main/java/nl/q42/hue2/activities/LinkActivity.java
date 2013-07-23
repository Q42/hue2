package nl.q42.hue2.activities;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nl.q42.hue2.R;
import nl.q42.hue2.adapters.BridgeAdapter;
import nl.q42.hue2.models.Bridge;
import nl.q42.javahueapi.HueService;
import nl.q42.javahueapi.Networker;
import nl.q42.javahueapi.models.SimpleConfig;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
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
		new AsyncTask<Void , Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				try {
					String upnpRequest = "M-SEARCH * HTTP/1.1\nHOST: 239.255.255.250:1900\nMAN: ssdp:discover\nMX: 10\nST: ssdp:all";
					DatagramSocket upnpSender = new DatagramSocket();
					upnpSender.send(new DatagramPacket(upnpRequest.getBytes(), upnpRequest.length(), new InetSocketAddress("239.255.255.250", 1900)));
					
					HashMap<String, Boolean> ipsDiscovered = new HashMap<String, Boolean>();
					
					while (true) {
						byte[] responseBuffer = new byte[1024];
						
						DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);
						upnpSender.receive(responsePacket);
						
						String ip = responsePacket.getAddress().getHostAddress();
						String response = new String(responsePacket.getData());
						
						if (ipsDiscovered.containsKey(ip)) {
							// See if response contains description location
							Matcher m = Pattern.compile("LOCATION: (.*)", Pattern.CASE_INSENSITIVE).matcher(response);
							if (m.find()) {
								String description = Networker.get(m.group(1)).getBody().toLowerCase();
								if (description.contains("<modeldescription>philips hue")) {
									Log.d("hue2", "Found hue device at " + ip);
								}
							}
							
							// Ignore subsequent packets
							ipsDiscovered.put(ip, true);
						}
					}
				} catch (SocketException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				return null;
			}
		}.execute();	
		
		getBridgeIps();
		// TODO handle rotates of the screen
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

package nl.q42.hue2.activities;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nl.q42.hue2.R;
import nl.q42.hue2.Util;
import nl.q42.hue2.adapters.BridgeAdapter;
import nl.q42.hue2.models.Bridge;
import nl.q42.javahueapi.Networker;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Window;
import android.widget.ListView;

public class LinkActivity extends Activity {
	private BridgeAdapter bridgesAdapter;
	private ListView bridgesList;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.activity_link);
		
		// Set up layout
		bridgesList = (ListView) findViewById(R.id.link_bridges);
		bridgesAdapter = new BridgeAdapter(this);
		bridgesList.setAdapter(bridgesAdapter);
		
		// Start searching for bridges and add them to the results
		bridgeSearchTask.execute();
		setProgressBarIndeterminateVisibility(true);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		// Stop searching for bridges
		bridgeSearchTask.cancel(true);
	}
	
	private AsyncTask<Void, Void, Void> bridgeSearchTask = new AsyncTask<Void, Void, Void>() {
		@Override
		protected Void doInBackground(Void... params) {
			// Search bridges on local network using UPnP
			try {					
				String upnpRequest = "M-SEARCH * HTTP/1.1\nHOST: 239.255.255.250:1900\nMAN: ssdp:discover\nMX: 8\nST:SsdpSearch:all";
				DatagramSocket upnpSender = new DatagramSocket();
				upnpSender.send(new DatagramPacket(upnpRequest.getBytes(), upnpRequest.length(), new InetSocketAddress("239.255.255.250", 1900)));
				
				HashMap<String, Boolean> ipsDiscovered = new HashMap<String, Boolean>();
				
				while (true) {
					byte[] responseBuffer = new byte[1024];
					
					DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);
					upnpSender.receive(responsePacket);
					
					final String ip = responsePacket.getAddress().getHostAddress();
					final String response = new String(responsePacket.getData());
					
					if (!ipsDiscovered.containsKey(ip)) {
						Matcher m = Pattern.compile("LOCATION: (.*)", Pattern.CASE_INSENSITIVE).matcher(response);
						
						if (m.find()) {
							final String description = Networker.get(m.group(1)).getBody();
							
							// Parsing with RegEx allowed here because the output format is fairly strict
							final String modelName = Util.quickMatch("<modelName>(.*?)</modelName>", description);
							final String friendlyName = Util.quickMatch("<friendlyName>(.*?)</friendlyName>", description);
															
							// Check from description if we're dealing with a hue bridge or some other device
							if (modelName.toLowerCase().contains("philips hue bridge")) {
								// Get bridge name this way, because you need a valid username to get it through the config API
								final String bridgeName = Util.quickMatch("(.*) \\([^)]+\\)$", friendlyName);
								
								bridgesList.post(new Runnable() {
									@Override
									public void run() {
										bridgesAdapter.add(new Bridge(ip, bridgeName));
									}
								});
							}
						}
						
						// Ignore subsequent packets
						ipsDiscovered.put(ip, true);
					}
				}
			} catch (SocketException e) {
				// TODO: Handle network errors
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			return null;
		}
	};
}

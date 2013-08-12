package nl.q42.hue2.activities;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nl.q42.hue2.R;
import nl.q42.hue2.Util;
import nl.q42.hue2.adapters.BridgeAdapter;
import nl.q42.hue2.dialogs.BridgeInfoDialog;
import nl.q42.hue2.dialogs.BridgeLinkDialog;
import nl.q42.hue2.dialogs.ErrorDialog;
import nl.q42.hue2.models.Bridge;
import nl.q42.javahueapi.HueService;
import nl.q42.javahueapi.HueService.ApiException;
import nl.q42.javahueapi.Networker;
import nl.q42.javahueapi.Networker.Result;
import nl.q42.javahueapi.models.SimpleConfig;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

public class LinkActivity extends Activity {
	private static final int SEARCH_TIMEOUT = 30000;
	private static final int BROADCAST_INTERVAL = 5000;
	private static final int LINK_INTERVAL = 1000;
	
	private BridgeAdapter bridgesAdapter;
	private ListView bridgesList;
	
	private ImageButton refreshButton;
	private ProgressBar loadingSpinner;
	
	private BridgeSearchTask bridgeSearchTask;
	private Timer linkChecker = new Timer();
	private boolean searchingPaused = false;
	
	private ArrayList<Bridge> bridges = new ArrayList<Bridge>();
	
	@SuppressWarnings("unchecked")
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_link);
		
		// Set up layout
		bridgesList = (ListView) findViewById(R.id.link_bridges);
		bridgesAdapter = new BridgeAdapter(this);
		bridgesList.setAdapter(bridgesAdapter);
		bridgesList.setEmptyView(findViewById(R.id.link_empty));
		
		// Set up loading UI elements		
		ActionBar ab = getActionBar();
		ab.setCustomView(R.layout.loader);
		ab.setDisplayShowCustomEnabled(true);
		ab.setDisplayShowHomeEnabled(false);
		
		RelativeLayout loadingLayout = (RelativeLayout) ab.getCustomView();

		loadingSpinner = (ProgressBar) loadingLayout.findViewById(R.id.loader_spinner);		
		refreshButton = (ImageButton) loadingLayout.findViewById(R.id.loader_refresh);
		
		setEventHandlers();
		
		// Restore state or start over
		if (savedInstanceState == null) {
			Bridge lastBridge = Util.getLastBridge(this);
			if (lastBridge != null) {
				// This will be set again after a successful connection, connection failure leaves the last bridge null
				Util.setLastBridge(this, null);
				
				// Try connecting to last bridge
				connectToLastBridge(lastBridge);
			} else {
				// Start searching for bridges and add them to the results
				startSearching(true);
			}
		} else {
			bridges = (ArrayList<Bridge>) savedInstanceState.getSerializable("bridges");
			
			for (Bridge b : bridges) {
				bridgesAdapter.add(b);
			}
			
			// If a search was running, continue without removing existing results
			if (savedInstanceState.getBoolean("searching")) {				
				startSearching(false);
			}
		}
	}
	
	@Override
	protected void onSaveInstanceState(Bundle bundle) {
		super.onSaveInstanceState(bundle);
		
		bundle.putBoolean("searching", searchingPaused);
		bundle.putSerializable("bridges", bridges);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
		// If the search task was stopped, it should be resumed later
		searchingPaused = bridgeSearchTask != null;
		
		// Stop any search or link operations
		stopSearching();
		linkChecker.cancel();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		if (searchingPaused) {
			startSearching(false);
			searchingPaused = false;
		}
	}
	
	private void setEventHandlers() {
		refreshButton.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				startSearching(true);
			}
		});
		
		// Add connect event
		bridgesList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
				Bridge b = bridgesAdapter.getItem(pos);
				
				if (b.hasAccess()) {
					connectToBridge(b);
				} else {
					showLinkDialog(b);					
				}
			}
		});
		
		// Add bridge info event
		bridgesList.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int pos, long id) {
				Bridge b = bridgesAdapter.getItem(pos);
				BridgeInfoDialog dialog = BridgeInfoDialog.newInstance(b);
				dialog.show(getFragmentManager(), "dialog_info");
				
				return true;
			}
		});
	}
	
	// Performs checks to make sure the last bridge is available
	private void connectToLastBridge(final Bridge b) {		
		new AsyncTask<Void, Void, Void>() {			
			@Override
			protected Void doInBackground(Void... params) {
				try {
					Result res = Networker.get("http://" + b.getIp() + "/description.xml");
					
					if (res.getResponseCode() == 200 && res.getBody().toLowerCase().contains("philips hue bridge")) {
						// Make sure that the device on this IP is actually the same bridge device
						String serial = Util.quickMatch("<serialNumber>(.*?)</serialNumber>", res.getBody());
						if (serial.equals(b.getSerial())) {
							connectToBridge(b);
						}
					}
				} catch (IOException e) {
					// Last bridge unavailable, ignore
				}
				
				return null;
			}
		}.execute();
	}
	
	public void connectToBridge(Bridge b) {
		Intent connectIntent = new Intent(LinkActivity.this, LightsActivity.class);
		connectIntent.putExtra("bridge", b);
		startActivity(connectIntent);
	}
	
	private void stopSearching() {		
		if (bridgeSearchTask != null) {
			bridgeSearchTask.cancel(true);
			bridgeSearchTask = null;
			
			bridgesList.post(new Runnable() {
				@Override
				public void run() {
					setSearchIndicator(false);
				}
			});
		}
	}
	
	private void startSearching(boolean clearResults) {		
		stopSearching();
		
		if (clearResults) {
			bridges.clear();
			bridgesAdapter.clear();
		}
		
		bridgeSearchTask = new BridgeSearchTask();
		bridgeSearchTask.execute();
		
		setSearchIndicator(true);
	}
	
	private void setSearchIndicator(boolean searching) {
		if (searching) {
			refreshButton.setVisibility(View.GONE);
			loadingSpinner.setVisibility(View.VISIBLE);
		} else {
			refreshButton.setVisibility(View.VISIBLE);
			loadingSpinner.setVisibility(View.GONE);
		}
	}
	
	private class BridgeSearchTask extends AsyncTask<Void, Void, Boolean> {
		@Override
		protected void onPostExecute(Boolean result) {
			bridgeSearchTask = null;
			setSearchIndicator(false);
			
			if (!result) {
				ErrorDialog.show(getFragmentManager(), R.string.dialog_bridge_search_title, R.string.dialog_network_error);
			}
		}
		
		@Override
		protected Boolean doInBackground(Void... params) {
			// Two different search methods are used in order
			// 1. Bruteforce all IPs from 192.168.1.0 to 192.168.1.255 (used by other apps, usually works when UPnP fails)
			// 2. Search bridges through UPnP discovery
			
			searchBridgesBruteforce(this);
			
			try {
				searchBridgesUPnP(this);
			} catch (IOException e) {
				// UPnP failure
			}
			
			return true;
		}
	}
	
	private void searchBridgesBruteforce(BridgeSearchTask task) {
		HashSet<String> ipsDiscovered = new HashSet<String>();
		
		// Add any existing results if continuing from an existing search
		for (Bridge b : bridges) {
			ipsDiscovered.add(b.getIp());
		}
		
		// Build list of IPs to search (search from center to outer range, because devices are usually around 192.168.1.100)
		ArrayList<String> ips = new ArrayList<String>(255);
		
		ips.add("192.168.1.127");
		
		for (int i = 1; i <= 127; i++) {
			ips.add("192.168.1." + (127 - i));
			ips.add("192.168.1." + (127 + i));
		}
		
		//  Check every IP for a listening device
		for (int i = 0; i < 255; i++) {			
			try {
				final String ip = ips.get(i);
				if (ipsDiscovered.contains(ip)) continue;
				
				// First try to get UPnP description to see if a device is a Hue bridge
				final String description = Networker.doNetwork("http://" + ip + "/description.xml", "GET", "", 10).getBody();
				final String modelname = Util.quickMatch("<modelName>(.*?)</modelName>", description);
				
				if (modelname.toLowerCase().contains("philips hue bridge")) {
					final SimpleConfig cfg = HueService.getSimpleConfig(ip);
					final boolean access = HueService.userExists(ip, Util.getDeviceIdentifier(LinkActivity.this));
					final String mac = Util.quickMatch("<serialNumber>(.*?)</serialNumber>", description);
					
					bridgesList.post(new Runnable() {
						@Override
						public void run() {
							Bridge b = new Bridge(ip, mac, cfg.swversion, cfg.name, access);
							bridges.add(b);
							bridgesAdapter.add(b);
						}
					});
				}
			} catch (IOException e) {
				// Either no device or not a Philips Hue bridge, move on
			} catch (ApiException e) {
				// Not a Philips Hue bridge
			} catch (Exception e) {
				// Other types of exceptions sometimes occur when device is misbehaving (e.g. bad UPnP description file)
			}
		}
	}
	
	private void searchBridgesUPnP(BridgeSearchTask task) throws IOException {
		// Search bridges on local network using UPnP
		String upnpRequest = "M-SEARCH * HTTP/1.1\nHOST: 239.255.255.250:1900\nMAN: ssdp:discover\nMX: 8\nST:SsdpSearch:all";
		DatagramSocket upnpSock = new DatagramSocket();
		upnpSock.setSoTimeout(100);
		upnpSock.send(new DatagramPacket(upnpRequest.getBytes(), upnpRequest.length(), new InetSocketAddress("239.255.255.250", 1900)));
		
		HashSet<String> ipsDiscovered = new HashSet<String>();
		
		// Add any existing results from bruteforce search and previous searches
		for (Bridge b : bridges) {
			ipsDiscovered.add(b.getIp());
		}
		
		long start = System.currentTimeMillis();
		long nextBroadcast = start + BROADCAST_INTERVAL;
		
		while (true) {
			// Send a new discovery broadcast once a second
			if (System.currentTimeMillis() > nextBroadcast) {
				upnpSock.send(new DatagramPacket(upnpRequest.getBytes(), upnpRequest.length(), new InetSocketAddress("239.255.255.250", 1900)));
				nextBroadcast = System.currentTimeMillis() + BROADCAST_INTERVAL;
			}
			
			byte[] responseBuffer = new byte[1024];
			
			DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);
			
			try {
				upnpSock.receive(responsePacket);
			} catch (SocketTimeoutException e) {
				if (System.currentTimeMillis() - start > SEARCH_TIMEOUT || task.isCancelled()) {
					break;
				} else {
					continue;
				}
			}
			
			final String ip = responsePacket.getAddress().getHostAddress();
			final String response = new String(responsePacket.getData());
			
			if (!ipsDiscovered.contains(ip)) {
				Matcher m = Pattern.compile("LOCATION: (.*)", Pattern.CASE_INSENSITIVE).matcher(response);
				
				if (m.find()) {
					final String description = Networker.get(m.group(1)).getBody();
					
					// Parsing with RegEx allowed here because the output format is fairly strict
					final String modelName = Util.quickMatch("<modelName>(.*?)</modelName>", description);
													
					// Check from description if we're dealing with a hue bridge or some other device
					if (modelName.toLowerCase().contains("philips hue bridge")) {
						try {
							final SimpleConfig cfg = HueService.getSimpleConfig(ip);
							final boolean access = HueService.userExists(ip, Util.getDeviceIdentifier(LinkActivity.this));
							final String mac = Util.quickMatch("<serialNumber>(.*?)</serialNumber>", description);
							
							bridgesList.post(new Runnable() {
								@Override
								public void run() {
									Bridge b = new Bridge(ip, mac, cfg.swversion, cfg.name, access);
									bridges.add(b);
									bridgesAdapter.add(b);
								}
							});
						} catch (ApiException e) {
							// Do nothing, this basically serves as an extra check to see if it's really a hue bridge
						}
					}
				}
				
				// Ignore subsequent packets
				ipsDiscovered.add(ip);
			}
		}
	}
	
	public void stopLinkChecker() {
		linkChecker.cancel();
	}
	
	public void startLinkChecker(final Bridge b, final BridgeLinkDialog dialog) {
		final String username = Util.getDeviceIdentifier(this);
		
		linkChecker = new Timer();
		linkChecker.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				try {
					boolean pressed = HueService.createUser(b.getIp(), "hue2 (" + Util.getDeviceName() + ")", username);
					
					if (pressed) {						
						// User created!
						dialog.dismiss();
						linkChecker.cancel();
						connectToBridge(b);
					}
				} catch (ApiException e) {
					// Ignore, it's because link button hasn't been pressed yet
				} catch (IOException e) {
					dialog.dismiss();
					linkChecker.cancel();
					
					bridgesList.post(new Runnable() {
						@Override
						public void run() {
							ErrorDialog.show(getFragmentManager(), R.string.dialog_bridge_lost_title, R.string.dialog_network_error);
						}
					});
				}
			}
		}, 0, LINK_INTERVAL);
	}
	
	public void showLinkDialog(Bridge b) {
		stopSearching();
		
		BridgeLinkDialog dialog = BridgeLinkDialog.newInstance(b);
		dialog.show(getFragmentManager(), "dialog_link");
	}
}

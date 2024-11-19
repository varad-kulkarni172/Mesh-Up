package com.example.meshup;

import java.net.ServerSocket;
import java.net.Socket;

import com.example.meshup.APModeWorker;
import com.example.meshup.ClientModeWorker;
import com.example.meshup.fragments.ReachablePeersFragment;
import com.example.meshup.wifimanagement.CustomWifiManager;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.widget.Toast;

public class MeshupService implements Runnable
{
	Thread locator;

	ServerSocket listeningSocket;
	Socket connectionSocket;
	CustomWifiManager apManager;
	ScanResult meshupSR;
	WifiManager wm;
	ReachablePeersFragment reachablePeersFragment;
	com.example.meshup.FragmentHolder activity;
	ClientModeWorker cmworker;
	
	public MeshupService(Thread l, CustomWifiManager cwm,WifiManager wm, ReachablePeersFragment rpf, com.example.meshup.FragmentHolder activity)
	{
		apManager=cwm;
		locator = l;
		this.wm = wm;
		reachablePeersFragment = rpf;
		this.activity = activity;
	}

	public void run()
	{
		Log.d("Meshup Debug", "in MeshupService.run()");
		
		try
		{
			locator.join();

			if (!FragmentHolder.isFound())
			// Hotspot mode
			{
				Log.d("Meshup Debug", "MeshupService: meshup not found, starting AP");
				
				WifiConfiguration meshupConfig = new WifiConfiguration();
				meshupConfig.SSID = "meshup.testing";

				apManager.setWifiApEnabled(meshupConfig, true);
				
				Log.d("Meshup Debug", "Meshup service: AP started...");
				
				Thread APMode = new Thread(new APModeWorker(apManager, reachablePeersFragment, activity));
				APMode.start();
			}

			else
			// Client mode
			{

				// toastIt("Connecting..");
				try
				{
					connectTo(MeshupLocator.meshupSR.SSID);
				} 
				
				catch (Exception e)
				{
					Log.d("Meshup Debug",  e.getMessage());
				}
				
				listeningSocket = new ServerSocket(12345);
				Log.d("Meshup Debug", "Server Socket established..");

				Log.d("Meshup Debug", "Waiting for Connection");
				
				
				cmworker = ClientModeWorker.getInstance(listeningSocket.accept(), activity);
				Thread clientMode = new Thread(cmworker);

				Log.d("Meshup Debug", "Conencted!");

				clientMode.start();
			}
		}

		catch (Exception exc)
		{
			Log.d("Meshup Debug", exc.getMessage());
		}
	}
	
	private void connectTo(String SSID) throws Exception
	{
		try
		{
			WifiConfiguration newCon = new WifiConfiguration();
			newCon.SSID = "\"" + SSID + "\"";
			newCon.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
			int netId = wm.addNetwork(newCon);
			wm.disconnect();
			wm.enableNetwork(netId, true);
			wm.reconnect();
		}
		
		catch(Exception e)
		{
			Log.d("Meshup Debug", e.getMessage());
		}
	}
	/*
	private void toastIt(final String toast)
	{
		runOnUiThread(new Runnable()
		{
			public void run()
			{
				Toast.makeText(getApplicationContext(), toast,
						Toast.LENGTH_LONG).show();
			}
		});
	}*/
}
package com.example.meshup.wifimanagement;

import com.example.meshup.FragmentHolder;
import com.example.meshup.MeshupLocator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.util.Log;

public class MeshUpBroadCastReceiver extends BroadcastReceiver 
{
	CustomWifiManager cWifiManager;
	MeshupLocator locator;
	
	public MeshUpBroadCastReceiver(CustomWifiManager cwm, MeshupLocator a) 
	{
		cWifiManager = cwm;
		locator = a;
	}
	
	@Override
	public void onReceive(Context context, Intent intent) 
	{
		String action = intent.getAction();
		
		if(action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
		{
			locator.updateWifiList(cWifiManager.getScanResults());
			Log.d("MeshUp Debug", "in onReceive()");
		}
	}
}

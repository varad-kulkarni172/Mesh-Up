package com.example.meshup;

import java.util.ArrayList;
import java.util.List;

import com.example.meshup.wifimanagement.APScanListener;
import android.net.wifi.ScanResult;
import android.util.Log;
import android.widget.Toast;

public class MeshupLocator implements Runnable, APScanListener
{
	public static ScanResult meshupSR;
	ArrayList<ScanResult> APs = new ArrayList<ScanResult>();

	public ScanResult getConfig()
	{
		return meshupSR;
	}
	
	@Override
	public void run()
	{
		Log.d("Meshup Debug", "in MeshupLocator.run()");

		try
		{
			Log.d("Meshup Debug",
					"MeshupLocator: going to sleep for 10 seconds...");
			Thread.sleep(10000);
			Log.d("Meshup Debug",
					"MeshupLocator: Woken up from sleep... Been 10 second already? :o");

			Log.d("Meshup Debug",
					"MeshupLocator: The following SSIDs were found: \n");
			int d = 0;

			for (final ScanResult s : APs)
			{
				Log.d("Meshup Debug", d + "." + s.SSID);
				d++;

				if (s.SSID.contains("meshup."))
				{
					synchronized (this)
					{
						com.example.meshup.FragmentHolder.setFound(true);
					}

					meshupSR = s;

					Log.d("Meshup Debug",
							"Meshup Locator: Meshup found");
				}
			}

			if (!com.example.meshup.FragmentHolder.isFound())
			{
				Log.d("Meshup Debug", "Meshup Locator: Meshup not found");
			}
		}

		catch (Exception exc)
		{
			Log.d("Meshup Debug", exc.getMessage());
		}
	}

	/*
	 * private void toastIt(final String toast) { runOnUiThread(new Runnable() {
	 * public void run() { Toast.makeText(getApplicationContext(), toast,
	 * Toast.LENGTH_LONG).show(); } }); }
	 */

	@Override
	public void updateWifiList(List<ScanResult> list)
	{
		// wifiList.setAdapter(new APAdapter(getApplicationContext(), list));

		APs = (ArrayList) list;
	}
}
package com.example.meshup;

import java.util.ArrayList;

//import meshup.Iteration1.R;
import com.example.meshup.fragments.ReachablePeersFragment;
import com.example.meshup.fragments.ServiceFragment;
import com.example.meshup.wifimanagement.VisibleClient;
import com.example.meshup.wifimanagement.MeshUpBroadCastReceiver;
import com.example.meshup.wifimanagement.CustomWifiManager;
import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.app.Activity;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.meshup.MeshupService;

public class FragmentHolder extends Activity
{	
	CustomWifiManager apManager;
	ListView clientList, wifiList;
	Handler UIHandler;
	ArrayList<VisibleClient> clients;
	ArrayList<com.example.meshup.ConnectedClient> connected;
	com.example.meshup.MeshupLocator meshupLocator;
	ActionBar actionBar;
	public static Handler uiHandler;
	public static Context context;
	
	EditText message;
	EditText chat;
	Button sendButton;
	
	ReachablePeersFragment reachableClientsFragment;
	ServiceFragment serviceFragment;
	
	ClientModeWorker worker;

	static boolean found = false;

	public synchronized static boolean isFound()
	{
		return found;
	}

	public synchronized static void setFound(boolean f)
	{
		found = f;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		try
		{
			super.onCreate(savedInstanceState);
			
			uiHandler = new Handler();
			context = getApplicationContext();

			setContentView(R.layout.main_layout);
			
			apManager = new CustomWifiManager(getApplicationContext());

			WifiManager manager = (WifiManager) this.getSystemService(WIFI_SERVICE);
			
			if(!manager.isWifiEnabled())
			{
				manager.setWifiEnabled(true);
			}
			
			manager.startScan();

			setupUI();

			IntentFilter meshupFilter = new IntentFilter();
			meshupFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
			meshupLocator = new MeshupLocator();
			registerReceiver(new MeshUpBroadCastReceiver(apManager, meshupLocator), meshupFilter);

			Thread locator = new Thread(meshupLocator);
			locator.start();

			Thread service = new Thread(new MeshupService(locator, apManager, (WifiManager) getSystemService(WIFI_SERVICE), reachableClientsFragment, this));
			service.start();
		}

		catch (Exception exc)
		{
			exc.printStackTrace();
		}
	}
	
	public String getMessage()
	{
		return message.getText().toString();
	}
	
	public void updateChat(final String message)
	{
		runOnUiThread(new Runnable()
		{
			public void run()
			{
				chat.append(message + '\n');
			}
		});
	}
	
	public void toastThis(final String toast)
	{
		runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				Toast.makeText(getApplicationContext(), toast, Toast.LENGTH_LONG).show();
			}
		});
	}
	
	private void setupUI()
	{
		//ViewGroup view = (ViewGroup) findViewById(R.id.mainfragment);
		reachableClientsFragment = new ReachablePeersFragment();
		serviceFragment = new ServiceFragment();
		
		message = (EditText) findViewById(R.id.editText1);
		sendButton = (Button) findViewById(R.id.sendButton);
		chat = (EditText) findViewById(R.id.editText2);
		
		sendButton.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				worker=ClientModeWorker.getInstance();
				if(worker!=null)
				{
					Message msg=Message.obtain();
					msg.obj=(String) message.getText().toString();
					worker.handler.sendMessage(msg);
				}
				
			}
		});
		
		actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		
		actionBar.addTab(actionBar.newTab().setText("Service").setTabListener(new CustomTabListener(serviceFragment)));
		actionBar.addTab(actionBar.newTab().setText("Reachable Peers").setTabListener(new CustomTabListener(reachableClientsFragment)));
	}
	
	class CustomTabListener implements TabListener
	{
		Fragment frag;
		
		public CustomTabListener(Fragment f)
		{
			frag = f;
		}
		
		@Override
		public void onTabReselected(Tab tab, FragmentTransaction ft)
		{
		}
		
		@Override
		public void onTabSelected(Tab tab, FragmentTransaction ft)
		{
			ft.replace(R.id.mainfragment, frag);
		}
		
		@Override
		public void onTabUnselected(Tab tab, FragmentTransaction ft)
		{
			ft.remove(frag);
		}
	}
}

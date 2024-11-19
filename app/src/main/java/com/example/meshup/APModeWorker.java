package com.example.meshup;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

import com.example.meshup.fragments.ReachablePeersFragment;
import com.example.meshup.wifimanagement.CustomWifiManager;
import com.example.meshup.wifimanagement.FinishScanListener;
import com.example.meshup.wifimanagement.VisibleClient;
import android.os.Handler;
import android.util.Log;
import android.webkit.WebView.FindListener;
import android.widget.TextView;

public class APModeWorker implements Runnable
{
	private Socket connectionSocket;
	private CustomWifiManager apManager;
	ArrayList<VisibleClient> clients = new ArrayList<VisibleClient>();
	ArrayList<ConnectedClient> connected = new ArrayList<ConnectedClient>();
	
	FragmentHolder activity;
	ReachablePeersFragment reachablePeersFragment;
	
	private final String TAG = "Meshup Debug";
	
	public APModeWorker(CustomWifiManager wm, ReachablePeersFragment rpf, FragmentHolder a)
	{
		this.apManager = wm;
		reachablePeersFragment = rpf;
		activity = a;
	}
	
	@Override
	public void run()
	{
		try
		{
			Log.d("Meshup Debug", "in APModeWorker.run()");
			
			while(true)
			{
				Thread clientFinder = new Thread(new Runnable()
				{
					@Override
					public void run()
					{
						apManager.getClientList(true, new FinishScanListener()
						{
							@Override
							public void onFinishScan(ArrayList<VisibleClient> clientList)
							{
								Log.d(TAG, "APModeWorker: in onFinishScan()");
								
								if(clients.size() == 0)
								{
									for(VisibleClient v:clientList)
									{
										clients.add(v);
									}
								}
								
								else
								{
									for(VisibleClient v:clientList)
									{
										boolean found = false;
										
										for(int i = 0; i < clients.size(); i++)
										{
											if(v.getIpAddr().equals(clients.get(i).getIpAddr()))
											{
												found = true;
												break;
											}
										}
										
										if(!found)
										{
											clients.add(v);
										}
									}
								}
							}
						});
					}
				});
				
				clientFinder.start();	//Start client finder 
				
				Log.d("Meshup Debug", "Now waiting...");
				
				clientFinder.join();	//wait for results from client finder
				
				Log.d("Meshup Debug", "Notified...");
				
				Log.d(TAG, "APModeWorker: found these clients:");
				
				for (VisibleClient v : clients)
				{
					Log.d("Meshup Debug", v.getIpAddr());
				}
				
				reachablePeersFragment.updateList(clients);
				
				for (final VisibleClient c:clients)
				{
					if(!c.IsConnected())
					{
						new Thread(new Runnable()
						{
							public void run()
							{
								try
								{
									Log.d("Meshup Debug", "APModeWorker: Connecting to Client " + c.getIpAddr());
									
									connectionSocket = new Socket(c.getIpAddr(), 12345);
									
									if(connectionSocket.isConnected())
									{
										Log.d("Meshup Debug", "APModeWorker: Connected to Client " + c.getIpAddr());
										c.setConnected(true);
									}
									
									c.toClient = new ObjectOutputStream(connectionSocket.getOutputStream());									
									c.fromClient = new ObjectInputStream(connectionSocket.getInputStream());
									
									Log.d("Meshup Debug", "Sending message to client " + c.getIpAddr());
									
									c.toClient.writeObject(new String("Hello! You are now connected to the Access point!"));
									
									Log.d("Meshup Debug", "Sent message...");	
									
									while(true)
									{
										try
										{
											String broadcast = (String) c.fromClient.readObject();
											
											for(VisibleClient x:clients)
											{
												if(!x.equals(c))
													x.toClient.writeObject(broadcast);
											}
										}
										
										catch(Exception exc)
										{
											Log.d("Meshup Debug", exc.getMessage());
										}
									}					
								}
	
								catch (Exception e)
								{
									e.printStackTrace();
									Log.d("Meshup Debug", c.getIpAddr()	+ " cause: " + e.getCause());
								}
							}
						}).start();
					}
				}
					
					Thread.sleep(5000);		//Conduct search every 5 seconds
			}
		}

		catch (Exception exc)
		{
			Log.d("Meshup Debug", exc.getMessage());
		}
	}
}
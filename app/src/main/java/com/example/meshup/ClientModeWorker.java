package com.example.meshup;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

//import android.widget.Toast;

public class ClientModeWorker implements Runnable
{
	private static ClientModeWorker worker = new ClientModeWorker();
	Socket commSocket;

	ServerSocket listeningSocket;
	Socket connectionSocket;

	ObjectInputStream fromHost;
	ObjectOutputStream toHost;

	FragmentHolder activity;
	Handler handler;

	public static ClientModeWorker getInstance()
	{
		return worker;
	}
	
	public static ClientModeWorker getInstance(Socket socket, FragmentHolder activity)
	{
		worker.activity = activity;

		try
		{
			worker.commSocket = socket;

			worker.fromHost = new ObjectInputStream(worker.commSocket.getInputStream());
			worker.toHost = new ObjectOutputStream(worker.commSocket.getOutputStream());
		}

		catch (Exception exc)
		{
			Log.d("Meshup Debug", "CMW: " + exc.getMessage());
		}
		
		return worker;
	}

//	public ClientModeWorker(Socket socket, FragmentHolder activity)
//	{
//		this.activity = activity;
//
//		try
//		{
//			commSocket = socket;
//
//			fromHost = new ObjectInputStream(commSocket.getInputStream());
//			toHost = new ObjectOutputStream(commSocket.getOutputStream());
//		}
//
//		catch (Exception exc)
//		{
//			Log.d("Meshup Debug", "CMW: " + exc.getMessage());
//		}
//	}

	public ClientModeWorker()
	{
		// TODO Auto-generated constructor stub
	}

	@SuppressLint("HandlerLeak")
    @Override
	public void run()
	{	
		Looper.prepare();
		
		handler= new Handler()
		{
			public void handleMessage(Message msg)
			{
				try
				{
					toHost.writeObject(msg.obj);
					activity.updateChat("You say: " + msg.obj);
				}
				
				catch (Exception e)
				{
					Log.d("Meshup Debug", Objects.requireNonNull(e.getMessage()));
				}
			}
		};
		
		Log.d("Meshup Debug", "in ClientModeWorker.run()");

		try
		{
			Log.d("Meshup Debug", "ClientModeWorker: reading string...");

			final String test = (String) fromHost.readObject();

			Log.d("Meshup Debug", "ClientModeWorker: read message: " + test);

			activity.toastThis(test);
			
			Thread recvThread = new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					while(true)
					{
						try
						{
							String test2 = (String) fromHost.readObject();
							
							activity.updateChat("Someone says: " + test2);
						}
						
						catch (Exception e)
						{
							Log.d("Meshup Debug", Objects.requireNonNull(e.getMessage()));
						}	
					}
				}
			});
			
			recvThread.start();
			
			Looper.loop();
		}

		catch (Exception exc)
		{
			Log.d("Meshup Debug", 	Objects.requireNonNull(exc.getMessage()));
		}
	}
}
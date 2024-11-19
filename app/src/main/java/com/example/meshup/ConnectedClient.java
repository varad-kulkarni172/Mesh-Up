package com.example.meshup;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.StreamCorruptedException;

import android.util.Log;

public class ConnectedClient
{

	String ipAddress;
	ObjectInputStream inputStream;
	ObjectOutputStream outputStream;

	ConnectedClient(String ip, InputStream in, OutputStream out)
	{
		ipAddress = ip;

		try
		{
			inputStream = new ObjectInputStream(in);
		}

		catch (StreamCorruptedException e1)
		{
			Log.d("Meshup Debug", "Caught StreamCorruptedException");
			e1.printStackTrace();
		}

		catch (IOException e1)
		{
			Log.d("Meshup Debug", "Caught StreamCorruptedException");
			e1.printStackTrace();
		}
		try
		{
			outputStream = new ObjectOutputStream(out);
		}

		catch (IOException e)
		{
			Log.d("Meshup Debug", "Caught StreamCorruptedException");
			e.printStackTrace();
		}
	}

	public String getIpAddress()
	{
		return ipAddress;
	}

	public void setIpAddress(String ipAddress)
	{
		this.ipAddress = ipAddress;
	}

	public ObjectInputStream getInputStream()
	{
		return inputStream;
	}

	public void setInputStream(ObjectInputStream inputStream)
	{
		this.inputStream = inputStream;
	}

	public ObjectOutputStream getOutputStream()
	{
		return outputStream;
	}

	public void setOutputStream(ObjectOutputStream outputStream)
	{
		this.outputStream = outputStream;
	}

}

package com.example.meshup.wifimanagement;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class VisibleClient 
{
	private String IpAddr;
	private String HWAddr;
	private String Device;
	private boolean isReachable;
	private boolean isConnected;
	
	public ObjectOutputStream toClient;
	public ObjectInputStream fromClient;

	public boolean IsConnected()
	{
		return isConnected;
	}
	
	public void setConnected(boolean b)
	{
		isConnected = true;
	}
	
	public VisibleClient(String ipAddr, String hWAddr, String device, boolean isReachable) 
	{
		super();
		this.IpAddr = ipAddr;
		this.HWAddr = hWAddr;
		this.Device = device;
		this.isReachable = isReachable;
		isConnected = false;
	}

	public String getIpAddr() 
	{
		return IpAddr;
	}
	
	public void setIpAddr(String ipAddr) 
	{
		IpAddr = ipAddr;
	}

	public String getHWAddr() 
	{
		return HWAddr;
	}
	
	public void setHWAddr(String hWAddr) 
	{
		HWAddr = hWAddr;
	}

	public String getDevice() 
	{
		return Device;
	}
	
	public void setDevice(String device) 
	{
		Device = device;
	}

	public boolean isReachable() 
	{
		return isReachable;
	}
	
	public void setReachable(boolean isReachable) 
	{
		this.isReachable = isReachable;
	}
}
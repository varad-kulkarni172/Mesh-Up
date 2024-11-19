package com.example.meshup.wifimanagement;

import java.util.List;

import android.net.wifi.ScanResult;

public interface APScanListener 
{
	public void updateWifiList(List <ScanResult> list);
}

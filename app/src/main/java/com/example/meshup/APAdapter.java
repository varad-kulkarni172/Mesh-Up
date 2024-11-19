package com.example.meshup;

import java.util.List;

//import meshup.Iteration1.R;
import android.annotation.SuppressLint;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class APAdapter extends ArrayAdapter <ScanResult> 
{	
	public APAdapter(Context context, List<ScanResult> objects) 
	{
		super(context, R.layout.ap_layout, objects);
	}
	
	@SuppressLint("SetTextI18n")
    @Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		ScanResult temp = getItem(position);
		
		if(convertView == null)
		{
			convertView = LayoutInflater.from(getContext()).inflate(R.layout.ap_layout, parent, false);
		}
		
		TextView ssid = (TextView) convertView.findViewById(R.id.ssid);
		ssid.setText(temp.SSID);
		
		TextView level = (TextView) convertView.findViewById(R.id.level);
		level.setText(Integer.toString(temp.level));
		
		return convertView;
	}
}

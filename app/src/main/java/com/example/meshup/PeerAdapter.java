package com.example.meshup;

import java.util.List;

//import meshup.Iteration1.R;
import com.example.meshup.wifimanagement.VisibleClient;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class PeerAdapter extends ArrayAdapter<VisibleClient> 
{	
	public PeerAdapter(Context context, List<VisibleClient> objects) 
	{
		super(context, R.layout.peer_layout, objects);
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) 
	{
		VisibleClient temp = getItem(position);
		
		if(convertView == null)
		{
			convertView = LayoutInflater.from(getContext()).inflate(R.layout.peer_layout, parent, false);
		}
		
		TextView holdersName = (TextView) convertView.findViewById(R.id.holderName);
		holdersName.setText("Dummy name");
		
		TextView ipAddr = (TextView) convertView.findViewById(R.id.deviceIP);
		ipAddr.setText(temp.getIpAddr());
		
		return convertView;
	}
}

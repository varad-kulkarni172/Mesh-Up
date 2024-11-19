package com.example.meshup.fragments;

import java.util.ArrayList;

//import meshup.Iteration1.R;
import com.example.meshup.PeerAdapter;
import com.example.meshup.R;

import com.example.meshup.wifimanagement.VisibleClient;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

public class ReachablePeersFragment extends Fragment
{
	private ArrayList<VisibleClient> clients = new ArrayList<VisibleClient>();
	
	public void updateList(ArrayList<VisibleClient> list)
	{
		clients = list;
	}
	
	public ReachablePeersFragment()
	{
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View rootView = inflater.inflate(R.layout.reachable_layout, container, false);
		
		ListView lv = (ListView) rootView.findViewById(R.id.listView1);
		lv.setAdapter(new PeerAdapter(getActivity(), clients));
		
		return rootView;
	}
}

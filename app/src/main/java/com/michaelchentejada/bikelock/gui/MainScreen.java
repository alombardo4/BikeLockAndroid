package com.michaelchentejada.bikelock.gui;

import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;

import com.michaelchentejada.bikelock.R;
import com.michaelchentejada.bikelock.bluetooth.BikeLockBluetoothLeService;
import com.michaelchentejada.bikelock.gui.adapters.LockListAdapter;

public class MainScreen extends ListFragment implements OnClickListener{
	BaseAdapter mAdapter;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		View layout = inflater.inflate(R.layout.main_screen, container, false);
		View btn = layout.findViewById(R.id.add_device);
		btn.setOnClickListener(this);
		return layout;
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Pair<String, String> device = (Pair<String, String>) mAdapter.getItem(position);
		
		Intent blService = new Intent(getActivity(), BikeLockBluetoothLeService.class);
		blService.putExtra(BikeLockBluetoothLeService.EXTRA_ADDR, device.first);
		blService.putExtra(BikeLockBluetoothLeService.EXTRA_PASS, new byte[] {0,1,2,3,4,5,6,7,8,9,0xa,0xb,0xc,0xd,0xe,0xf});
        System.out.println(blService.getExtras().get(BikeLockBluetoothLeService.EXTRA_PASS));
		getActivity().startService(blService);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mAdapter = new LockListAdapter(getActivity());
		setListAdapter(mAdapter);
	}

	
	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.add_device) {

            FragmentTransaction ft = this.getFragmentManager().beginTransaction();
            ft.replace(R.id.container, new NewLockFragment(), NewLockFragment.class.getName())
                    .addToBackStack(NewLockFragment.class.getName())
                    .commit();
		}
	}

}

package com.bikelock.bikelock.gui.views;

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
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.bikelock.bikelock.R;
import com.bikelock.bikelock.bluetooth.BikeLockBluetoothLeService;
import com.bikelock.bikelock.gui.adapters.LockListAdapter;
import com.bikelock.bikelock.model.PairedDevice;

public class MainScreen extends ListFragment implements OnClickListener{
	BaseAdapter mAdapter;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		View layout = inflater.inflate(R.layout.main_screen, container, false);
		Button btn = (Button) layout.findViewById(R.id.add_device);
		btn.setOnClickListener(this);
        mAdapter = new LockListAdapter(getActivity());
        setListAdapter(mAdapter);
		return layout;
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		PairedDevice device = (PairedDevice) mAdapter.getItem(position);
		connectToDevice(device);
        Toast.makeText(getActivity().getBaseContext(), getString(R.string.connecting_to_lock), Toast.LENGTH_SHORT).show();
    }

    private void connectToDevice(PairedDevice device) {
        Intent blService = new Intent(getActivity(), BikeLockBluetoothLeService.class);
        blService.putExtra(BikeLockBluetoothLeService.EXTRA_ADDR, device.getAddress());
        blService.putExtra(BikeLockBluetoothLeService.EXTRA_PASS, new byte[] {0,1,2,3,4,5,6,7,8,9,0xa,0xb,0xc,0xd,0xe,0xf}); //TODO replace with password
        System.out.println(blService.getExtras().get(BikeLockBluetoothLeService.EXTRA_PASS));
        getActivity().startService(blService);
    }

	
	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.add_device) {

            FragmentTransaction ft = this.getFragmentManager().beginTransaction();
            NewLockFragment fragment = new NewLockFragment();
            ft.replace(R.id.container, fragment, NewLockFragment.class.getName());
            ft.addToBackStack(NewLockFragment.class.getName());
            ft.commit();
		}
	}

}

package com.bikelock.bikelock.gui.views;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.bikelock.bikelock.R;
import com.bikelock.bikelock.database.LockDBAdapter;
import com.bikelock.bikelock.gui.adapters.FoundDeviceListAdapter;
import com.bikelock.bikelock.model.PairedDevice;


public class NewLockFragment extends ListFragment implements LeScanCallback{
	private static final int REQUEST_ENABLE_BT = 0;
	private static final int REQUEST_ADD_BT = 1;
	
	private static final long SCAN_PERIOD = 10000;
		
	private FoundDeviceListAdapter mAdapter;
	private BluetoothAdapter mBluetoothAdapter;
	private boolean mScanning;
    private Handler mHandler;
    
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
    	final BluetoothDevice device = (BluetoothDevice) mAdapter.getItem(position);
        
    	if (device == null) return;
        
        scanLeDevice(false);
        
        Bundle bundle = new Bundle();
        bundle.putString(AddLockDialog.ADDR_KEY, device.getAddress());
        
        DialogFragment diag = new AddLockDialog();
        diag.setTargetFragment(this, REQUEST_ADD_BT);
        diag.setArguments(bundle);
        diag.show(getFragmentManager(), AddLockDialog.class.getName());
        
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(NewLockFragment.this);
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(this);
        } else if (mScanning){
            mScanning = false;
            mBluetoothAdapter.stopLeScan(this);
        }
        mAdapter.notifyDataSetChanged();

    }
    
	@Override
	public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
		if (device == null) {
			return;
		} 
		
		mAdapter.addItem(device);		
		mAdapter.notifyDataSetChanged();
	}
		
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		mHandler = new Handler();
		mAdapter = new FoundDeviceListAdapter(getActivity());
		setListAdapter(mAdapter);
		final BluetoothManager bluetoothManager = (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
            case REQUEST_ENABLE_BT:
                //Scan bluetooth if possible, else quit
                if (resultCode == Activity.RESULT_OK) {
                    scanLeDevice(true);
                }else{
                    Toast toast = Toast.makeText(getActivity(), "Bluetooth must be enabled", Toast.LENGTH_SHORT);
                    toast.show();
                    getFragmentManager().popBackStack();
                }

                break;
            case REQUEST_ADD_BT:
                if (resultCode == Activity.RESULT_OK) {
                    FragmentTransaction ft = getFragmentManager().beginTransaction();
                    ft.replace(R.id.container, HomeFragment.newInstance(), HomeFragment.class.getName())
                            .commit();




                }
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
	}
	
	@Override
	public void onPause() {
		super.onPause();
		scanLeDevice(false);
	}
	
	@Override
	public void onResume() {
		super.onResume();
	    MainActivity.toolbar.setTitle(getString(R.string.add_lock));
		//Enable Bluetooth if disabled; otherwise, start scanning right away
		if (!mBluetoothAdapter.isEnabled()) {
		    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}else{
			mAdapter.clear();
			scanLeDevice(true);
		}
	}
	


	public final static class AddLockDialog extends DialogFragment implements OnClickListener{
		public static final String ADDR_KEY = "address";
		
		private EditText name;
		private EditText passwd;
		private String address;
		
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			
			AlertDialog.Builder builder =  new AlertDialog.Builder(getActivity());
			builder.setTitle("New Lock");
			
			View layout = getActivity().getLayoutInflater().inflate(R.layout.add_lock_diag, null);
			name = (EditText) layout.findViewById(R.id.name);
			passwd = (EditText) layout.findViewById(R.id.passwd);
			
			builder.setView(layout);
			
			//TODO: Fix button always closing dialog
			
			builder.setPositiveButton(R.string.save, this);
			builder.setNegativeButton(android.R.string.cancel, this);
			
			return builder.create();
		}

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			address = getArguments().getString(ADDR_KEY);
		}
		
		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
			case DialogInterface.BUTTON_POSITIVE:
				boolean valid = true;
				
				if (name.getText().length() == 0) {
					name.setError("Please write a name");
					valid = false;
				}else{
					name.setError(null);
				}
				
				
				if (passwd.getText().length() == 0) {
					passwd.setError("Please write the password");
					valid = false;
				}else{
					passwd.setError(null);

				}
				
				if (valid == true) {
                    LockDBAdapter dbAdapter = LockDBAdapter.getInstance(getActivity());
                    PairedDevice device = new PairedDevice(address, name.getText().toString(), passwd.getText().toString());

                    dbAdapter.addLock(device);
					
					getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, null);
					dismiss();
				}
				
				break;
			case DialogInterface.BUTTON_NEGATIVE:
				getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_CANCELED, null);
				dismiss();
				break;
			default:
				break;
			}
			
		}
	}
}

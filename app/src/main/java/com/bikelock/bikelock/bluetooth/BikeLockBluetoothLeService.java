package com.bikelock.bikelock.bluetooth;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

public class BikeLockBluetoothLeService extends Service{
	public final static String EXTRA_ADDR = "Address";
	public final static String EXTRA_PASS = "Password";
	private BluetoothAdapter mBluetoothAdapter;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	/**
	 * Initialize the adapter and set to unlock the requested device
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		
		final BluetoothManager blManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = blManager.getAdapter();
		
		if (mBluetoothAdapter == null) {
			throw new RuntimeException("Error obtaining bluetooth adapter");
		}
		
		final String address = intent.getStringExtra(EXTRA_ADDR);
		
		if (address == null) {
			throw new NullPointerException("A MAC address is required");
		}
		
		final byte[] password = intent.getByteArrayExtra(EXTRA_PASS);
		
		if (password == null) {
			throw new NullPointerException("A password is required");
		}
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				BluetoothCallback callback = new BluetoothCallback();
				
				final BluetoothDevice lock = mBluetoothAdapter.getRemoteDevice(address);
				if (!callback.connect(lock, com.bikelock.bikelock.bluetooth.BikeLockBluetoothLeService.this)){
					return;
				}
				callback.writeString("UNLOCK\r");
				
				byte[] nonce = callback.readLine();
				byte hashInput[] = new byte[32];
				System.arraycopy(password, 0, hashInput, 0, 16);
				System.arraycopy(nonce, 0, hashInput, 16, 16);
				MessageDigest digest;
				try {
					digest = MessageDigest.getInstance("SHA-256");
				} catch (NoSuchAlgorithmException e) {
					throw new UnsupportedOperationException("?!?");
				}
				byte[] hash = digest.digest(hashInput);
				
				callback.writeBytes(hash);
				callback.writeBytes(new byte[]{'\r'});
				callback.close();
			}
		}).start();
		
		return START_NOT_STICKY;
	}

}

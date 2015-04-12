package com.bikelock.bikelock.bluetooth;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.app.IntentService;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class BikeLockBluetoothLeService extends IntentService{
	

	public final static String EXTRA_ADDR = "Address";
	public final static String EXTRA_PASS = "Password";
	public final static String EXTRA_NEW_PASS = "New password";
	
	private BluetoothAdapter mBluetoothAdapter;

	/**
	 * Constructor for the new service
	 */
	public BikeLockBluetoothLeService() {
		//Sets the name for the worker thread
		super("Bluetooth Service Thread");
		Log.d("Tag", "Service Created");
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		
		//Gets the bluetooth adapter
		final BluetoothManager blManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = blManager.getAdapter();
		
		if (mBluetoothAdapter == null) {
			throw new RuntimeException("Error obtaining bluetooth adapter");
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		mBluetoothAdapter = null;
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		final String address = intent.getStringExtra(EXTRA_ADDR);

		if (address == null) {
			throw new NullPointerException("A MAC address is required");
		}
		
		final String strPassword = intent.getStringExtra(EXTRA_PASS);
		
		if (strPassword == null) {
			throw new NullPointerException("A password is required");
		}
		
		int bytesToCopy = Math.min(strPassword.length(), 16);
		byte[] password = new byte[16];
		System.arraycopy(strPassword.getBytes(), 0, password, 0, bytesToCopy);

		final String strNewPassword = intent.getStringExtra(EXTRA_NEW_PASS);

		if (strNewPassword == null) {
			unlockLock(address, password);
		}else{
			bytesToCopy = Math.min(strNewPassword.length(), 16);
			byte[] newPassword = new byte[16];
			System.arraycopy(strNewPassword.getBytes(), 0, newPassword, 0, bytesToCopy);
			
			changePassword(address, password, newPassword);
		}
	}
	
	private void unlockLock (final String address, final byte[] password){
		BluetoothCallback callback = new BluetoothCallback(BikeLockBluetoothLeService.this);

		final BluetoothDevice lock = mBluetoothAdapter.getRemoteDevice(address);
		if (!callback.connect(lock, BikeLockBluetoothLeService.this)){
			Log.e("TAG", "Connection Failed, Retrying");
			callback.close();
            try {
                Thread.sleep(200);
            } catch (Exception e) {

            }
            if (!callback.connect(lock, BikeLockBluetoothLeService.this)){
                Log.e("TAG", "Connection Failed");
                callback.close();
                return;
            }
		}

		callback.writeString("UNLOCK\r");

		byte[] nonce = callback.readLine();

		if (nonce == null) {
			Log.e("TAG","Reading the nonce failed");
			callback.close();
			return;
		}

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
	
	private void changePassword(final String address, final byte[] oldPassword, final byte[] newPassword){
		BluetoothCallback callback = new BluetoothCallback(BikeLockBluetoothLeService.this);

		final BluetoothDevice lock = mBluetoothAdapter.getRemoteDevice(address);
		if (!callback.connect(lock, BikeLockBluetoothLeService.this)){
			Log.e("TAG", "Connection Failed, Retrying");
			callback.close();
            try {
                Thread.sleep(200);
            } catch (Exception e) {

            }
            if (!callback.connect(lock, BikeLockBluetoothLeService.this)){
                Log.e("TAG", "Connection Failed");
                callback.close();
                return;
            }
		}

		callback.writeString("CHANGE PASSWORD\r");

		byte[] nonce = callback.readLine();

		if (nonce == null) {
			Log.e("TAG","Reading the nonce failed");
			callback.close();
			return;
		}

		byte hashInput[] = new byte[32];
		System.arraycopy(oldPassword, 0, hashInput, 0, 16);
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
		
		byte[] response = callback.readLine();
		
		if (response == null || ! new String(response).equals("NEW PASSWORD")) {
			Log.e("TAG","Acknowledgement not received");
			callback.close();
			return;
		}
		
		callback.writeBytes(newPassword);
		callback.writeBytes(new byte[]{'\r'});
		
		response = callback.readLine();
		
		if (response == null || ! new String(response).equals("CONFIRM PASSWORD")) {
			Log.e("TAG","Second acknowledgement not received");
			callback.close();
			return;
		}
		
		callback.writeBytes(newPassword);
		callback.writeBytes(new byte[]{'\r'});
		
		response = callback.readLine();
		
		if (response == null || ! new String(response).equals("SUCCESS")) {
			Log.e("TAG","Second acknowledgement not received");
			callback.close();
			return;
		}
		
		callback.close();
	}

}
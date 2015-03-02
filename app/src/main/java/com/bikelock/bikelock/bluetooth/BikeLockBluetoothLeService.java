package com.bikelock.bikelock.bluetooth;

import java.util.UUID;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class BikeLockBluetoothLeService extends Service{
	public final static String EXTRA_ADDR = "Address";
	public final static String EXTRA_PASS = "Password";
	private final static String TAG = "BLE Service";
	
	private BluetoothAdapter mBluetoothAdapter;
	private byte passwd[];
	
	private final BluetoothGattCallback mCallback = new BluetoothGattCallback() {
		private int position = 0;
		private byte readBuffer[] = new byte[512];
				
		private final void sendHash(byte nonce[], BluetoothGatt gatt){
			byte hash[] = new byte[32];
			System.arraycopy(passwd, 0, hash, 0, 16);
			System.arraycopy(nonce, 0, hash, 16, 16);
			
			Log.i(TAG, "Sending hash");
			
			//Send hash
			RxTx.setValue(hash);
			gatt.writeCharacteristic(RxTx);
			
			//Terminate package
			RxTx.setValue("\n");
			gatt.writeCharacteristic(RxTx);
			
			gatt.close();
		}
		
		public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
			//If the characteristic that changed is the serial terminal
			if (characteristic.getUuid().equals(uuidRxTx)) {
				//Get the new values
				byte newBytes[] = characteristic.getValue();

				for (byte incoming : newBytes) {
					switch (incoming) {
					case '\n':
					case '\r':
						//End of packet
						if (position == 16) {
							//Nonce received;
							Log.i(TAG, "Nonce received");
							sendHash(readBuffer, gatt);
						}
						position = 0;
						break;
					default:
						if (position < 511) {
							readBuffer[position++] = incoming;
							readBuffer[position] = 0;
					      }
						break;
					}
				}
			} 
		}
		
		public void onCharacteristicRead(BluetoothGatt gatt, android.bluetooth.BluetoothGattCharacteristic characteristic, int status) {}
		
		public void onCharacteristicWrite(BluetoothGatt gatt, android.bluetooth.BluetoothGattCharacteristic characteristic, int status) {}
		
		public void onConnectionStateChange(android.bluetooth.BluetoothGatt gatt, int status, int newState) {
			switch (newState) {
			case BluetoothProfile.STATE_CONNECTED:
				//On connection, start service discovery
				Log.i(TAG, "Lock Connected");
				gatt.discoverServices();
				break;
			case BluetoothProfile.STATE_DISCONNECTED:
				Log.i(TAG, "Lock Disconnected");
				gatt.close();
				break;
			default:
				break;
			}
		}
		
		public void onServicesDiscovered(android.bluetooth.BluetoothGatt gatt, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				Log.i(TAG, "Services Discovered");
								
				BluetoothGattService serialPort = gatt.getService(uuid);
				if (serialPort == null) {
					throw new UnsupportedOperationException("The device does not have a serial port");
				}
				
				RxTx = serialPort.getCharacteristic(uuidRxTx);
				if (RxTx == null) {
					throw new UnsupportedOperationException("The device does not have the serial port characteristic");
				}
				
				//Set callback for new received data
				gatt.setCharacteristicNotification(RxTx, true);
				
				//On service discovery, send the initial unlock packet.
				RxTx.setValue("UNLOCK\n");
				gatt.writeCharacteristic(RxTx);
			}
		}
	};
	
	private BluetoothGattCharacteristic RxTx;
	
	private final UUID uuid = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");

	private final UUID uuidRxTx= UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
	
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
		
		String address = intent.getStringExtra(EXTRA_ADDR);
		
		if (address == null) {
			throw new NullPointerException("A MAC address is required");
		}
		
		byte[] password = intent.getByteArrayExtra(EXTRA_PASS);
		
		if (password == null) {
			throw new NullPointerException("A password is required");
		}
		
		passwd = password;
		
		
		final BluetoothDevice lock = mBluetoothAdapter.getRemoteDevice(address);
		lock.connectGatt(this, false, mCallback);
		
		return START_NOT_STICKY;
	}

}

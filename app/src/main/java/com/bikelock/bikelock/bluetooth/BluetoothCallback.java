package com.bikelock.bikelock.bluetooth;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class BluetoothCallback extends BluetoothGattCallback{
	private final UUID uuid = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
	private final UUID uuidRxTx= UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
	//private final UUID uuidClientConfig= UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
	private final static String TAG = "BLE CallBack";
	
	private static final int STATE_NOTHING = -1;
	private static final int STATE_CONNECTED = 0;
	private static final int STATE_WRITE = 1;
	private static final int STATE_READ = 2;
	
	private BluetoothGatt mGatt;
	private BluetoothGattCharacteristic RxTx;
	
	private static final int TIMEOUT = 10000;
	private Thread mCurrentThread;
	private volatile int mInterruptState = STATE_NOTHING;
	
	/**
	 * Closes the GATT
	 */
	public final void close(){
		mGatt.close();
	}

	private ArrayDeque<Byte> readBuffer = new ArrayDeque<Byte>();
	private ArrayDeque<Byte[]> lineBuffer = new ArrayDeque<Byte[]>();
	
	public byte[] readLine(){
		
		while (lineBuffer.isEmpty()) {
			waitUntil(TIMEOUT, STATE_READ);			
		}
		
		Byte[] line = lineBuffer.pop();
		byte[] bLine = new byte[line.length];
		for (int i = 0; i < line.length; i++) {
			bLine[i] = (byte) line[i];
		}
		return bLine;		
	}
	
	public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
		//If the characteristic that changed is the serial terminal
		if (characteristic.getUuid().equals(uuidRxTx)) {
			
			//Get the new values
			byte newBytes[] = characteristic.getValue();

			for (byte incoming : newBytes) {
				switch (incoming) {
				case '\r':
					Byte[] tmp = new Byte[readBuffer.size()]; 
					readBuffer.toArray(tmp);
					lineBuffer.add(tmp);
					readBuffer.clear();
					
					mInterruptState = STATE_READ;
					mCurrentThread.interrupt();
					break;
				default:
					readBuffer.add(incoming);
					break;
				}
			}
		} 
	}
	
	public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {}
	
	public void onCharacteristicRead(BluetoothGatt gatt, android.bluetooth.BluetoothGattCharacteristic characteristic, int status) {}
	
	public void onCharacteristicWrite(BluetoothGatt gatt, android.bluetooth.BluetoothGattCharacteristic characteristic, int status) {
		mInterruptState = STATE_WRITE;
		mCurrentThread.interrupt();
	}

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
			
			//Make the connect() method return
			mInterruptState = STATE_CONNECTED;
			mCurrentThread.interrupt();
			
//			BluetoothGattDescriptor descriptor = RxTx.getDescriptor(uuidClientConfig);
//			descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
//			gatt.writeDescriptor(descriptor);
			
		}
	}

	/**
	 * Connects to the bluetooth device
	 * @param device The target device
	 * @param context A handle to the current context
	 * @return True if the connection succeeded, false otherwise
	 */
	public boolean connect(final BluetoothDevice device,final Context context){
		Handler handler = new Handler(Looper.getMainLooper());
		handler.post(new Runnable() {
			@Override
			public void run() {
				mGatt = device.connectGatt(context, false, BluetoothCallback.this);
			}
		});
		
		
		mCurrentThread = Thread.currentThread();	
		return waitUntil(TIMEOUT , STATE_CONNECTED);	
	}

	/**
	 * Writes the supplied bytes to the current device
	 * @param bytes
	 */
	public void writeBytes(byte[] bytes){
		//Split the bytes into the 20 byte blocks supported by android
		List<byte[]> blocks = splitArray(bytes, 20);
		for (byte[] block : blocks) {
			RxTx.setValue(block);
			mGatt.writeCharacteristic(RxTx);
			waitUntil(TIMEOUT, STATE_WRITE);
			
			//Not optional
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {}
			
		}
	}
	
	public void writeString(String string){
		writeBytes(string.getBytes());
	}
	
	private static List<byte[]> splitArray(byte[] items, int maxSubArraySize) {
		  List<byte[]> result = new ArrayList<byte[]>();
		  if (items ==null || items.length == 0) {
		      return result;
		  }

		  int from = 0;
		  int to = 0;
		  int slicedItems = 0;
		  while (slicedItems < items.length) {
		      to = from + Math.min(maxSubArraySize, items.length - to);
		      byte[] slice = Arrays.copyOfRange(items, from, to);
		      result.add(slice);
		      slicedItems += slice.length;
		      from = to;
		  }
		  return result;
	}
	
	/**
	 * Waits until the selected interrupt is raised
	 * @param timeout The requested timeout
	 * @param flag The required flag
	 * @return True if the interrupt was raised, false on timeout failure
	 */
	private boolean waitUntil(int timeout, int flag){
		try {
			Thread.sleep(timeout);
		} catch (InterruptedException e) {
			//If interrupted on time, return true
			if (mInterruptState == flag) {
				mInterruptState = STATE_NOTHING;
				return true;
			} else{
				//If the interrupt was unrelated, keep waiting
				waitUntil(timeout, flag);
			}
		}
		//On timeout, fail
		return false;
	}
}

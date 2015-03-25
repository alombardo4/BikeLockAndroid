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
	private static final int RESET_TIMEOUT = 3;
	private static final int STATE_CLOSING = 4;
	
	private BluetoothGatt mGatt;
	private BluetoothGattCharacteristic RxTx;
	private BluetoothDevice mDevice;
	private final Context mContext;
	
	private static final int TIMEOUT = 3000;
	private volatile int mInterruptState = STATE_NOTHING;
	
	/**
	 * Closes the GATT
	 */
	public final void close(){
		mInterruptState = STATE_CLOSING;
		mGatt.close();
	}

	private final ArrayDeque<Byte> readBuffer;
	private final ArrayDeque<Byte[]> lineBuffer;
	private final Handler mHandler;
	
	/**
	 * Creates a new callback. The constructor must not be called in the UI thread.
	 * @author Michael Chen
	 */
	public BluetoothCallback(Context context) {
		//Initialize the read buffers
		readBuffer = new ArrayDeque<Byte>();
		lineBuffer = new ArrayDeque<Byte[]>();
		
		mContext = context;
		
		mHandler = new Handler(Looper.getMainLooper());
	}
	
	/**
	 * Reads a line; the line must be terminated by a carriage return
	 * @return The sequence of bytes, not including the carriage return character or null if a timeout occured
	 */
	public byte[] readLine(){
		//All operators regarding lineBuffer must be synchronized
		boolean isEmpty;
		synchronized (lineBuffer) {
			isEmpty = lineBuffer.isEmpty();
		}		
		
		//Wait until the buffer is not empty anymore or a timeout occurs
		if (isEmpty) {
			waitUntil(TIMEOUT, STATE_READ);
		}
		
		final Byte[] line;
		
		//Pop the last complete line read.
		synchronized (lineBuffer) {
			//If nothing arrived before the timeout period, quit
			if (lineBuffer.isEmpty()) return null;
			
			//Retrieve the message
			line = lineBuffer.pop();
		}		
		
		//Cast from Byte to byte
		final byte[] bLine = new byte[line.length];
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
					//On carriage return, add the new line to the lineBuffer queue
					
					Byte[] tmp = new Byte[readBuffer.size()]; 
					readBuffer.toArray(tmp);
					
					synchronized (lineBuffer) {
						lineBuffer.add(tmp);
					}
					
					readBuffer.clear();
					
					//If anyone is awaiting a read, awaken them
					notify(STATE_READ);
					
					break;
				default:
					readBuffer.add(incoming);
					break;
				}
			}
		} 
	}
		
	public void onCharacteristicWrite(BluetoothGatt gatt, android.bluetooth.BluetoothGattCharacteristic characteristic, int status) {
		//Continue writing
		notify(STATE_WRITE);
	}

	public void onConnectionStateChange(android.bluetooth.BluetoothGatt gatt, int status, int newState) {
		switch (newState) {
		case BluetoothProfile.STATE_CONNECTED:
			//On connection, start service discovery
			Log.i(TAG, "Lock Connected");
			gatt.discoverServices();
			break;
		case BluetoothProfile.STATE_DISCONNECTED:
			if (mInterruptState == STATE_CLOSING) {
				return;
			}
			Log.i(TAG, "Lock Disconnected");
			
			notify(RESET_TIMEOUT);
			
			close();
			mGatt = mDevice.connectGatt(mContext, false, BluetoothCallback.this);
			
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
			notify(STATE_CONNECTED);
			
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
		mDevice = device;
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				mGatt = device.connectGatt(context, false, BluetoothCallback.this);
			}
		});
			
		return waitUntil(TIMEOUT , STATE_CONNECTED);	
	}

	/**
	 * Writes the supplied bytes to the current device
	 * @param bytes
	 */
	public void writeBytes(byte[] bytes){
		//Split the bytes into the 20 byte blocks supported by android
		final List<byte[]> blocks = splitArray(bytes, 20);
		
		for (final byte[] block : blocks) {
			
			//Write the value from the main thread
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					RxTx.setValue(block);
					mGatt.writeCharacteristic(RxTx);
				}
			});
			
			//Wait until the block has been written
			waitFor(STATE_WRITE);
			
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
	 * @return True if the thread was notified, false on timeout failure
	 */
	private synchronized boolean waitUntil(int timeout, int flag){
		
		//Used to differentiate a timeout exit from a notify exit
		long tBefore=System.nanoTime() / 1000;
		
		while (true){
			try {
				wait(timeout);
				
				//If the right flag was set, resume execution
				if (mInterruptState == flag) {
					mInterruptState = STATE_NOTHING;
					return true;
				}
				
				//Reset the timeout
				if (mInterruptState == RESET_TIMEOUT) {
					mInterruptState = STATE_NOTHING;
					tBefore = System.nanoTime()/1000;
				}
				
				if (System.nanoTime()/ 1000 - tBefore > timeout) {
					//Fail on timeout
					return false;
				}
				
			} catch (InterruptedException e) {
				if (mInterruptState == flag) {
					mInterruptState = STATE_NOTHING;
					return true;
				}
			}
		}
	}
	
	/**
	 * Wakes up the thread
	 * @param flag
	 */
	private synchronized void notify(int flag){
		mInterruptState = flag;
		this.notify();
	}
	
	/**
	 * Waits until the thread is notified.
	 * @param flag
	 */
	private synchronized void waitFor(int flag){
		while (true){

			//If the condition is met, resume execution.
			if (mInterruptState == flag) {
				mInterruptState = STATE_NOTHING;
				return;
			}
			
			//Otherwise, just wait
			try {
				this.wait();
			} catch (InterruptedException e) {}
		}
	}
}

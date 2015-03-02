package com.bikelock.bikelock.gui.adapters;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.bikelock.bikelock.R;
import com.bikelock.bikelock.database.LockDatabase;

import java.util.ArrayList;

/**
 * Created by alec on 3/2/15.
 */
public class FoundDeviceListAdapter extends BaseAdapter {
    private SQLiteDatabase mDb;
    private ArrayList<BluetoothDevice> mDevices;
    private LayoutInflater mLayoutInflater;

    public FoundDeviceListAdapter(Activity activity) {
        mDevices = new ArrayList<BluetoothDevice>();
        mLayoutInflater = activity.getLayoutInflater();
        final LockDatabase dbHelper = new LockDatabase(activity);
        mDb = dbHelper.getReadableDatabase();

    }

    @Override
    public int getCount() {
        return mDevices.size();
    }

    @Override
    public BluetoothDevice getItem(int position) {
        return mDevices.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void addItem(BluetoothDevice device){
        if (mDevices.contains(device)){
            return;
        }

        Cursor c = mDb.query(LockDatabase.TABLE_NAME, new String[]{BaseColumns._ID}, BaseColumns._ID + " = ?", new String[]{device.getAddress()}, null, null, null);
        if (c.moveToFirst()) {
            return;
        }

        mDevices.add(device);
        this.notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = mLayoutInflater.inflate(R.layout.list_item_devices, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else{
            holder = (ViewHolder) convertView.getTag();
        }

        BluetoothDevice item = getItem(position);

        if (item.getName() == null) {
            holder.mName.setText(item.getAddress());
            holder.mAddr.setText("");
        } else {
            holder.mName.setText(item.getName());
            holder.mAddr.setText(item.getAddress());
        }

        return convertView;
    }

    public void clear(){
        mDevices.clear();
    }

    static class ViewHolder{
        private final TextView mName;
        private final TextView mAddr;

        public ViewHolder(View listView) {
            mName = (TextView) listView.findViewById(R.id.name);
            mAddr = (TextView) listView.findViewById(R.id.address);
        }
    }

}
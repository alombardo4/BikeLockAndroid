package com.michaelchentejada.bikelock.gui.adapters;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.michaelchentejada.bikelock.R;
import com.michaelchentejada.bikelock.database.LockDBAdapter;
import com.michaelchentejada.bikelock.database.LockDatabase;
import com.michaelchentejada.bikelock.model.PairedDevice;

import java.util.List;

/**
 * Created by alec on 3/2/15.
 */
public class LockListAdapter extends BaseAdapter {
    private LayoutInflater mLayoutInflater;
    private List<PairedDevice> devices;

    public LockListAdapter(Activity activity) {
        LockDBAdapter dbAdapter = LockDBAdapter.getInstance(activity);
        devices = dbAdapter.getDevices();
        mLayoutInflater = activity.getLayoutInflater();
    }

    @Override
    public int getCount() {
        return devices.size();
    }

    @Override
    public PairedDevice getItem(int position) {
        PairedDevice device = devices.get(position);
        return device;
    }

    @Override
    public long getItemId(int position) {
        return position;
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

        PairedDevice device = devices.get(position);
        if (device.getName() == null) {
            holder.mName.setText(device.getAddress());
            holder.mAddr.setText("");
        } else {
            holder.mName.setText(device.getName());
            holder.mAddr.setText(device.getAddress());
        }

        return convertView;
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

package com.bikelock.bikelock.database;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.util.Pair;

import com.bikelock.bikelock.model.PairedDevice;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by alec on 3/2/15.
 */
public class LockDBAdapter {
    private static LockDBAdapter adapter;
    private static LockDatabase dbHelper;

    public static LockDBAdapter getInstance(Activity activity) {
        if (adapter == null) {
            adapter = new LockDBAdapter();
            dbHelper = new LockDatabase(activity);
        }
        return adapter;
    }

    public void addLock(PairedDevice device) {
        addLock(device.getAddress(), device.getName(), device.getPassword());
    }

    private void addLock(String address, String name, String password) {
        ContentValues values = new ContentValues();
        values.put(BaseColumns._ID, address);
        values.put(LockDatabase.LOCK_NAME, name);
        values.put(LockDatabase.LOCK_PASS, password);
        if (getNumberOfDevices() == 0) {
            values.put(LockDatabase.LOCK_PRIMARY, "1");
        } else {
            values.put(LockDatabase.LOCK_PRIMARY, "0");
        }
        insertItem(values);

    }


    public int getNumberOfDevices() {
        SQLiteDatabase db = getReadable();
        Cursor cursor = db.query(LockDatabase.TABLE_NAME, new String[]{BaseColumns._ID}, null, null, null, null, null);
        int count = cursor.getCount();
        db.close();
        return count;
    }

    public PairedDevice getFirstDevice() {
        SQLiteDatabase db = getReadable();

        String whereClause = LockDatabase.LOCK_PRIMARY + "=?";
        Cursor cursor = db.query(LockDatabase.TABLE_NAME,
                new String[]{BaseColumns._ID, LockDatabase.LOCK_NAME, LockDatabase.LOCK_PASS},
                whereClause, new String[]{"1"}, null, null, null);
        cursor.moveToFirst();
        db.close();
        if (cursor.getCount() > 0) {
            return new PairedDevice(cursor.getString(0), cursor.getString(1), cursor.getString(2));
        } else {
            return null;
        }

    }

    public List<PairedDevice> getDevices() {
        List<PairedDevice> devices = new ArrayList<>();
        SQLiteDatabase db = getReadable();
        Cursor cursor = db.query(LockDatabase.TABLE_NAME, new String[]{BaseColumns._ID, LockDatabase.LOCK_NAME, LockDatabase.LOCK_PASS}, null, null, null, null, null);
        for (int i = 0; i < cursor.getCount(); i++) {
            cursor.moveToPosition(i);
            PairedDevice device = new PairedDevice(cursor.getString(0), cursor.getString(1), cursor.getString(2));
            devices.add(device);
        }
        db.close();
        return devices;
    }

    public void updateDevice(PairedDevice device) {
        SQLiteDatabase db = getWriteable();
        ContentValues values = new ContentValues();
        values.put(LockDatabase.LOCK_NAME, device.getName());
        values.put(LockDatabase.LOCK_PASS, device.getPassword());
        String whereClause = BaseColumns._ID + "=?";
        db.update(LockDatabase.TABLE_NAME, values, whereClause, new String[] {device.getAddress()});
        db.close();
    }

    public void deleteDevice(PairedDevice device) {
        SQLiteDatabase db = getWriteable();
        String whereClause = BaseColumns._ID + "=?";
        db.delete(LockDatabase.TABLE_NAME, whereClause, new String[]{device.getAddress()});
        db.close();
    }
    private SQLiteDatabase getWriteable() {
        return dbHelper.getWritableDatabase();
    }

    private SQLiteDatabase getReadable() {
        return dbHelper.getReadableDatabase();
    }

    private void insertItem(ContentValues values) {
        SQLiteDatabase db = getWriteable();
        db.insert(LockDatabase.TABLE_NAME, null, values);
        db.close();
    }

    public void setPrimaryDevice(PairedDevice device) {
        SQLiteDatabase db = getWriteable();
        ContentValues values = new ContentValues();
        values.put(LockDatabase.LOCK_PRIMARY, "0");
        db.update(LockDatabase.TABLE_NAME, values, null, null);
        values = new ContentValues();
        values.put(LockDatabase.LOCK_PRIMARY, "1");
        String whereClause = BaseColumns._ID + "=?";
        db.update(LockDatabase.TABLE_NAME, values, whereClause, new String[] {device.getAddress()});
        db.close();
    }


}

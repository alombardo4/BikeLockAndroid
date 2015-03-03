package com.bikelock.bikelock.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

public class LockDatabase extends SQLiteOpenHelper {

	public static final String DATABASE_NAME = "Lock.db";
	public static final String TABLE_NAME = "LOCKDB";
	public static final String LOCK_NAME = "NAME";
	public static final String LOCK_PASS = "PASSWD";

	public LockDatabase(Context context) {
		super(context, DATABASE_NAME, null, 1);
	}
	
	private static final String CREATE_COMMAND = "CREATE TABLE " + TABLE_NAME + " ("
			+ BaseColumns._ID + " TEXT PRIMARY KEY, "
			+ LOCK_NAME + " TEXT,"
			+ LOCK_PASS + " TEXT)";
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(CREATE_COMMAND);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	}

}

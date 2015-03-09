package com.bikelock.bikelock.gui.views;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.widget.Toast;

import com.bikelock.bikelock.R;
import com.bikelock.bikelock.database.LockDBAdapter;

public class MainActivity extends ActionBarActivity {
	static Toolbar toolbar;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        LockDBAdapter adapter = LockDBAdapter.getInstance(this);
        int count = adapter.getNumberOfDevices();
        if (count >= 1) {
            FragmentTransaction fr = getSupportFragmentManager().beginTransaction();
            fr.add(R.id.container, HomeFragment.newInstance(), null)
                    .commit();
        } else {
            FragmentTransaction fr = getSupportFragmentManager().beginTransaction();
            fr.add(R.id.container, new NewLockFragment(), null)
                    .commit();

        }

    }
    @Override
    public void onBackPressed(){

        // here remove code for your last fragment
        super.onBackPressed();

    }

}

package com.bikelock.bikelock.gui.views;

import android.app.FragmentTransaction;
import android.os.Bundle;
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
        if (count == 1) {
            FragmentTransaction fr = getFragmentManager().beginTransaction();
            fr.replace(R.id.container, HomeFragment.newInstance(), HomeFragment.class.getName())
                    .addToBackStack(HomeFragment.class.getName())
                    .commit();
        } else {
            FragmentTransaction fr = getFragmentManager().beginTransaction();
            fr.replace(R.id.container, new NewLockFragment(), NewLockFragment.class.getName())
                    .addToBackStack(NewLockFragment.class.getName())
                    .commit();

        }

    }

}

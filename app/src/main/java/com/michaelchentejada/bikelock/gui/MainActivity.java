package com.michaelchentejada.bikelock.gui;

import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;

import com.michaelchentejada.bikelock.R;

public class MainActivity extends ActionBarActivity {
	private Toolbar toolbar;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        FragmentTransaction fr = getFragmentManager().beginTransaction();
		fr.replace(R.id.container, new MainScreen(), MainScreen.class.getName()).addToBackStack(MainScreen.class.getName());
		fr.commit();
	}
}

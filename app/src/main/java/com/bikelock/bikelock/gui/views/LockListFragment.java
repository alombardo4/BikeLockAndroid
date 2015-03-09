package com.bikelock.bikelock.gui.views;


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.bikelock.bikelock.R;
import com.bikelock.bikelock.database.LockDBAdapter;
import com.bikelock.bikelock.gui.adapters.LockListAdapter;
import com.melnykov.fab.FloatingActionButton;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link LockListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LockListFragment extends Fragment {
    private ListView lockList;
    private LockListAdapter adapter;
    private FloatingActionButton fab;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment LockListFragment.
     */
    public static LockListFragment newInstance() {
        LockListFragment fragment = new LockListFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public LockListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onResume() {
        super.onResume();
        MainActivity.toolbar.setTitle(getString(R.string.your_locks));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_lock_list, container, false);
        lockList = (ListView) view.findViewById(R.id.lock_list);
        fab = (FloatingActionButton) view.findViewById(R.id.fab);
        fab.setOnClickListener(new FABOnClickListener());


        adapter = new LockListAdapter(getActivity());
        lockList.setOnItemClickListener(new ListItemClickListener());
        lockList.setAdapter(adapter);


        return view;
    }

    private class ListItemClickListener implements AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            LockDBAdapter ad = LockDBAdapter.getInstance(getActivity());
            ad.setPrimaryDevice(adapter.getItem(position));
            FragmentManager fm = getActivity().getSupportFragmentManager();
            startActivity(new Intent(getActivity().getBaseContext(), MainActivity.class));
            getActivity().finish();
        }
    }

    private class FABOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.container, new NewLockFragment(), null)
                    .addToBackStack(null)
                    .commit();

        }
    }

}

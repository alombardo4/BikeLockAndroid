package com.bikelock.bikelock.gui.views;


import android.content.Intent;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.bikelock.bikelock.R;
import com.bikelock.bikelock.bluetooth.BikeLockBluetoothLeService;
import com.bikelock.bikelock.database.LockDBAdapter;
import com.bikelock.bikelock.model.PairedDevice;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link HomeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HomeFragment extends Fragment {
    private PairedDevice device;
    private Button connect, edit, delete;
    private TextView name;


    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment HomeFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static HomeFragment newInstance() {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

        }
        LockDBAdapter adapter = LockDBAdapter.getInstance(getActivity());
        device = adapter.getFirstDevice();


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        name = (TextView) view.findViewById(R.id.lock_name);
        connect = (Button) view.findViewById(R.id.unlock_button);
        edit = (Button) view.findViewById(R.id.edit_button);
        delete = (Button) view.findViewById(R.id.delete_button);

        connect.setOnClickListener(new ConnectListener());
        name.setText(device.getName());
        return view;

    }

    private class ConnectListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            connectToDevice(device);
            Toast.makeText(getActivity().getBaseContext(), getString(R.string.connecting_to_lock), Toast.LENGTH_SHORT).show();
        }

        private void connectToDevice(PairedDevice device) {
            Intent blService = new Intent(getActivity(), BikeLockBluetoothLeService.class);
            blService.putExtra(BikeLockBluetoothLeService.EXTRA_ADDR, device.getAddress());
            blService.putExtra(BikeLockBluetoothLeService.EXTRA_PASS, new byte[] {0,1,2,3,4,5,6,7,8,9,0xa,0xb,0xc,0xd,0xe,0xf}); //TODO replace with password
            System.out.println(blService.getExtras().get(BikeLockBluetoothLeService.EXTRA_PASS));
            getActivity().startService(blService);
        }
    }



}

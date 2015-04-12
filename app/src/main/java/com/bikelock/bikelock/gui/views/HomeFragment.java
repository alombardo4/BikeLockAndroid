package com.bikelock.bikelock.gui.views;


import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
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
        setHasOptionsMenu(true);

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.main_fragment, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_list:
                FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.container, new LockListFragment())
                        .addToBackStack(null)
                        .commit();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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
        edit.setOnClickListener(new EditListener());
        delete.setOnClickListener(new DeleteListener());
        name.setText(device.getName());
        return view;

    }
    @Override
    public void onResume() {
        super.onResume();
        MainActivity.toolbar.setTitle(getString(R.string.bike_lock));
    }
    private class DeleteListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            DeleteDevicePopupDialog deleteDevicePopupDialog = new DeleteDevicePopupDialog();
            deleteDevicePopupDialog.show(getActivity().getSupportFragmentManager(), DeleteDevicePopupDialog.class.getName());
        }
    }
    private class EditListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            EditNamePopupDialog editNamePopupDialog = new EditNamePopupDialog();
            editNamePopupDialog.show(getActivity().getSupportFragmentManager(), EditNamePopupDialog.class.getName());
        }
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
            blService.putExtra(BikeLockBluetoothLeService.EXTRA_PASS, device.getPassword());

//            blService.putExtra(BikeLockBluetoothLeService.EXTRA_PASS, new byte[] {0,1,2,3,4,5,6,7,8,9,0xa,0xb,0xc,0xd,0xe,0xf});
            System.out.println(blService.getExtras().get(BikeLockBluetoothLeService.EXTRA_PASS));
            getActivity().startService(blService);
        }
    }

    private class EditNamePopupDialog extends DialogFragment {

        private EditText nameField, passwordField, confirmPasswordField;

        public EditNamePopupDialog() {

        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View view = inflater.inflate(R.layout.edit_name_popup_dialog, null);
            nameField = (EditText) view.findViewById(R.id.edit_name_edittext);
            passwordField = (EditText) view.findViewById(R.id.new_password);
            confirmPasswordField = (EditText) view.findViewById(R.id.new_password_confirm);
            nameField = (EditText) view.findViewById(R.id.edit_name_edittext);

            nameField.setText(device.getName());

            builder.setView(view)
                    .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            device.setName(nameField.getText().toString());
                            name.setText(device.getName());
                            LockDBAdapter dbAdapter = LockDBAdapter.getInstance(getActivity());
                            dbAdapter.updateDevice(device);
                            String password = passwordField.getText().toString().trim();
                            String confirm = confirmPasswordField.getText().toString().trim();
                            if (!password.equals("") && !confirm.equals("")) {
                                if (password.equals(confirm)) {
                                    Toast.makeText(getActivity(), getString(R.string.changing_password), Toast.LENGTH_SHORT).show();
                                    SaveNewPasswordDialog saveDialog = new SaveNewPasswordDialog(password);
                                    saveDialog.show(getActivity().getSupportFragmentManager(), SaveNewPasswordDialog.class.getName());
                                } else {
                                    Toast.makeText(getActivity(), getString(R.string.password_mismatch), Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User cancelled the dialog
                        }
                    });
            // Create the AlertDialog object and return it
            return builder.create();
        }
    }

    private class DeleteDevicePopupDialog extends DialogFragment {


        public DeleteDevicePopupDialog() {

        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            builder.setMessage(getString(R.string.remove_device_confirmation))
                    .setPositiveButton(R.string.remove, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            LockDBAdapter dbAdapter = LockDBAdapter.getInstance(getActivity());
                            dbAdapter.deleteDevice(device);
                            FragmentTransaction ft = ((android.support.v7.app.ActionBarActivity) getActivity()).getSupportFragmentManager().beginTransaction();
                            ft.replace(R.id.container, LockListFragment.newInstance(), LockListFragment.class.getName())
                                    .commit();
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User cancelled the dialog
                        }
                    });
            // Create the AlertDialog object and return it
            return builder.create();
        }
    }


    private class SaveNewPasswordDialog extends DialogFragment {

        private String password;
        public SaveNewPasswordDialog(String password) {
            this.password = password;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            builder.setMessage(getString(R.string.did_password_change_message))
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            LockDBAdapter dbAdapter = LockDBAdapter.getInstance(getActivity());
                            String oldPassword = device.getPassword();
                            device.setPassword(password);
                            dbAdapter.updateDevice(device);
                            Intent blService = new Intent(getActivity(), BikeLockBluetoothLeService.class);
                            blService.putExtra(BikeLockBluetoothLeService.EXTRA_ADDR, device.getAddress());
                            blService.putExtra(BikeLockBluetoothLeService.EXTRA_PASS, oldPassword);
                            blService.putExtra(BikeLockBluetoothLeService.EXTRA_NEW_PASS, password);
                            getActivity().startService(blService);
                        }
                    })
                    .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User cancelled the dialog
                        }
                    });
            // Create the AlertDialog object and return it
            return builder.create();
        }
    }
}

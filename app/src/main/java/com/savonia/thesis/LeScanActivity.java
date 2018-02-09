package com.savonia.thesis;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class LeScanActivity extends AppCompatActivity {

    private final static int REQUEST_ENABLE_BT = 1;
    private final static int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private BluetoothAdapter mBluetoothAdapter;
    private Button lookUp;
    private Button stopLookUp;
    private ProgressBar spinner;
    private TextView lookUpText;
    private boolean mScanning;
    private Handler mHandler;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private ListView devicesList;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_le_scan);
        lookUp =  (Button) findViewById(R.id.lookUpBtn);
        stopLookUp = (Button) findViewById(R.id.stopLookUpBtn);
        spinner = (ProgressBar) findViewById(R.id.spinner);
        lookUpText = (TextView) findViewById(R.id.lookUpTextView);
        devicesList = (ListView) findViewById(R.id.devices_list);

        mHandler = new Handler();
        mLeDeviceListAdapter = new LeDeviceListAdapter();

        Drawable progressDrawable = spinner.getIndeterminateDrawable().mutate();
        progressDrawable.setColorFilter(getColor(R.color.colorPrimary), PorterDuff.Mode.MULTIPLY);
        spinner.setProgressDrawable(progressDrawable);

        // Initially, the 'Stop' button and the 'spinner' are invisible
        stopLookUp.setVisibility(View.INVISIBLE);
        spinner.setVisibility(View.GONE);

        lookUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (isAccessFineLocationAllowed())
                    scanLeDevice(true);
                else
                    requestAccessFineLocation();
            }
        });

        stopLookUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanLeDevice(false);
            }
        });

        // Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

    }

    private void startScanning() {
        mScanning = true;
        lookUp.setVisibility(View.INVISIBLE);
        stopLookUp.setVisibility(View.VISIBLE);
        spinner.setVisibility(View.VISIBLE);
        lookUpText.setText(getResources().getString(R.string.look_up_text_view_scanning));
    }

    private void stopScanning() {
        mScanning = false;
        stopLookUp.setVisibility(View.INVISIBLE);
        spinner.setVisibility(View.INVISIBLE);
        lookUp.setVisibility(View.VISIBLE);
        lookUpText.setText(getResources().getString(R.string.look_up_text_view_initial));
    }

    private void scanFinished() {
        mScanning = false;
        stopLookUp.setVisibility(View.INVISIBLE);
        spinner.setVisibility(View.INVISIBLE);
        lookUp.setVisibility(View.INVISIBLE);
        lookUpText.setVisibility(View.INVISIBLE);

        // replacing old layout with listView
        devicesList.setVisibility(View.VISIBLE);
        devicesList.setAdapter(mLeDeviceListAdapter);
        // TODO add refresh button to the toolbar and invoke it from here
    }


    private boolean isAccessFineLocationAllowed() {
        if (ContextCompat.checkSelfPermission(LeScanActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)
            return false;
        else
            return true;

    }
    private void requestAccessFineLocation() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(LeScanActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION)) {

            // If the user denied the permission previously, it will be shown again
            ActivityCompat.requestPermissions(LeScanActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);

        } else {
            ActivityCompat.requestPermissions(LeScanActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Toast.makeText(LeScanActivity.this, "Permission Granted!",
                            Toast.LENGTH_SHORT).show();

                    startScanning();

                } else {

                    Toast.makeText(LeScanActivity.this, "Permission Denied!",
                            Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void scanLeDevice(final boolean enable) {

        final BluetoothLeScanner bluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        if (enable) {

            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(mLeDeviceListAdapter.getCount() == 0) {
                        stopScanning();
                        lookUpText.setText(getResources().getString(R.string.look_up_text_view_scanning_failed));
                    }
                    bluetoothLeScanner.stopScan(myLeScanCallback);
                }
            }, SCAN_PERIOD);


            startScanning();
            bluetoothLeScanner.startScan(myLeScanCallback);

        } else {
            stopScanning();
            bluetoothLeScanner.stopScan(myLeScanCallback);
        }
    }


    private ScanCallback myLeScanCallback = new ScanCallback() {

        // TODO: read about ScanCallback
        @Override
        public void onScanResult(int callbackType, final ScanResult result) {

            super.onScanResult(callbackType, result);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    scanFinished();
                    mLeDeviceListAdapter.addDevice(result.getDevice());
                    mLeDeviceListAdapter.notifyDataSetChanged();
                }
            });
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            if(mLeDeviceListAdapter.getCount() == 0) {
                stopScanning();
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            if(mLeDeviceListAdapter.getCount() == 0) {
                stopScanning();
            }
        }

    };

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = LeScanActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            if(!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int position) {
            return mLeDevices.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.devices_item, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceName = (TextView) view.findViewById(R.id.deviceNameTxtV);
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.deviceAddressTxtV);
                viewHolder.connect_btn = (Button) view.findViewById(R.id.connect_button);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(position);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());

            viewHolder.connect_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //TODO connecting to the selected device
                }
            });

            return view;
        }
    }


    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
        Button connect_btn;
    }

}
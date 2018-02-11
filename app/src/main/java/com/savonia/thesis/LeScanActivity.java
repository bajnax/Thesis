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
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
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
    private Handler mHandler;
    private Toolbar toolBar;
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
        toolBar = (Toolbar) findViewById(R.id.tool_bar);
        setSupportActionBar(toolBar);

        mHandler = new Handler();
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        devicesList.setAdapter(mLeDeviceListAdapter);

        Drawable progressDrawable = spinner.getIndeterminateDrawable().mutate();
        progressDrawable.setColorFilter(getColor(R.color.colorPrimary), PorterDuff.Mode.MULTIPLY);
        spinner.setProgressDrawable(progressDrawable);

        // Initially, the 'Stop' button and the 'spinner' are gone
        stopLookUp.setVisibility(View.GONE);
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


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds 'refresh' item to the action bar
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh: {
                Toast.makeText(this, "Refresh selected", Toast.LENGTH_SHORT).show();
                //TODO create 'refresh' method and call it from here
            }
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }


    @Override
    public void onPause() {
        super.onPause();

        scanLeDevice(false);
    }

    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLeDeviceListAdapter.clear();
                mLeDeviceListAdapter.notifyDataSetChanged();
            }
        });

        //mLeDeviceListAdapter.clear();
        //mLeDeviceListAdapter.notifyDataSetChanged();
        stopScanning();
    }

    private void startScanning() {
        // if the listView is empty, then layout with 'stop' button,
        // spinner and textView becomes visible
        if(mLeDeviceListAdapter.getCount() == 0) {
            lookUp.setVisibility(View.GONE);

            stopLookUp.setVisibility(View.VISIBLE);
            spinner.setVisibility(View.VISIBLE);
            lookUpText.setVisibility(View.VISIBLE);
            lookUpText.setText(getResources().getString(R.string.look_up_text_view_scanning));
        }
    }

    private void stopScanning() {
        // if the listView is empty, then layout with 'scan' button
        // and textView is shown
        if(mLeDeviceListAdapter.getCount() == 0) {
            devicesList.setVisibility(View.GONE);
            stopLookUp.setVisibility(View.GONE);
            spinner.setVisibility(View.GONE);

            lookUp.setVisibility(View.VISIBLE);
            lookUpText.setVisibility(View.VISIBLE);
            lookUpText.setText(getResources().getString(R.string.look_up_text_view_initial));
        }
    }

    private void scanFinished() {
        stopLookUp.setVisibility(View.GONE);
        spinner.setVisibility(View.GONE);
        lookUp.setVisibility(View.GONE);
        lookUpText.setVisibility(View.GONE);

        // replacing old layout with listView
        devicesList.setVisibility(View.VISIBLE);

        // TODO add refresh button to the toolbar and set it to 'VISIBLE' here
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
                    stopScanning();
                    lookUpText.setText(getResources().getString(R.string.look_up_text_view_scanning_failed));
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

    // Adapter for holding devices, which are found during scanning.
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

                // TODO insert an icon into the 'connect' button intead of the text
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
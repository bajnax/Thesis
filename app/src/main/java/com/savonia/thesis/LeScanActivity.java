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
    private ProgressBar spinner;
    private TextView lookUpText;
    private Handler mHandler;
    private Toolbar toolBar;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private ListView devicesList;
    private boolean isScanning = false;
    // Stops scanning after 7 seconds.
    private static final long SCAN_PERIOD = 7000;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_le_scan);
        lookUp =  (Button) findViewById(R.id.lookUpBtn);
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
        spinner.setVisibility(View.GONE);

        lookUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isScanning)
                    scanLeDevice(false);
                else
                    scanLeDevice(true);
            }
        });

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE is not supported on your device!",
                    Toast.LENGTH_LONG).show();
            finish();
        } else {
            // Initializes Bluetooth adapter.
            final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = bluetoothManager.getAdapter();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds 'refresh' item to the action bar
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if(isScanning) {
            menu.findItem(R.id.action_refresh).setVisible(false);
            menu.findItem(R.id.action_pause_scanning).setVisible(true);
        } else {
            menu.findItem(R.id.action_refresh).setVisible(true);
            menu.findItem(R.id.action_pause_scanning).setVisible(false);
        }
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh: {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mLeDeviceListAdapter.clear();
                        mLeDeviceListAdapter.notifyDataSetChanged();
                    }
                });
                scanLeDevice(true);

                return true;
            }
            case R.id.action_pause_scanning: {
                if(isScanning)
                    scanLeDevice(false);

                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }

    }


    @Override
    public void onPause() {
        super.onPause();

        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            scanLeDevice(false);
        }
    }


    @Override
    public void onResume() {
        super.onResume();

        // if BLE is not working
        checkBluetooth();

        // if GPS is not working
        checkLocationServices();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLeDeviceListAdapter.clear();
                mLeDeviceListAdapter.notifyDataSetChanged();
            }
        });
        stopScanning();
    }


    private void startScanning() {

        // if the listView is empty, then layout with 'stop' button,
        // spinner and textView becomes visible
        if(mLeDeviceListAdapter.getCount() == 0) {
            invalidateOptionsMenu();

            lookUp.setVisibility(View.VISIBLE);
            lookUp.setText(R.string.stop_look_up_btn_txt);
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
            spinner.setVisibility(View.GONE);
            invalidateOptionsMenu();

            lookUp.setVisibility(View.VISIBLE);
            lookUp.setText(R.string.look_up_btn_txt);
            lookUpText.setVisibility(View.VISIBLE);
            lookUpText.setText(getResources().getString(R.string.look_up_text_view_initial));
        }
    }


    private void scanFinished() {
        lookUp.setText(R.string.look_up_btn_txt);
        spinner.setVisibility(View.GONE);
        lookUp.setVisibility(View.GONE);
        lookUpText.setVisibility(View.GONE);
        invalidateOptionsMenu();

        // replacing old layout with listView
        devicesList.setVisibility(View.VISIBLE);
    }


    private void scanLeDevice(final boolean enable) {

        if (isAccessFineLocationAllowed()) {

            checkLocationServices();
            checkBluetooth();

            final BluetoothLeScanner bluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

            if (enable) {
                // Stops scanning after a pre-defined scan period
                // if some BLE device is found

                // Otherwise, the user should stop scanning manually
                // or pause the app
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (mLeDeviceListAdapter.getCount() > 0) {
                            // if the listView is filled, but scanning was still 'on' in the background
                            bluetoothLeScanner.stopScan(myLeScanCallback);
                            isScanning = false;
                            invalidateOptionsMenu();
                            Toast.makeText(LeScanActivity.this, "Stopped scanning!",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                }, SCAN_PERIOD);

                bluetoothLeScanner.startScan(myLeScanCallback);
                isScanning = true;
                startScanning();

            } else {
                bluetoothLeScanner.stopScan(myLeScanCallback);
                isScanning = false;
                stopScanning();
            }
        } else {
            requestAccessFineLocation();
            checkBluetooth();
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
            /*if(mLeDeviceListAdapter.getCount() == 0) {
                stopScanning();
            }*/
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            /*if(mLeDeviceListAdapter.getCount() == 0) {
                stopScanning();
            }*/
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

            if (view == null) {
                view = mInflator.inflate(R.layout.devices_item, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceName = (TextView) view.findViewById(R.id.deviceNameTxtV);
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.deviceAddressTxtV);

                // TODO insert an icon into the 'connect' button instead of the text
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
                    //TODO connecting to the selected device onClick of 'connect' button
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

    private boolean isAccessFineLocationAllowed() {
        if (ContextCompat.checkSelfPermission(LeScanActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)
            return false;
        else
            return true;

    }

    private void requestAccessFineLocation() {

        ActivityCompat.requestPermissions(LeScanActivity.this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Toast.makeText(LeScanActivity.this, "Permission Granted!",
                            Toast.LENGTH_SHORT).show();

                } else {
                    Toast.makeText(LeScanActivity.this, "Permission Denied! The app " +
                                    "won't function without this permission",
                            Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == LeScanActivity.RESULT_CANCELED) {

                //Bluetooth not enabled.
                Toast.makeText(LeScanActivity.this, "The app " +
                                "won't function if you don't enable BLE!",
                        Toast.LENGTH_LONG).show();

                finish();
            }
        }
    }


    // Enables Bluetooth if it is disabled
    public void checkBluetooth () {
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }


    // Checks the location services' status
    public void checkLocationServices() {

        // TODO: startActivityForResult to enable GPS
        // if unavailable, call 'finish()'

    }

}
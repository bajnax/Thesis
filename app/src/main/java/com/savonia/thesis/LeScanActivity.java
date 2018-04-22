package com.savonia.thesis;

import android.Manifest;
import android.animation.LayoutTransition;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
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

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import java.util.ArrayList;
import java.util.List;

public class LeScanActivity extends AppCompatActivity {

    private final static int REQUEST_ENABLE_BT = 1;
    private final static int REQUEST_ENABLE_LS = 3;
    private final static int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 2;
    private final static long SCAN_PERIOD = 8000;

    private Handler mHandler;
    private BluetoothAdapter mBluetoothAdapter;
    private Button lookUp;
    private ProgressBar spinner;
    private TextView lookUpText;
    private TextView scanningStatusText;
    private Toolbar toolBar;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private ListView devicesList;
    private boolean isScanning = false;
    private boolean isGpsEnabled;
    private boolean isNetworkEnabled;
    private LocationManager mLocationManager;
    private GoogleApiClient googleApiClient;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_le_scan);
        lookUp =  (Button) findViewById(R.id.lookUpBtn);
        spinner = (ProgressBar) findViewById(R.id.spinner);
        lookUpText = (TextView) findViewById(R.id.lookUpTextView);
        scanningStatusText = (TextView) findViewById(R.id.scanningStatusTextView);
        devicesList = (ListView) findViewById(R.id.devices_list);
        toolBar = (Toolbar) findViewById(R.id.tool_bar);
        setSupportActionBar(toolBar);
        mHandler = new Handler();


        // Animating layout changes
        ((ViewGroup) findViewById(R.id.le_scan_id)).getLayoutTransition()
                .enableTransitionType(LayoutTransition.CHANGING);

        mLeDeviceListAdapter = new LeDeviceListAdapter();
        devicesList.setAdapter(mLeDeviceListAdapter);

        Drawable progressDrawable = spinner.getIndeterminateDrawable().mutate();
        progressDrawable.setColorFilter(getColor(R.color.colorPrimary), PorterDuff.Mode.MULTIPLY);
        spinner.setProgressDrawable(progressDrawable);
        spinner.setVisibility(View.INVISIBLE);

        lookUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isScanning)
                    scanLeDevice(true);
            }
        });

        // Comparing hardware's compatibility with the requirements of the app
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE is not supported on your device!",
                    Toast.LENGTH_LONG).show();
            finish();
        } else {
            try {
                // Initializing Bluetooth adapter.
                final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
                if (bluetoothManager != null)
                    mBluetoothAdapter = bluetoothManager.getAdapter();

                // Initializing location manager
                mLocationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
            } catch(NullPointerException ex) {
                ex.printStackTrace();
            }

            // Enabling Bluetooth, if it is disabled
            checkBluetooth();

            if (!isAccessFineLocationAllowed()) {
                // Requesting location permissions, if not granted
                requestAccessFineLocation();
            }
            else if(!isLocationEnabled()){
                // Enabling location services, if they are disabled
                enableLocation();
            }

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
            menu.findItem(R.id.action_refresh).setVisible(true);
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

                if(!isScanning)
                    scanLeDevice(true);

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

        mLeDeviceListAdapter.clear();
        mLeDeviceListAdapter.notifyDataSetChanged();
        stopScanning();
    }


    private void startScanning() {

        // if the listView is empty, then layout with
        // spinner and textView becomes visible, the button becomes gone
        if(mLeDeviceListAdapter.getCount() == 0) {
            invalidateOptionsMenu();

            lookUp.setVisibility(View.INVISIBLE);
            lookUpText.setVisibility(View.VISIBLE);
            lookUpText.setText(getResources().getString(R.string.look_up_text_view_scanning));
        }

        // noticeable changes of the scanning status in the layout
        scanningStatusText.setText(R.string.scanning_status_active);
        spinner.setVisibility(View.VISIBLE);
    }


    private void stopScanning() {

        // if the listView is empty, then layout with 'scan' button
        // and textView is shown
        if(mLeDeviceListAdapter.getCount() == 0) {
            devicesList.setVisibility(View.INVISIBLE);
            invalidateOptionsMenu();

            lookUp.setVisibility(View.VISIBLE);
            lookUpText.setVisibility(View.VISIBLE);
            lookUpText.setText(getResources().getString(R.string.look_up_text_view_initial));
        }

        // noticeable changes of the scanning status in the layout
        scanningStatusText.setText(R.string.scanning_status_inactive);
        spinner.setVisibility(View.INVISIBLE);
    }

    // in case any devices are found
    private void scanFinished() {
        lookUp.setVisibility(View.INVISIBLE);
        lookUpText.setVisibility(View.INVISIBLE);
        invalidateOptionsMenu();

        // listView with devices shows up
        devicesList.setVisibility(View.VISIBLE);
    }


    private void scanLeDevice(final boolean enable) {

        final BluetoothLeScanner bluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        // before scanning, the app makes sure that bluetooth and
        // location services are turned on and required permissions are granted
        if (isAccessFineLocationAllowed() && mBluetoothAdapter.isEnabled() && isLocationEnabled()) {

            if (enable) {

                if(!isScanning) {

                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (isScanning) {
                                isScanning = false;
                                bluetoothLeScanner.stopScan(myLeScanCallback);
                                stopScanning();
                            }
                        }
                    }, SCAN_PERIOD);


                    bluetoothLeScanner.startScan(myLeScanCallback);
                    isScanning = true;
                    startScanning();
                }

            } else {
                bluetoothLeScanner.stopScan(myLeScanCallback);
                isScanning = false;
                stopScanning();
            }
        } else {

            if(enable) {
                if (!isAccessFineLocationAllowed()) {
                    requestAccessFineLocation();
                }

                checkBluetooth();

                if (isAccessFineLocationAllowed() && !isLocationEnabled())
                    enableLocation();

            } else {
                // if location was disabled during scanning
                if(mBluetoothAdapter.isEnabled())
                    bluetoothLeScanner.stopScan(myLeScanCallback);

                isScanning = false;
                stopScanning();
            }
        }

    }


    private ScanCallback myLeScanCallback = new ScanCallback() {

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
        public void onBatchScanResults(final List<ScanResult> results) {
            super.onBatchScanResults(results);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    scanFinished();
                    for(ScanResult result: results) {
                        mLeDeviceListAdapter.addDevice(result.getDevice());
                        mLeDeviceListAdapter.notifyDataSetChanged();
                    }
                }
            });
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Toast.makeText(LeScanActivity.this,
                    "onScanFailed: " + String.valueOf(errorCode),
                    Toast.LENGTH_LONG).show();
            stopScanning();
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

            // implementing ViewHolder design pattern to increase performance
            ViewHolder viewHolder;

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

            final BluetoothDevice device = mLeDevices.get(position);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());

            viewHolder.connect_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(device.getAddress().equals(GattAttributesSample.DEVICE_ADDRESS)) {
                        Intent connectToDevice = new Intent();
                        connectToDevice.setClass(LeScanActivity.this, LeConnectedDeviceActivity.class);
                        connectToDevice.putExtra("deviceAddress", GattAttributesSample.DEVICE_ADDRESS);
                        startActivity(connectToDevice);
                    }
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
            }
        } else if (requestCode == REQUEST_ENABLE_LS) {

            switch (resultCode) {
                case LeScanActivity.RESULT_OK:
                    // All required changes were successfully made

                    break;
                case LeScanActivity.RESULT_CANCELED:
                    Toast.makeText(LeScanActivity.this, "The app " +
                                    "won't function without enabled location services",
                            Toast.LENGTH_LONG).show();
                    break;
                default:
                    break;
            }
        }
    }


    // Enables Bluetooth if it is disabled
    public void checkBluetooth () {
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }


    public boolean isLocationEnabled() {
        if(isAccessFineLocationAllowed()) {

            if(mLocationManager == null) {
                mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            }

            isGpsEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            isNetworkEnabled = mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if(!isGpsEnabled && !isNetworkEnabled)
                return false;
            else
                return true;
        } else
            return false;
    }


    public void enableLocation() {

        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(LeScanActivity.this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                        @Override
                        public void onConnected(Bundle bundle) {

                        }

                        @Override
                        public void onConnectionSuspended(int i) {
                            googleApiClient.connect();
                        }
                    })
                    .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                        @Override
                        public void onConnectionFailed(ConnectionResult connectionResult) {

                            Log.d("Location error", "Location error " + connectionResult.getErrorCode());
                        }
                    }).build();
            googleApiClient.connect();
        }

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(30 * 1000);
        locationRequest.setFastestInterval(5 * 1000);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        builder.setAlwaysShow(true);

        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        try {
                            // requesting to enable Bluetooth
                            status.startResolutionForResult(LeScanActivity.this, REQUEST_ENABLE_LS);

                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                        }
                        break;
                }
            }
        });
    }
}
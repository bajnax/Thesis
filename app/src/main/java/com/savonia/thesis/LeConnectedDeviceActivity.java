package com.savonia.thesis;

import android.Manifest;
import android.animation.LayoutTransition;
import android.app.ActivityManager;
import android.app.Service;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.PopupMenu;
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
import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.LabelFormatter;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.PointsGraphSeries;
import com.savonia.thesis.db.SensorsValuesDatabase;
import com.savonia.thesis.db.entity.Gas;
import com.savonia.thesis.db.entity.Temperature;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class LeConnectedDeviceActivity extends AppCompatActivity implements OnFragmentInteractionListener<Object> {

    private final static String TAG = LeConnectedDeviceActivity.class.getSimpleName();

    private final static int REQUEST_ENABLE_BT = 1;
    private final static String DEVICE_STATE = "DeviceConnection";
    private final static String BOUND_STATE = "BindingState";
    private final static String RECEIVED_SERVICES = "ServicesReceived";
    private final static String CONNECTION_TYPE = "AutoconnectionOrDirect";
    private final static String RECEIVING_DATA = "ReceivingData";

    private final static int REQUEST_ENABLE_LS = 3;
    private final static int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 2;

    private SimpleDateFormat mDateFormatter;

    private BluetoothAdapter mBluetoothAdapter;

    private ExpandableListAdapter listAdapter;
    private List<String> servicesList;
    private HashMap<String, List<String>> characteristicsList;

    private Toolbar toolBar;

    // used for tabs and viewPager
    private NonSwipeableViewPager viewPager;
    private ConnectedDevicePagerAdapter pagerAdapter;
    private TabLayout tabLayout;

    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private BluetoothLowEnergyService mBluetoothLEService;

    // used to check if services had been already received
    private boolean isDeviceConnected;
    private boolean isServiceBound;
    private boolean hasReceivedServices;
    private boolean isReceivingData;
    private boolean isDirectlyConnected;

    private String deviceAddress;
    private ServicesFragment servicesFragment;

    private LocationManager mLocationManager;
    private GoogleApiClient googleApiClient;

    // replace graph icons with material ones
    private int[] imageResId = {
            R.drawable.services, R.drawable.temperature, R.drawable.gas
    };

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save some data
        savedInstanceState.putBoolean(DEVICE_STATE, isDeviceConnected);
        savedInstanceState.putBoolean(RECEIVED_SERVICES, hasReceivedServices);
        savedInstanceState.putBoolean(RECEIVING_DATA, isReceivingData);
        savedInstanceState.putBoolean(BOUND_STATE, isServiceBound);
        savedInstanceState.putBoolean(CONNECTION_TYPE, isDirectlyConnected);

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }


    // TODO: read about ServiceConnection
    // initializing BLE service and attempting to connect to the device
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.d(TAG, "ON SERVICE CONNECTED");
            isServiceBound = true;

            // receiving a singleton entity of the BluetoothLowEnergyService from via IBinder
            mBluetoothLEService = ((BluetoothLowEnergyService.LocalBinder) service).getService();
            if (!mBluetoothLEService.initialize()) {
                Toast.makeText(LeConnectedDeviceActivity.this,
                        "Unable to initialize Bluetooth", Toast.LENGTH_SHORT).show();
                finish();
            }

            if(!isDeviceConnected) {
                Log.d(TAG, "WITHOUT AUTOCONNECT");
                isDirectlyConnected = true;
                mBluetoothLEService.connect(deviceAddress, false);
            } else {

                // direct connection is closed and new autoConnection is opened
                if(isDirectlyConnected || !hasReceivedServices || !isReceivingData) {
                    Log.d(TAG, "AUTOCONNECT");
                    isDirectlyConnected = false;
                    hasReceivedServices = false;
                    isReceivingData = false;
                    mBluetoothLEService.connect(deviceAddress, true);
                }
            }
        }

        // Called when a connection to the Service has been lost
        // The binding remains active
        // Usually happens in extreme situations like crash or kill of the hosting process
        @Override
        public void onServiceDisconnected(ComponentName componentName) {

            Log.d(TAG, "ON SERVICE DISCONNECTED");
            unregisterReceiver(mGattUpdateReceiver);
            unbindService(mServiceConnection);
            isServiceBound = false;

            mBluetoothLEService = null;
            isDeviceConnected = false;
            isDirectlyConnected = false;
            isReceivingData = false;
            hasReceivedServices = false;

            if (!mBluetoothAdapter.enable()) {
                // Enabling BLE, if it is disabled
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            }
        }
    };

    
    // Events broadcasted by the service
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device. When the data
    // is read or notification is triggered
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLowEnergyService.ACTION_GATT_CONNECTED.equals(action)) {
                isDeviceConnected = true;
                invalidateOptionsMenu();

                // Setting up the servicesFragment' connection state
                setConnectionState(0);

            } else if (BluetoothLowEnergyService.ACTION_GATT_DISCONNECTED.equals(action)) {
                //isReceivingData = false;
                invalidateOptionsMenu();

                // Setting up the servicesFragment' connection state
                setConnectionState(1);

            } else if (BluetoothLowEnergyService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                hasReceivedServices = true;
                displayGattServices(mBluetoothLEService.getSupportedGattServices());
                // Setting up the servicesFragment' connection state
                setConnectionState(2);

            } else if (BluetoothLowEnergyService.ACTION_DATA_AVAILABLE.equals(action)) {

                if(!isReceivingData) {
                    isReceivingData = true;
                    invalidateOptionsMenu();
                }

                //displaySensorsData(intent.getStringExtra(BluetoothLowEnergyService.EXTRA_DATA));

                // Setting up the servicesFragment' connection state
                setConnectionState(3);

            }
        }
    };


    private static IntentFilter GattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLowEnergyService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLowEnergyService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLowEnergyService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLowEnergyService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_le_connected_device);

        if (savedInstanceState != null) {
            // Restore value of members from saved state
            isDeviceConnected = savedInstanceState.getBoolean(DEVICE_STATE);
            hasReceivedServices = savedInstanceState.getBoolean(RECEIVED_SERVICES);
            isReceivingData = savedInstanceState.getBoolean(RECEIVING_DATA);
            isServiceBound = savedInstanceState.getBoolean(BOUND_STATE);
            isDirectlyConnected = savedInstanceState.getBoolean(CONNECTION_TYPE);
        } else {
            isReceivingData = false;
            isDirectlyConnected = false;
            isDeviceConnected = false;
            hasReceivedServices = false;
            isServiceBound = false;
        }

        toolBar = (Toolbar) findViewById(R.id.tool_bar);
        setSupportActionBar(toolBar);

        // Setting up the tabs and viewPager
        viewPager = (NonSwipeableViewPager) findViewById(R.id.viewpager);
        // 2 fragments on the left and on the right from the currently selected one
        // will keep their state
        viewPager.setOffscreenPageLimit(2);

        // Create an adapter that knows which fragment should be shown on each page
        pagerAdapter = new ConnectedDevicePagerAdapter(LeConnectedDeviceActivity.this, getSupportFragmentManager());

        // Set the adapter onto the view pager
        viewPager.setAdapter(pagerAdapter);

        // Give the TabLayout the ViewPager
        tabLayout = (TabLayout) findViewById(R.id.sliding_tabs);
        tabLayout.setupWithViewPager(viewPager);

        try {
            for (int i = 0; i < imageResId.length; i++) {
                tabLayout.getTabAt(i).setIcon(imageResId[i]);
                //tabLayout.getTabAt(i).getIcon().setColorFilter(getColor(R.color.colorPrimary), PorterDuff.Mode.MULTIPLY);
            }
        } catch(NullPointerException ex) {
            ex.printStackTrace();
        }
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        if(bluetoothManager != null)
            mBluetoothAdapter = bluetoothManager.getAdapter();
        else
            finish();

        // Get the Intent that started this activity and extract the string
        Intent intent = getIntent();
        deviceAddress = intent.getStringExtra("deviceAddress");

        if(deviceAddress == null) {
            finish();
        }

        // Initializing location manager
        mLocationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
        if(!isLocationEnabled()){
            // Enabling location services, if they are disabled
            enableLocation();
        }

    }

    // might be used to receive messages from fragments
    @Override
    public void onFragmentInteraction(String tag, Object data) {

    }


    @Override
    protected void onResume() {
        super.onResume();

        if (!mBluetoothAdapter.enable()) {
            // Enabling BLE, if it is disabled
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

        if(!isLocationEnabled()){
            // Enabling location services, if they are disabled
            enableLocation();
        }

        // Starting service if it is not running yet and binding to it afterwards
        Intent gattServiceIntent = new Intent(getApplicationContext(), BluetoothLowEnergyService.class);
        // the service will be created only once, because calls to startService are not nested
        getApplicationContext().startService(gattServiceIntent);
        // service clients are able to bind to it at any time
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        Log.d(TAG, "BINDING ON RESUME");
        registerReceiver(mGattUpdateReceiver, GattUpdateIntentFilter());

    }


    @Override
    protected void onPause() {
        super.onPause();
    }


    @Override
    protected void onStop() {
        super.onStop();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (isFinishing()) {
            unregisterReceiver(mGattUpdateReceiver);

            Log.d(TAG, "ACTIVITY ONDESTROY");

            if(isServiceBound)
                unbindService(mServiceConnection);

            if(mBluetoothLEService != null) {
                mBluetoothLEService.disconnect();
                mBluetoothLEService.close();
            }

            Intent stoppingServiceIntent = new Intent(getApplicationContext(), BluetoothLowEnergyService.class);
            getApplicationContext().stopService(stoppingServiceIntent);
            mBluetoothLEService = null;
        } else {

            // service keeps running, but the activity unbinds on configuration changes
            if(isServiceBound) {
                Log.d(TAG, "UNBINDING ON DESTROY");
                unregisterReceiver(mGattUpdateReceiver);
                unbindService(mServiceConnection);
                isServiceBound = false;
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_CANCELED) {
            Toast.makeText(LeConnectedDeviceActivity.this, "The app " +
                            "won't function without enabled Bluetooth",
                    Toast.LENGTH_LONG).show();
        } else if (requestCode == REQUEST_ENABLE_LS) {

            switch (resultCode) {
                case LeScanActivity.RESULT_OK:
                    // All required changes were successfully made

                    break;
                case LeScanActivity.RESULT_CANCELED:
                    Toast.makeText(LeConnectedDeviceActivity.this, "The app " +
                                    "won't function without enabled location services",
                            Toast.LENGTH_LONG).show();
                    break;
                default:
                    break;
            }
        }
    }


    // TODO: put location related alert in a separate file with application context and call it from both activities if needed
    private boolean isAccessFineLocationAllowed() {
        if (ContextCompat.checkSelfPermission(LeConnectedDeviceActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)
            return false;
        else
            return true;

    }


    public boolean isLocationEnabled() {
        if(isAccessFineLocationAllowed()) {

            if(mLocationManager == null) {
                mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            }

            try {
                boolean isGpsEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                boolean isNetworkEnabled = mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

                if(!isGpsEnabled && !isNetworkEnabled)
                    return false;
                else
                    return true;

            } catch(NullPointerException exc) {
                exc.printStackTrace();
            }

            return false;

        } else
            return false;
    }


    public void enableLocation() {

        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(LeConnectedDeviceActivity.this)
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
                            // requesting to enable location services
                            status.startResolutionForResult(LeConnectedDeviceActivity.this, REQUEST_ENABLE_LS);

                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                        }
                        break;
                }
            }
        });
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu
        getMenuInflater().inflate(R.menu.connected_device_menu, menu);
        return true;
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        menu.findItem(R.id.menu_device_setting).setVisible(true);

        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_device_setting: {
                View menuItemView = findViewById(R.id.menu_device_setting);
                showPopup(menuItemView);
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    public void showPopup(View view) {
        PopupMenu popup = new PopupMenu(this, view);

        popup.getMenuInflater()
                .inflate(R.menu.actions, popup.getMenu());

            popup.getMenu().findItem(R.id.showServices).setTitle(R.string.show_services);


        // registering popup with OnMenuItemClickListener
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.showServices:
                        //swapLayoutViews();
                        break;
                    case R.id.showSami:
                        // TODO: send data to SaMi cloud
                        break;
                }
                return true;
            }
        });

        popup.show();
    }


    private void setConnectionState(int connectionState) {
        String tag = pagerAdapter.getServicesFragmentTag();
        servicesFragment = (ServicesFragment) getSupportFragmentManager().findFragmentByTag(tag);

        if(servicesFragment != null)
            if(servicesFragment.isResumed())
                servicesFragment.setConnectionState(connectionState, hasReceivedServices);
    }


    /*private void displaySensorsData(String data) {
        if (data != null) {
            Toast.makeText(LeConnectedDeviceActivity.this, "Broadcasted data: " + data,
                    Toast.LENGTH_SHORT).show();
        }
    }*/


    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null)
            return;
        String characteristicUuid;
        String serviceUuid;

        for (BluetoothGattService currentGattService : gattServices) {

            // searching through characteristics of the service with sensors' data
            if (currentGattService != null) {
                serviceUuid = currentGattService.getUuid().toString();
                List<BluetoothGattCharacteristic> gattCharacteristics = currentGattService.getCharacteristics();

                for (BluetoothGattCharacteristic currentGattCharacteristic : gattCharacteristics) {

                    // reading characteristics
                    if (currentGattCharacteristic != null) {
                        characteristicUuid = currentGattCharacteristic.getUuid().toString();


                        //TODO: sometimes the app does not reach this point. Probably, not all the services are received!
                        // enabling notification for the characteristics with the sensors' data
                        if(serviceUuid.equals(GattAttributesSample.UUID_SENSORS_SERVICE)
                                && characteristicUuid.equals(GattAttributesSample.UUID_SENSORS_CHARACTERISTIC)) {
                            Log.d(TAG, "Service: " + serviceUuid + ", \nCharacteristic: " + characteristicUuid);

                            mNotifyCharacteristic = currentGattCharacteristic;
                            // setting notification for the current characteristic
                            // to broadcast changes automatically
                            final int characteristicProperties = mNotifyCharacteristic.getProperties();
                            if ((characteristicProperties | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                                mBluetoothLEService.readCharacteristic(mNotifyCharacteristic);
                            }
                            if ((characteristicProperties | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                                mBluetoothLEService.enableCharacteristicNotification(mNotifyCharacteristic, true);
                            }
                        }
                    }
                }
            }
        }
    }


}
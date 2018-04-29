package com.savonia.thesis;

import android.Manifest;
import android.arch.lifecycle.ViewModelProviders;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
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
import com.savonia.thesis.viewmodels.SharedViewModel;

public class LeConnectedDeviceActivity extends AppCompatActivity implements OnFragmentInteractionListener<Object> {

    private final static String TAG = LeConnectedDeviceActivity.class.getSimpleName();

    private final static int REQUEST_ENABLE_BT = 1;
    private final static int REQUEST_ENABLE_LS = 2;
    private final static String DEVICE_STATE = "DeviceConnection";
    private final static String BOUND_STATE = "BindingState";
    private final static String RECEIVED_SERVICES = "ServicesReceived";
    private final static String RECEIVING_DATA = "ReceivingData";
    private final static String CONNECTION_TYPE = "ConnectionType";
    private final static String SCROLL_TO_END = "ScrollToEnd";

    private BluetoothAdapter mBluetoothAdapter;

    private Toolbar toolBar;

    // used for tabs and viewPager
    private NonSwipeableViewPager viewPager;
    private ConnectedDevicePagerAdapter pagerAdapter;
    private TabLayout tabLayout;

    private SharedViewModel sharedViewModel;

    private BluetoothLowEnergyService mBluetoothLEService;

    // used to check if services had been already received
    private boolean isScrollToEndChecked = false;
    private boolean isDeviceConnected = false;
    private boolean isServiceBound = false;
    private boolean hasReceivedServices = false;
    private boolean isReceivingData = false;
    private boolean shouldAutoConnect = false;

    private String deviceAddress;
    private PopupMenu popup;

    private LocationManager mLocationManager;
    private GoogleApiClient googleApiClient;

    // replace graph icons with material ones
    private int[] imageResId = {
            R.drawable.services, R.drawable.temperature, R.drawable.gas
    };

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save some data
        savedInstanceState.putBoolean(SCROLL_TO_END, isScrollToEndChecked);
        savedInstanceState.putBoolean(DEVICE_STATE, isDeviceConnected);
        savedInstanceState.putBoolean(RECEIVED_SERVICES, hasReceivedServices);
        savedInstanceState.putBoolean(RECEIVING_DATA, isReceivingData);
        savedInstanceState.putBoolean(BOUND_STATE, isServiceBound);
        savedInstanceState.putBoolean(CONNECTION_TYPE, shouldAutoConnect);

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }


    // initializing BLE service and attempting to connect to the device
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.d(TAG, "ON SERVICE CONNECTED");
            isServiceBound = true;
            registerReceiver(mGattUpdateReceiver, GattUpdateIntentFilter());

            // receiving a singleton entity of the BluetoothLowEnergyService from via IBinder
            mBluetoothLEService = ((BluetoothLowEnergyService.LocalBinder) service).getService();
            if (!mBluetoothLEService.initialize()) {
                Toast.makeText(LeConnectedDeviceActivity.this,
                        "Unable to initialize Bluetooth", Toast.LENGTH_SHORT).show();
                finish();
            }

            if(!isDeviceConnected) {
                // Connecting directly for the first time and then reconnecting with autoConnect
                // solves unstable connection sometimes
                if(shouldAutoConnect)
                    Log.d(TAG, "WITH AUTOCONNECT");
                else
                    Log.d(TAG, "WITHOUT AUTOCONNECT");

                mBluetoothLEService.connect(deviceAddress, shouldAutoConnect);
                shouldAutoConnect = true;
            }
        }

        // Called when a connection to the Service has been lost
        // The binding remains active
        // Usually happens in extreme situations like crash or kill of the hosting process
        @Override
        public void onServiceDisconnected(ComponentName componentName) {

            Log.d(TAG, "ON SERVICE DISCONNECTED");

            try {
                unregisterReceiver(mGattUpdateReceiver);
            }catch(IllegalArgumentException ex) {
                ex.printStackTrace();
            }
            unbindService(mServiceConnection);
            isServiceBound = false;

            mBluetoothLEService = null;
            shouldAutoConnect = false;
            isDeviceConnected = false;
            isReceivingData = false;
            hasReceivedServices = false;
        }
    };


    private final BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            try {
                if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                    final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                            BluetoothAdapter.ERROR);
                    switch (state) {
                        case BluetoothAdapter.STATE_OFF: {
                            Log.i(TAG, "Bluetooth off");
                        }
                            break;
                        case BluetoothAdapter.STATE_TURNING_OFF:
                            Log.i(TAG, "Turning Bluetooth off");
                            clearConnectionToDevice();
                            break;
                        case BluetoothAdapter.STATE_ON:
                            Log.i(TAG, "Bluetooth on");
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    establishConnectionToDevice();
                                }
                            }, 7000);
                            break;
                        case BluetoothAdapter.STATE_TURNING_ON:
                            Log.i(TAG, "Turning Bluetooth on");
                            break;
                    }
                }
            } catch (NullPointerException ex) {
                ex.printStackTrace();
            }
        }
    };

    private static IntentFilter bleStateFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);

    
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
                sharedViewModel.setConnectionState(0);
                sharedViewModel.setHasReceivedServices(hasReceivedServices);

            } else if (BluetoothLowEnergyService.ACTION_GATT_DISCONNECTED.equals(action)) {
                isDeviceConnected = false;
                shouldAutoConnect = false;
                invalidateOptionsMenu();

                // Setting up the servicesFragment' connection state
                sharedViewModel.setConnectionState(1);
                sharedViewModel.setHasReceivedServices(hasReceivedServices);

            } else if (BluetoothLowEnergyService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                hasReceivedServices = true;
                // Setting up the servicesFragment' connection state
                sharedViewModel.setConnectionState(2);
                sharedViewModel.setHasReceivedServices(hasReceivedServices);

            } else if (BluetoothLowEnergyService.ACTION_DATA_AVAILABLE.equals(action)) {

                if(!isReceivingData) {
                    isReceivingData = true;
                    invalidateOptionsMenu();
                }

                // Setting up the servicesFragment' connection state
                sharedViewModel.setConnectionState(3);
                sharedViewModel.setHasReceivedServices(hasReceivedServices);

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
            isScrollToEndChecked = savedInstanceState.getBoolean(SCROLL_TO_END);
            isDeviceConnected = savedInstanceState.getBoolean(DEVICE_STATE);
            hasReceivedServices = savedInstanceState.getBoolean(RECEIVED_SERVICES);
            isReceivingData = savedInstanceState.getBoolean(RECEIVING_DATA);
            isServiceBound = savedInstanceState.getBoolean(BOUND_STATE);
            shouldAutoConnect = savedInstanceState.getBoolean(CONNECTION_TYPE);
        } else {
            isScrollToEndChecked = false;
            shouldAutoConnect = false;
            isReceivingData = false;
            isDeviceConnected = false;
            hasReceivedServices = false;
            isServiceBound = false;
        }

        sharedViewModel = ViewModelProviders.of(LeConnectedDeviceActivity.this).get(SharedViewModel.class);

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
        registerReceiver(bluetoothStateReceiver, bleStateFilter);
        // after switching bluetooth adapter off and turning it on again
        // the app should wait for 25 seconds
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    establishConnectionToDevice();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }, 70);

    }


    /* It is also important to use onStop() to release resources that might leak memory,
        because it is possible for the system to kill the process hosting your activity without
        calling the activity's final onDestroy() callback*/
    @Override
    protected void onStop() {
        super.onStop();

        try {
            // to avoid error in 7.1.1
            if(popup != null) {
                popup.dismiss();
                popup.getMenu().close();
                popup = null;
            }

            unregisterReceiver(bluetoothStateReceiver);
        }catch(Exception ex) {
            ex.printStackTrace();
        }

        if (!isChangingConfigurations()) {
            Log.d(TAG, "ACTIVITY ONSTOP");
            clearConnectionToDevice();
        } else {
            // service keeps running, but the activity unbinds on configuration changes
            if(isServiceBound) {
                Log.d(TAG, "UNBINDING ON ONSTOP");
                try {
                    unregisterReceiver(mGattUpdateReceiver);
                }catch(IllegalArgumentException ex) {
                    ex.printStackTrace();
                }
                unbindService(mServiceConnection);
                isServiceBound = false;
            }
        }

    }


    @Override
    protected void onPause() {
        super.onPause();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    private void clearConnectionToDevice() {
        if(isServiceBound) {
            try {
                unregisterReceiver(mGattUpdateReceiver);
            }catch(IllegalArgumentException ex) {
                ex.printStackTrace();
            }
            unbindService(mServiceConnection);
        }
        isServiceBound = false;
        isReceivingData = false;
        isDeviceConnected = false;
        hasReceivedServices = false;


        if(mBluetoothLEService != null) {
            Log.d(TAG, "disconnecting, then closing gatt and service");
            mBluetoothLEService.removePendingCallbacks();
            mBluetoothLEService.disconnect();
            mBluetoothLEService.close();

            sharedViewModel.setConnectionState(1);
            sharedViewModel.setHasReceivedServices(hasReceivedServices);

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent stoppingServiceIntent = new Intent(getApplicationContext(), BluetoothLowEnergyService.class);
                    getApplicationContext().stopService(stoppingServiceIntent);
                    mBluetoothLEService = null;
                }
            }, 50);

        }
    }

    private void establishConnectionToDevice() {

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
        Log.d(TAG, "BINDING in activity");
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
        popup = new PopupMenu(this, view);

        popup.getMenuInflater().inflate(R.menu.actions, popup.getMenu());

        popup.getMenu().findItem(R.id.setScrollToEnd).setChecked(isScrollToEndChecked);

        // registering popup with OnMenuItemClickListener
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.setScrollToEnd: // sets scrolling to the last added value in fragments' graphs
                        if(item.isChecked()) {
                            item.setChecked(false);
                            isScrollToEndChecked = false;
                        } else {
                            item.setChecked(true);
                            isScrollToEndChecked = true;
                        }
                        sharedViewModel.setScrollToEnd(isScrollToEndChecked);

                        // Keeps the popup menu open
                        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
                        item.setActionView(new View(LeConnectedDeviceActivity.this));
                        item.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
                            @Override
                            public boolean onMenuItemActionExpand(MenuItem item) {
                                return false;
                            }

                            @Override
                            public boolean onMenuItemActionCollapse(MenuItem item) {
                                return false;
                            }
                        });
                        return false;
                    case R.id.sendToSami:
                        try {
                            Intent connectToDevice = new Intent();
                            connectToDevice.setClass(LeConnectedDeviceActivity.this, PostRequestActivity.class);
                            startActivity(connectToDevice);

                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                        return true;
                    case R.id.getFromSami:
                        try {

                            Intent connectToDevice = new Intent();
                            connectToDevice.setClass(LeConnectedDeviceActivity.this, GetRequestActivity.class);
                            startActivity(connectToDevice);

                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }

                        return true;
                    default: return true;
                }
            }
        });

        popup.show();
    }

}
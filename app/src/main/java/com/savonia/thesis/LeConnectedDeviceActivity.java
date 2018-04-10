package com.savonia.thesis;

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
import android.content.ServiceConnection;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentManager;
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
    private final static String SERVICE_STATE = "ServiceRunning";
    private final static String RECEIVED_SERVICES = "ServicesReceived";

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
    private boolean isServiceRunning = false;
    private boolean hasReceivedServices = false;
    private boolean isReceivingData = false;

    private String deviceAddress;
    private ServicesFragment servicesFragment;

    // replace graph icons with material ones
    private int[] imageResId = {
            R.drawable.services, R.drawable.temperature, R.drawable.gas
    };

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save some data
        savedInstanceState.putBoolean(SERVICE_STATE, isServiceRunning);
        savedInstanceState.putBoolean(RECEIVED_SERVICES, hasReceivedServices);

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }


    // initializing BLE service and attempting to connect to the device
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            // receiving a singleton entity of the BluetoothLowEnergyService from via IBinder
            mBluetoothLEService = ((BluetoothLowEnergyService.LocalBinder) service).getService();
            if (!mBluetoothLEService.initialize()) {
                Toast.makeText(LeConnectedDeviceActivity.this,
                        "Unable to initialize Bluetooth", Toast.LENGTH_SHORT).show();
                finish();
            }

            if(!isServiceRunning)
                mBluetoothLEService.connect(deviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLEService = null;
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
                isServiceRunning = true;
                invalidateOptionsMenu();

                // Setting up the servicesFragment' connection state
                setConnectionState(0);

            } else if (BluetoothLowEnergyService.ACTION_GATT_DISCONNECTED.equals(action)) {
                isReceivingData = false;
                invalidateOptionsMenu();

                // Setting up the servicesFragment' connection state
                setConnectionState(1);

            } else if (BluetoothLowEnergyService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                    displayGattServices(mBluetoothLEService.getSupportedGattServices());

                // Setting up the servicesFragment' connection state
                setConnectionState(2);

            } else if (BluetoothLowEnergyService.ACTION_DATA_AVAILABLE.equals(action)) {

                if(!isReceivingData) {
                    isReceivingData = true;
                    invalidateOptionsMenu();
                }

                displaySensorsData(intent.getStringExtra(BluetoothLowEnergyService.EXTRA_DATA));

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
            isServiceRunning = savedInstanceState.getBoolean(SERVICE_STATE);
            hasReceivedServices = savedInstanceState.getBoolean(RECEIVED_SERVICES);
        } else {
            isServiceRunning = false;
            hasReceivedServices = false;
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

        for (int i = 0; i < imageResId.length; i++) {
            tabLayout.getTabAt(i).setIcon(imageResId[i]);
            //tabLayout.getTabAt(i).getIcon().setColorFilter(getColor(R.color.colorPrimary), PorterDuff.Mode.MULTIPLY);
        }

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

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

        // Starting service if it is not running yet and binding to it afterwards
        Intent gattServiceIntent = new Intent(getApplicationContext(), BluetoothLowEnergyService.class);
        // the service will be created only once
        getApplicationContext().startService(gattServiceIntent);
        // service clients are able to bind to it at any time
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        registerReceiver(mGattUpdateReceiver, GattUpdateIntentFilter());

    }

    // might be used to receive messages from fragments
    @Override
    public void onFragmentInteraction(String tag, Object data) {

    }


    @Override
    protected void onResume() {
        super.onResume();

        if (!mBluetoothAdapter.enable()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
    }


    @Override
    protected void onStop() {
        super.onStop();
        /*if (!isChangingConfigurations())*/
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing()) {
            unregisterReceiver(mGattUpdateReceiver);
            mBluetoothLEService.close();
            unbindService(mServiceConnection);
            Intent stoppingServiceIntent = new Intent(getApplicationContext(), BluetoothLowEnergyService.class);
            getApplicationContext().stopService(stoppingServiceIntent);
            mBluetoothLEService = null;
        } else {
            // service keeps running, but the activity unbinds on configuration changes
            unregisterReceiver(mGattUpdateReceiver);
            unbindService(mServiceConnection);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_CANCELED) {
            finish();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu
        getMenuInflater().inflate(R.menu.connected_device_menu, menu);
        return true;
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if(isReceivingData)
            menu.findItem(R.id.menu_device_setting).setVisible(true);
        else
            menu.findItem(R.id.menu_device_setting).setVisible(false);

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


    private void displaySensorsData(String data) {
        if (data != null) {
            Toast.makeText(LeConnectedDeviceActivity.this, "Broadcasted data: " + data,
                    Toast.LENGTH_SHORT).show();
        }
    }


    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null)
            return;
        String characteristicUuid = null;
        String serviceUuid = "unknown service";
        /*String serviceName = "unknown service";
        String characteristicNameString = "unknown characteristic";
        servicesList = new ArrayList<String>();
        characteristicsList = new HashMap<String, List<String>>();*/

        //int serviceNumber = 0;

        // TODO: get proper names for services and characteristics

        for (BluetoothGattService currentGattService : gattServices) {

            // searching through characteristics of the service with sensors' data
            if (currentGattService != null) {
                serviceUuid = currentGattService.getUuid().toString();
                /*serviceName = GattAttributesSample.getName(serviceUuid);

                if(serviceName != null)
                    servicesList.add(serviceName);// + ", " + serviceUuid);
                else
                    servicesList.add(serviceUuid);*/

                List<BluetoothGattCharacteristic> gattCharacteristics =
                        currentGattService.getCharacteristics();
                //List<String> characteristicsNamesList = new ArrayList<String>();

                for (BluetoothGattCharacteristic currentGattCharacteristic : gattCharacteristics) {

                    // reading characteristics
                    if (currentGattCharacteristic != null) {

                        characteristicUuid = currentGattCharacteristic.getUuid().toString();
                        /*characteristicNameString = GattAttributesSample.getName(characteristicUuid);

                        if(characteristicNameString != null)
                            characteristicsNamesList.add(characteristicNameString);
                        else
                            characteristicsNamesList.add(characteristicUuid);*/

                        // enabling notification for the characteristics with the sensors' data
                        if(serviceUuid.equals(GattAttributesSample.UUID_SENSORS_SERVICE)
                                && characteristicUuid.equals(GattAttributesSample.UUID_SENSORS_CHARACTERISTIC)) {

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

                /*characteristicsList.put(servicesList.get(serviceNumber), characteristicsNamesList);
                serviceNumber++;*/

            }
        }

        hasReceivedServices = true;
        /*listAdapter = new ExpandableAttributesAdapter(this, servicesList, characteristicsList);

        // setting list adapter
        expListView.setAdapter(listAdapter);*/
    }

}
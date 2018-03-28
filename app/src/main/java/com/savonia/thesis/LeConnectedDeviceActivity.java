package com.savonia.thesis;

import android.animation.LayoutTransition;
import android.app.ActivityManager;
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
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
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

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.savonia.thesis.db.SensorsValuesDatabase;
import com.savonia.thesis.db.entity.Temperature;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class LeConnectedDeviceActivity extends AppCompatActivity {

    private final static int REQUEST_ENABLE_BT = 1;
    private final static String CONNECTION_STATE = "ConnectionEstablished";
    private final static String RECEIVED_SERVICES = "ServicesReceived";

    private BluetoothAdapter mBluetoothAdapter;

    private ExpandableListAdapter listAdapter;
    private List<String> servicesList;
    private HashMap<String, List<String>> characteristicsList;

    //TODO: change layout
    private TextView deviceStatus;
    private ExpandableListView expListView;
    private GraphView sensorsGraph;
    private Toolbar toolBar;
    private ProgressBar spinner;
    private TextView lookUpText;

    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private boolean isReceivingData = false;

    private BluetoothLowEnergyService mBluetoothLEService;

    // used to check if services had been already received
    private boolean isServiceRunning = false;
    private boolean hasReceivedServices = false;
    private String deviceAddress;

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save some data
        savedInstanceState.putBoolean(CONNECTION_STATE, isServiceRunning);
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
                updateConnectionState("Connected");
                isServiceRunning = true;
                spinner.setVisibility(View.VISIBLE);
                lookUpText.setVisibility(View.VISIBLE);
                invalidateOptionsMenu();
            } else if (BluetoothLowEnergyService.ACTION_GATT_DISCONNECTED.equals(action)) {
                updateConnectionState("Disconnected");
                isReceivingData = false;
                spinner.setVisibility(View.VISIBLE);
                lookUpText.setVisibility(View.VISIBLE);
                invalidateOptionsMenu();

            } else if (BluetoothLowEnergyService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                    // TODO: in case services have already been discovered, simply retrieve them from the ROOM and enableNotifyCharacteristic on the sensor's data characteristic
                    displayGattServices(mBluetoothLEService.getSupportedGattServices());
            } else if (BluetoothLowEnergyService.ACTION_DATA_AVAILABLE.equals(action)) {

                if(!isReceivingData) {
                    isReceivingData = true;
                    invalidateOptionsMenu();
                }

                displaySensorsData(intent.getStringExtra(BluetoothLowEnergyService.EXTRA_DATA));
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
            isServiceRunning = savedInstanceState.getBoolean(CONNECTION_STATE);
            hasReceivedServices = savedInstanceState.getBoolean(RECEIVED_SERVICES);
        } else {
            isServiceRunning = false;
            hasReceivedServices = false;
        }

        deviceStatus = (TextView) findViewById(R.id.deviceStatus);

        // spinner, its neighbouring textView, graph and connectivity to the SaMi cloud
        // won't be shown if another device is connected
        spinner = (ProgressBar) findViewById(R.id.spinner);
        lookUpText = (TextView) findViewById(R.id.lookUpTextView);
        spinner.setVisibility(View.GONE);
        lookUpText.setVisibility(View.GONE);

        toolBar = (Toolbar) findViewById(R.id.tool_bar);
        setSupportActionBar(toolBar);

        //setting up the graph's vertical labels
        sensorsGraph = (GraphView) findViewById(R.id.graph);
        sensorsGraph.setTitle("Current sensor\'s data");
        sensorsGraph.setTitleColor(R.color.colorPrimaryDark);
        sensorsGraph.getViewport().setScalableY(true);
        sensorsGraph.getGridLabelRenderer().setVerticalAxisTitle("Value");
        sensorsGraph.getGridLabelRenderer().setHorizontalAxisTitle("Time");
        sensorsGraph.getGridLabelRenderer().setLabelVerticalWidth(40);

        expListView = (ExpandableListView) findViewById(R.id.expandableListView);
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

        // setting up the progress bar, in case the BLE Shield is connected
        if(deviceAddress.equals(GattAttributesSample.DEVICE_ADDRESS)) {
            Drawable progressDrawable = spinner.getIndeterminateDrawable().mutate();
            progressDrawable.setColorFilter(getColor(R.color.colorPrimary), PorterDuff.Mode.MULTIPLY);
            spinner.setProgressDrawable(progressDrawable);
            spinner.setVisibility(View.VISIBLE);
            lookUpText.setVisibility(View.VISIBLE);
        }

        // Starting service if it is not running yet and binding to it afterwards
        Intent gattServiceIntent = new Intent(getApplicationContext(), BluetoothLowEnergyService.class);
        // the service will be created only once
        getApplicationContext().startService(gattServiceIntent);
        // service clients are able to bind to it at any time
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        registerReceiver(mGattUpdateReceiver, GattUpdateIntentFilter());

       /* if (mBluetoothLEService != null && !isServiceRunning) {
            final boolean result = mBluetoothLEService.connect(deviceAddress);
            Toast.makeText(LeConnectedDeviceActivity.this,
                    "Connect request result: " + result, Toast.LENGTH_SHORT).show();
        }*/


        expListView.setVisibility(View.GONE);
        sensorsGraph.setVisibility(View.GONE);


        final SensorsDataViewModel sensorsDataViewModel =
                ViewModelProviders.of(this).get(SensorsDataViewModel.class);

        sensorsDataViewModel.getTemperatures().observe(this, new Observer<List<Temperature>>() {
            @Override
            public void onChanged(@Nullable final List<Temperature> temperatures) {
                //TODO: Update the cached copy of the temperatures on the graph

                if(temperatures == null){
                    Toast.makeText(LeConnectedDeviceActivity.this,
                            "No data received", Toast.LENGTH_SHORT).show();
                }

            }
        });

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
        /*if (!isChangingConfigurations()) {
            unregisterReceiver(mGattUpdateReceiver);
            unbindService(mServiceConnection);
            mBluetoothLEService = null;
        }*/
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        // the service will be running if
        if(isFinishing()) {
            unregisterReceiver(mGattUpdateReceiver);
            unbindService(mServiceConnection);
            Intent stoppingServiceIntent = new Intent(getApplicationContext(), BluetoothLowEnergyService.class);
            getApplicationContext().stopService(stoppingServiceIntent);
            mBluetoothLEService = null;
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

        if(expListView.getVisibility() == View.VISIBLE)
            popup.getMenu().findItem(R.id.showServices).setTitle(R.string.draw_graph);
        else
            popup.getMenu().findItem(R.id.showServices).setTitle(R.string.show_services);


        // registering popup with OnMenuItemClickListener
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.showServices:
                        swapLayoutViews();
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


    private void swapLayoutViews() {
        if( expListView.getVisibility() == View.VISIBLE ) {
            slideToLeft(expListView);
            slideFromRight(sensorsGraph);
            expListView.setVisibility(View.GONE);
            sensorsGraph.setVisibility(View.VISIBLE);
        }
        else {
            slideToLeft(sensorsGraph);
            slideFromRight(expListView);
            sensorsGraph.setVisibility(View.GONE);
            expListView.setVisibility(View.VISIBLE);
        }
    }


    private void hideLayoutViews() {
        expListView.setVisibility(View.GONE);
        sensorsGraph.setVisibility(View.GONE);
        spinner.setVisibility(View.VISIBLE);
        lookUpText.setVisibility(View.VISIBLE);
        lookUpText.setText(R.string.connection_lost);
    }


    public void slideToLeft(View view){
        TranslateAnimation animate = new TranslateAnimation(0,-view.getWidth()*2,0,0);
        animate.setDuration(500);
        view.startAnimation(animate);
    }


    public void slideFromRight(View view) {
        TranslateAnimation animate = new TranslateAnimation(view.getWidth(), 0,0,0);
        animate.setDuration(500);
        animate.setFillAfter(true);
        view.startAnimation(animate);
    }


    private void updateConnectionState(final String status) {
        deviceStatus.setText(status);
    }


    // TODO: fill the graph with sensors' data via LiveData using Room
    private void displaySensorsData(String data) {
        if (data != null) {

            // If the app is receiving data, then it can be shown on a graph
            spinner.setVisibility(View.GONE);
            lookUpText.setVisibility(View.GONE);

            Toast.makeText(LeConnectedDeviceActivity.this, "Broadcasted data: " + data,
                    Toast.LENGTH_SHORT).show();
        }
    }


    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null)
            return;
        String characteristicUuid = null;
        String serviceUuid = "unknown service";
        String serviceName = "unknown service";
        String characteristicNameString = "unknown characteristic";
        servicesList = new ArrayList<String>();
        characteristicsList = new HashMap<String, List<String>>();

        int serviceNumber = 0;

        // TODO: get proper names for services and characteristics

        for (BluetoothGattService currentGattService : gattServices) {

            // searching through characteristics of the service with sensors' data
            if (currentGattService != null) {
                serviceUuid = currentGattService.getUuid().toString();
                serviceName = GattAttributesSample.getName(serviceUuid);

                if(serviceName != null)
                    servicesList.add(serviceName);// + ", " + serviceUuid);
                else
                    servicesList.add(serviceUuid);

                List<BluetoothGattCharacteristic> gattCharacteristics =
                        currentGattService.getCharacteristics();
                List<String> characteristicsNamesList = new ArrayList<String>();

                for (BluetoothGattCharacteristic currentGattCharacteristic : gattCharacteristics) {

                    // reading characteristics
                    if (currentGattCharacteristic != null) {

                        characteristicUuid = currentGattCharacteristic.getUuid().toString();
                        characteristicNameString = GattAttributesSample.getName(characteristicUuid);

                        if(characteristicNameString != null)
                            characteristicsNamesList.add(characteristicNameString);
                        else
                            characteristicsNamesList.add(characteristicUuid);

                        // enabling notification for the characteristics with the sensors' data
                        if(serviceUuid.equals(GattAttributesSample.UUID_SENSORS_SERVICE)
                                && characteristicUuid.equals(GattAttributesSample.UUID_SENSORS_CHARACTERISTIC)) {

                            lookUpText.setText(R.string.waiting_notified_characteristic);

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

                characteristicsList.put(servicesList.get(serviceNumber), characteristicsNamesList);
                serviceNumber++;

            }
        }

        hasReceivedServices = true;
        listAdapter = new ExpandableAttributesAdapter(this, servicesList, characteristicsList);

        // setting list adapter
        expListView.setAdapter(listAdapter);
        expListView.setVisibility(View.VISIBLE);
    }

}
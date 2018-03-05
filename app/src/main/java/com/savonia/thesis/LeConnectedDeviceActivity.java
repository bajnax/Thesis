package com.savonia.thesis;

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
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class LeConnectedDeviceActivity extends AppCompatActivity {

    private final static int REQUEST_ENABLE_BT = 1;

    private BluetoothAdapter mBluetoothAdapter;

    private Runnable mTimer1;
    private Runnable mTimer2;

    ExpandableListAdapter listAdapter;
    ExpandableListView expListView;
    List<String> servicesList;
    HashMap<String, List<String>> characteristicsList;
    //TODO: change layout
    TextView deviceStatus;

    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private boolean mConnected = false;

    private BluetoothLowEnergyService mBluetoothLEService;

    // initializing BLEservice and attempting to connect to the device
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLEService = ((BluetoothLowEnergyService.LocalBinder) service).getService();
            if (!mBluetoothLEService.initialize()) {
                Toast.makeText(LeConnectedDeviceActivity.this,
                        "Unable to initialize Bluetooth", Toast.LENGTH_SHORT).show();
                finish();
            }
            mBluetoothLEService.connect((GattAttributesSample.DEVICE_ADDRESS));
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
                mConnected = true;
                updateConnectionState("Connected");
                invalidateOptionsMenu();
            } else if (BluetoothLowEnergyService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState("Disconnected");
            } else if (BluetoothLowEnergyService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                displayGattServices(mBluetoothLEService.getSupportedGattServices());
            } else if (BluetoothLowEnergyService.ACTION_DATA_AVAILABLE.equals(action)) {
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

        expListView = (ExpandableListView) findViewById(R.id.expandableListView);
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Get the Intent that started this activity and extract the string
        Intent intent = getIntent();
        String deviceAddress = intent.getStringExtra("deviceAddress");

        Toast.makeText(LeConnectedDeviceActivity.this, "Device address: " + deviceAddress,
                Toast.LENGTH_SHORT).show();

        // TODO: set visibility  for the graphs creation button only for the BLEshield
        if(deviceAddress == null) { // || !deviceAddress.equals(GattAttributesSample.DEVICE_ADDRESS)) {
            finish();
        }
        deviceStatus = (TextView) findViewById(R.id.deviceStatus);

        Intent gattServiceIntent = new Intent(LeConnectedDeviceActivity.this, BluetoothLowEnergyService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

    }


    @Override
    protected void onResume() {
        super.onResume();

        if (!mBluetoothAdapter.enable()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQUEST_ENABLE_BT);
        }
        registerReceiver(mGattUpdateReceiver, GattUpdateIntentFilter());
        if (mBluetoothLEService != null) {
            final boolean result = mBluetoothLEService.connect(GattAttributesSample.DEVICE_ADDRESS);
            Toast.makeText(LeConnectedDeviceActivity.this,
                    "Connect request result: " + result, Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLEService = null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_CANCELED) {
            finish();
        }
    }


    private void updateConnectionState(final String status) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                deviceStatus.setText(status);
            }
        });
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
        String serviceName = "unknown service";
        String characteristicNameString = "unknown characteristic";
        servicesList = new ArrayList<String>();
        characteristicsList = new HashMap<String, List<String>>();

        int serviceNumber = 0;

        for (BluetoothGattService currentGattService : gattServices) {

            // searching through characteristics of the service with sensors' data
            if (currentGattService != null) {
                serviceUuid = currentGattService.getUuid().toString();
                serviceName = GattAttributesSample.getName(serviceUuid);

                if(serviceName != null)
                    servicesList.add(serviceName + ", " + serviceUuid);
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
                            characteristicsNamesList.add(characteristicNameString + ", " + characteristicUuid);
                        else
                            characteristicsNamesList.add(characteristicUuid);

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

                characteristicsList.put(servicesList.get(serviceNumber), characteristicsNamesList);
                serviceNumber++;

            }
        }

        listAdapter = new ExpandableAttributesAdapter(this, servicesList, characteristicsList);

        // setting list adapter
        expListView.setAdapter(listAdapter);

    }

}
package com.savonia.thesis;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.savonia.thesis.db.SensorsValuesDatabase;
import com.savonia.thesis.db.entity.Gas;
import com.savonia.thesis.db.entity.Temperature;
import com.savonia.thesis.repository.CentralRepository;

import java.util.List;
import java.util.Scanner;
import java.util.UUID;

public class BluetoothLowEnergyService extends Service {

    private final static String TAG = BluetoothLowEnergyService.class.getSimpleName();

    private Handler mHandler = new Handler(Looper.getMainLooper());
    private Runnable notReceivingData;
    private Runnable reconnect;
    private Runnable discoverServices;
    private Runnable connectingDevice;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattCharacteristic sensorsDataCharacteristic;
    private Boolean hasReceivedData = false;
    private Boolean hasConnected = false;
    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;


    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";

    // Implementing callback methods for GATT events: discovered services and connection changes
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);

                hasConnected = true;
                Log.i(TAG, "Connected to GATT server. onConnectionStateChange received: " + status);

                if(mBluetoothAdapter != null)
                    mBluetoothAdapter.cancelDiscovery();

                // Attempts to discover services after successful connection.
                // waiting for 1 second after connection to overcome status 129
                mHandler.postDelayed(discoverServices = new Runnable(){
                    @Override
                    public void run() {
                        try {
                            if(mBluetoothGatt != null && mBluetoothAdapter != null) {
                                Log.i(TAG, "Attempting to start service discovery:" +
                                        mBluetoothGatt.discoverServices());
                            }
                        } catch(Exception e) {
                            e.printStackTrace();
                        }
                    }
                }, 1000);

            } else if (newState == STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");


                // using direct connection helps in this case
                if(status == 133) {
                    reConnect(10000, false);
                }

                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                Log.d(TAG, "Services discovered successfully");


                // enabling notification for the characteristic with measurements
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (mBluetoothGatt != null && mBluetoothAdapter != null) {
                                BluetoothGattService sensorsService = mBluetoothGatt.getService(UUID.fromString(GattAttributesSample.UUID_SENSORS_SERVICE));
                                if (sensorsService != null) {
                                    BluetoothGattCharacteristic characteristic =
                                            sensorsService.getCharacteristic(UUID.fromString(GattAttributesSample.UUID_SENSORS_CHARACTERISTIC));
                                    if (characteristic != null) {
                                        enableCharacteristicNotification(characteristic, true);
                                        readCharacteristic(characteristic);
                                    }
                                }

                                // restarts the service from there if notified characteristic does not arrive in 18 seconds
                                // after receiving services
                                mHandler.postDelayed(notReceivingData = new Runnable() {
                                    @Override
                                    public void run() {
                                        try{
                                            if(!hasReceivedData) {
                                                reConnect(3000, false);
                                                Log.d(TAG, "NOTIFIED CHARACTERISTIC DID NOT ARRIVE");
                                            }
                                        } catch(Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }, 18000);

                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                });

            } else {
                Log.d(TAG, "The method 'onServicesDiscovered' received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                Log.d(TAG, "onCharacteristicRead");
            } else {
                Log.d(TAG, "The method 'onCharacteristicRead' received: " + status);
            }
        }

        // invokes broadcast when notified about any changes in characteristic
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            Log.d(TAG, "onCharacteristicChanged");
            mHandler.removeCallbacks(notReceivingData);
            hasReceivedData = true;
        }

    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    //  checks what kind of measurements has been received and saves its value in the database accordingly
    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {

        final Intent intent = new Intent(action);

        final byte[] data = characteristic.getValue();
        String value;
        if (data != null && data.length > 0) {
            value = new String(data);
            Log.d(TAG, "Received value: " + value);
            intent.putExtra(EXTRA_DATA, value);

            // retrieving the double value from the temperature notification
            if(value.charAt(0) == 't' && !value.contains("g")) {    // MODIFY for your sensor's character
                try {
                    StringBuilder sb = new StringBuilder(value);
                    value = sb.substring(1);
                    value = value.trim();
                    Log.d(TAG, "Trimmed value: " + value);

                    double tempValue = Double.parseDouble(value);

                    Temperature temperature = new Temperature(tempValue);
                    CentralRepository.getInstance(SensorsValuesDatabase.getDatabase(getApplicationContext())).insertTemperature(temperature);
                }catch (Exception e){
                    e.printStackTrace();
                }
            } else if(value.charAt(0) == 'g' && !value.contains("t")) {     // MODIFY for your sensor's character
                // retrieving the double value from the gas notification
                try {
                    StringBuilder sb = new StringBuilder(value);
                    value = sb.substring(1);
                    value = value.trim();
                    Log.d(TAG, "Trimmed value: " + value);

                    double gasValue = Double.parseDouble(value);
                    Gas gas = new Gas(gasValue);
                    CentralRepository.getInstance(SensorsValuesDatabase.getDatabase(getApplicationContext())).insertGas(gas);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }

        }

        sendBroadcast(intent);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
       Log.d(TAG, "SERVICE ONSTARTCOMMAND");
        // If we get killed, after returning from here, restart
        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        Log.d(TAG, "SERVICE ONDESTROY");
        mHandler.removeCallbacks(notReceivingData);
        mHandler.removeCallbacks(reconnect);
        mHandler.removeCallbacks(discoverServices);
        mHandler.removeCallbacks(connectingDevice);
    }


    public class LocalBinder extends Binder {
        // Return this instance of BluetoothLowEnergyService so clients can call public methods
        BluetoothLowEnergyService getService() {
            return BluetoothLowEnergyService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "BINDING");
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // when LeConnectedDeviceActivity is destroyed, it unbinds the current service
        Log.d(TAG, "UNBINDING");
        return super.onUnbind(intent);
    }

    // The IBinder is unique for all clients that bind to the service
    private final IBinder mBinder = new LocalBinder();


    // initializing local BluetoothAdapter
    public boolean initialize() {

        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(getApplicationContext().BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.d(TAG, "BluetoothManager initialization failed");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.d(TAG, "BluetoothAdapter initialization failed");
            return false;
        }

        return true;
    }


    // Connecting to the GATT server of the device
    // result is returned asynchronously through BluetoothGattCallback in onConnectionStateChange()
    public boolean connect(final String address, boolean autoConnect) {
        if (mBluetoothAdapter == null || address == null) {
            Log.d(TAG, "Unspecified address or uninitialized BluetoothAdapter");
            return false;
        }

        mBluetoothAdapter.cancelDiscovery();

        // Trying to reconnect to previously connected device
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {

            mHandler.post(new Runnable(){
                @Override
                public void run() {
                    try {
                        Log.d(TAG, "CALLING mBluetoothGatt.connect()");
                        mBluetoothGatt.connect();
                    } catch(Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });


            mHandler.postDelayed(connectingDevice = new Runnable() {
                @Override
                public void run() {
                    try{
                        if(!hasConnected) {
                            reConnect(3000, true);
                            Log.d(TAG, "FAILED TO CONNECT DIRECTLY TO THE DEVICE. RETRYING WItH AUTOCONNECT");
                        }
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                }
            }, 7000);


            return true;
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.d(TAG, "Device not found.  Unable to connect");
            return false;
        }

        mHandler.post(new Runnable(){
            @Override
            public void run() {
                try {
                    // connecting to the Gatt server of device. 'autoConnect' is set to true
                    Log.d(TAG, "Trying to create a new connection to the Gatt server");
                    mBluetoothGatt = device.connectGatt(getApplicationContext(), autoConnect,
                            mGattCallback, BluetoothDevice.TRANSPORT_LE);

                    mBluetoothDeviceAddress = address;
                    mConnectionState = STATE_CONNECTING;
                    if (mBluetoothGatt != null) {
                        mBluetoothGatt.connect();
                    }
                } catch(Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        return true;
    }

    // disconnect or cancel the current connection
    // the result is reported via BluetoothGattCallback in onConnectionStateChange()
    public void disconnect() {
        mHandler.removeCallbacks(discoverServices);
        mHandler.removeCallbacks(connectingDevice);
        hasReceivedData = false;
        hasConnected = false;
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.d(TAG, "BluetoothAdapter is not initialized");
        } else {
            enableCharacteristicNotification(sensorsDataCharacteristic, false);
            mHandler.postDelayed(new Runnable(){
                @Override
                public void run() {
                    try {
                        if(mBluetoothAdapter != null && mBluetoothGatt != null) {
                            Log.d(TAG, "Disconnecting from the device");
                            mBluetoothGatt.disconnect();
                            mConnectionState = STATE_DISCONNECTED;
                        }
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                }
            }, 20);
        }
    }

    // releasing resources
    public void close() {
        hasReceivedData = false;
        if (mBluetoothGatt == null) {
        } else {

            mHandler.postDelayed(new Runnable(){
                @Override
                public void run() {
                    try {
                        if(mBluetoothAdapter != null && mBluetoothGatt != null) {
                            mBluetoothAdapter.cancelDiscovery();
                            mBluetoothGatt.close();
                            mBluetoothGatt = null;
                        }
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                }
            }, 40);
        }

    }

    // requesting to read characteristic
    // the result is asynchronously returned through BluetoothGattCallback in onCharacteristicRead()
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.d(TAG, "BluetoothAdapter is not initialized");
            return;
        }


        mHandler.postDelayed(new Runnable(){
            @Override
            public void run() {
                try {
                    if (mBluetoothGatt != null && mBluetoothAdapter.isEnabled()) {
                        Log.d(TAG, "READING CHARACTERISTIC");
                        mBluetoothGatt.readCharacteristic(characteristic);
                    }
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }, 100);


    }


    // enabling notification for the characteristic with sensors' data
    public void enableCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean shouldEnable) {

        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.d(TAG, "BluetoothAdapter is not initialized");
            return;
        }

        if(shouldEnable) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        if(mBluetoothGatt != null && mBluetoothAdapter.isEnabled()) {
                            Log.d(TAG, "ENABLING CHARACTERISTIC NOTIFICATION");
                            Boolean notificationStatus = mBluetoothGatt.setCharacteristicNotification(characteristic, shouldEnable);

                            if (!notificationStatus) {
                                Log.d(TAG, "Enabling notification failed!");
                                return;
                            }

                            sensorsDataCharacteristic = characteristic;

                            mHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        BluetoothGattDescriptor descriptor =
                                                characteristic.getDescriptor(UUID.fromString(GattAttributesSample.CLIENT_CHARACTERISTIC_CONFIG));
                                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                        if (mBluetoothGatt != null && mBluetoothAdapter.isEnabled()) {
                                            Log.d(TAG, "WRITING NOTIFICATION DESCRIPTOR");
                                            mBluetoothGatt.writeDescriptor(descriptor);
                                        }
                                    } catch (Exception ex) {
                                        ex.printStackTrace();
                                    }
                                }
                            }, 50);

                        }
                    } catch(Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });

        } else if(sensorsDataCharacteristic != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "DISABLING CHARACTERISTIC NOTIFICATION");
                    try {

                        Boolean notificationStatus = mBluetoothGatt.setCharacteristicNotification(characteristic, shouldEnable);

                        if(!notificationStatus) {
                            Log.d(TAG, "disabling notification failed!");
                            return;
                        }
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    BluetoothGattDescriptor descriptor =
                                            characteristic.getDescriptor(UUID.fromString(GattAttributesSample.CLIENT_CHARACTERISTIC_CONFIG));
                                    descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                                    if (mBluetoothGatt != null) {
                                        mBluetoothGatt.writeDescriptor(descriptor);
                                    }

                                    sensorsDataCharacteristic = null;
                                } catch(Exception exc) {
                                        exc.printStackTrace();
                                    }
                            }
                        }, 10);

                    } catch(Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
        }
    }

    public void removePendingCallbacks() {
        Log.d(TAG, "Removing pending callbacks");
        mHandler.removeCallbacks(notReceivingData);
        mHandler.removeCallbacks(reconnect);
        mHandler.removeCallbacks(discoverServices);
        mHandler.removeCallbacks(connectingDevice);
    }

    public void reConnect(int delay, Boolean autoConnect) {
        disconnect();
        close();
        mHandler.postDelayed(reconnect = new Runnable() {
            @Override
            public void run() {
                if(mBluetoothDeviceAddress != null && !mBluetoothDeviceAddress.isEmpty())
                    connect(mBluetoothDeviceAddress, autoConnect);
            }
        }, delay);
    }

}

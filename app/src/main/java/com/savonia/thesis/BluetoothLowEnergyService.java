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
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattCharacteristic sensorsDataCharacteristic;
    private Boolean success;
    private int mConnectionState = STATE_DISCONNECTED;


    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;
    private static final UUID CLIENT_CHARACTERISTIC_CONFIG_UUID = convertFromInteger(0x2902);


    private static UUID convertFromInteger(int i) {
        final long MSB = 0x0000000000001000L;
        final long LSB = 0x800000805f9b34fbL;
        long value = i & 0xFFFFFFFF;
        return new UUID(MSB | (value << 32), LSB);
    }

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

                Log.i(TAG, "Connected to GATT server. onConnectionStateChange received: " + status);

                if(mBluetoothAdapter != null)
                    mBluetoothAdapter.cancelDiscovery();
                // Attempts to discover services after successful connection.
                // waiting for 1 second after connection to overcome status 129
                mHandler.postDelayed(new Runnable(){
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

                // brute forcing to connect in case of infamous status 133
                if(status == 133) {
                    disconnect();
                    close();
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            connect(mBluetoothDeviceAddress, true);
                        }
                    }, 11000);
                }

                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                Log.d(TAG, "Services discovered successfully");
            } else {
                Log.d(TAG, "The method 'onServicesDiscovered' received: " + status);
                if(status == 129) {
                    if(mBluetoothAdapter != null)
                        mBluetoothAdapter.cancelDiscovery();

                    mHandler.postDelayed(new Runnable() {
                     @Override
                     public void run() {
                         try {
                             if(mBluetoothGatt != null && mBluetoothAdapter !=null)
                                 mBluetoothGatt.discoverServices();
                         } catch (NullPointerException ex) {
                             ex.printStackTrace();
                         }
                     }
                 }, 60);
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            } else {
                Log.d(TAG, "The method 'onCharacteristicRead' received: " + status);
            }

        }

        // invokes broadcast when notified about any changes in characteristic
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    // broadcasting characteristic's data to LeConnectedDeviceActivity
    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {

        final Intent intent = new Intent(action);

        final byte[] data = characteristic.getValue();
        String value;
        if (data != null && data.length > 0) {
            value = new String(data);
            intent.putExtra(EXTRA_DATA, value);

            // retrieving the double value from the temperature notification
            if(value.charAt(0) == 't' && !value.contains("g")) {
                try {
                    StringBuilder sb = new StringBuilder(value);
                    value = sb.substring(2);
                    value = value.trim();

                    double tempValue = Double.parseDouble(value);

                    Temperature temperature = new Temperature(tempValue);
                    CentralRepository.getInstance(SensorsValuesDatabase.getDatabase(getApplicationContext())).insertTemperature(temperature);
                }catch (Exception e){
                    e.printStackTrace();
                }
            } else if(value.charAt(0) == 'g' && !value.contains("t")) {
                // retrieving the double value from the gas notification
                try {
                    StringBuilder sb = new StringBuilder(value);
                    value = sb.substring(2);
                    value = value.trim();

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
       // Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        Log.d(TAG, "SERVICE ONDESTROY");
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
                    mBluetoothGatt.connect();

                    mBluetoothDeviceAddress = address;
                    mConnectionState = STATE_CONNECTING;
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
            }, 40);
        }
    }

    // releasing resources
    public void close() {
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
            }, 80);
        }

    }

    // requesting to read characteristic
    // the result is asynchronously returned through BluetoothGattCallback in onCharacteristicRead()
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.d(TAG, "BluetoothAdapter is not initialized");
            return;
        }

        mHandler.post(new Runnable(){
            @Override
            public void run() {
                try {
                    Log.d(TAG, "READING CHARACTERISTIC");
                    mBluetoothGatt.readCharacteristic(characteristic);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // enabling notification for the characteristic with sensors' data
    public void enableCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enable) {

        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.d(TAG, "BluetoothAdapter is not initialized");
            return;
        }

        if(enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "ENABLING CHARACTERISTIC NOTIFICATION");
                    try {
                        if(mBluetoothGatt != null && mBluetoothAdapter.isEnabled()) {
                            Boolean notificationStatus = mBluetoothGatt.setCharacteristicNotification(characteristic, enable);

                            if (!notificationStatus) {
                                Log.d(TAG, "Enabling notification failed!");
                                return;
                            }

                            sensorsDataCharacteristic = characteristic;

                            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID);
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            mBluetoothGatt.writeDescriptor(descriptor);
                        }
                    } catch(Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }, 200);
        } else
            if (sensorsDataCharacteristic != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "DISABLING CHARACTERISTIC NOTIFICATION");
                    try {

                        Boolean notificationStatus = mBluetoothGatt.setCharacteristicNotification(characteristic, enable);

                        if(!notificationStatus) {
                            Log.d(TAG, "disabling notification failed!");
                            return;
                        }

                        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID);
                        descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                        mBluetoothGatt.writeDescriptor(descriptor);

                        sensorsDataCharacteristic = null;

                    } catch(Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
        }
    }

    // when gattServices are discovered, this method
    // retrieves a list of the services, which are available on the device
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null)
            return null;

        return mBluetoothGatt.getServices();
    }

}

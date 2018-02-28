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
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.util.List;
import java.util.UUID;

public class BluetoothLowEnergyService extends Service {
    private final static String TAG = BluetoothLowEnergyService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
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

                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

            } else if (newState == STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                /*Toast.makeText(BluetoothLowEnergyService.this, "Disconnected from GATT server.",
                        Toast.LENGTH_SHORT).show();*/
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                /*Toast.makeText(BluetoothLowEnergyService.this, "The method 'onServicesDiscovered' received: " + status,
                        Toast.LENGTH_SHORT).show();*/
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
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

       /* Toast.makeText(BluetoothLowEnergyService.this, "Broadcasting characteristic update " + characteristic.getUuid(),
                Toast.LENGTH_SHORT).show();*/

        final byte[] data = characteristic.getValue();
        if (data != null && data.length > 0) {
            intent.putExtra(EXTRA_DATA, new String(data));
        }
        sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        BluetoothLowEnergyService getService() {
            return BluetoothLowEnergyService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // when LeConnectedDeviceActivity is destroyed, it unbinds the current service
        // afterwards, the cleanup is performed
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();


    // initializing local BluetoothAdapter
    public boolean initialize() {

        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                /*Toast.makeText(BluetoothLowEnergyService.this, "BluetoothManager initialization failed.",
                        Toast.LENGTH_SHORT).show();*/
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            /*Toast.makeText(BluetoothLowEnergyService.this, "BluetoothAdapter initialization failed.",
                    Toast.LENGTH_SHORT).show();*/
            return false;
        }

        return true;
    }


    // Connecting to the GATT server of the device
    // result is returned asynchronously through BluetoothGattCallback in onConnectionStateChange()
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            /*Toast.makeText(BluetoothLowEnergyService.this, "Unspecified address or uninitialized BluetoothAdapter.",
                    Toast.LENGTH_SHORT).show();*/
            return false;
        }

        // Trying to reconnect to previously connected device
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            /*Toast.makeText(BluetoothLowEnergyService.this, "Trying to use an existing mBluetoothGatt for connection.",
                    Toast.LENGTH_SHORT).show();*/
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            /*Toast.makeText(BluetoothLowEnergyService.this, "Device not found.  Unable to connect",
                    Toast.LENGTH_SHORT).show();*/
            return false;
        }

        // directly connecting to the device, therefore 'autoConnect' is set to false
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        /*Toast.makeText(BluetoothLowEnergyService.this, "Trying to create a new connection",
                Toast.LENGTH_SHORT).show();*/

        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    // disconnect or cancel the current connection
    // the result is reported via BluetoothGattCallback in onConnectionStateChange()
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            /*Toast.makeText(BluetoothLowEnergyService.this, "BluetoothAdapter is not initialized",
                    Toast.LENGTH_SHORT).show();*/
            return;
        }
        mBluetoothGatt.disconnect();
    }

    // releasing resources
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    // requesting to read characteristic
    // the result is asynchronously returned through BluetoothGattCallback in onCharacteristicRead()
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            /*Toast.makeText(BluetoothLowEnergyService.this, "BluetoothAdapter is not initialized",
                    Toast.LENGTH_SHORT).show();*/
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    // enabling notification for the characteristic with sensors' data
    public void enableCharacteristicNotification(BluetoothGattCharacteristic characteristic) {

        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            /*Toast.makeText(BluetoothLowEnergyService.this, "BluetoothAdapter is not initialized",
                    Toast.LENGTH_SHORT).show();*/
            return;
        }

        mBluetoothGatt.setCharacteristicNotification(characteristic, true);
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBluetoothGatt.writeDescriptor(descriptor);

    }

    // when gattServices are discovered, this method
    // retrieves a list of the services, which are available on the device
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }

}

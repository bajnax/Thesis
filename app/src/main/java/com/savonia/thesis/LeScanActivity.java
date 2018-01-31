package com.savonia.thesis;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class LeScanActivity extends AppCompatActivity {

    private final static int REQUEST_ENABLE_BT = 1;
    private final static int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1;
    private BluetoothAdapter mBluetoothAdapter;
    private Button lookUp;
    private Button stopLookUp;
    private ProgressBar spinner;
    private TextView lookUpText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_le_scan);
        lookUp =  (Button) findViewById(R.id.lookUpBtn);
        stopLookUp = (Button) findViewById(R.id.stopLookUpBtn);
        spinner = (ProgressBar) findViewById(R.id.spinner);
        lookUpText = (TextView) findViewById(R.id.lookUpTextView);

        Drawable progressDrawable = spinner.getIndeterminateDrawable().mutate();
        progressDrawable.setColorFilter(getColor(R.color.colorMain), PorterDuff.Mode.MULTIPLY);
        spinner.setProgressDrawable(progressDrawable);

        // Initially, the 'Stop' button and the 'spinner' are invisible
        stopLookUp.setVisibility(View.INVISIBLE);
        spinner.setVisibility(View.GONE);

        lookUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (isAccessCoarseLocationAllowed())
                    startScanning();
                else
                    requestAccessCoarseLocation();
            }
        });

        stopLookUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopScanning();
            }
        });

        // Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

    }

    private void startScanning() {
        lookUp.setVisibility(View.INVISIBLE);
        stopLookUp.setVisibility(View.VISIBLE);
        spinner.setVisibility(View.VISIBLE);
        lookUpText.setText(getResources().getString(R.string.look_up_text_view_scanning));

    }

    private void stopScanning() {
        stopLookUp.setVisibility(View.INVISIBLE);
        spinner.setVisibility(View.INVISIBLE);
        lookUp.setVisibility(View.VISIBLE);
        lookUpText.setText(getResources().getString(R.string.look_up_text_view_initial));

    }

    private boolean isAccessCoarseLocationAllowed() {
        if (ContextCompat.checkSelfPermission(LeScanActivity.this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)
            return false;
        else
            return true;

    }
    private void requestAccessCoarseLocation() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(LeScanActivity.this,
                Manifest.permission.ACCESS_COARSE_LOCATION)) {

            // If the user denied the permission previously, it will be shown again
            ActivityCompat.requestPermissions(LeScanActivity.this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);

        } else {
            ActivityCompat.requestPermissions(LeScanActivity.this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Toast.makeText(LeScanActivity.this, "Permission Granted!",
                            Toast.LENGTH_SHORT).show();

                    startScanning();

                } else {

                    Toast.makeText(LeScanActivity.this, "Permission Denied!",
                            Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

}
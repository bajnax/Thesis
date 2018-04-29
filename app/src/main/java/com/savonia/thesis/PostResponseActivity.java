package com.savonia.thesis;

import android.arch.lifecycle.ViewModelProviders;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.savonia.thesis.db.entity.Gas;
import com.savonia.thesis.db.entity.Temperature;
import com.savonia.thesis.viewmodels.SaMiViewModel;
import com.savonia.thesis.webclient.measuremetsmodels.DataModel;
import com.savonia.thesis.webclient.measuremetsmodels.MeasurementsModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

public class PostResponseActivity extends AppCompatActivity {

    private static final String TAG = PostResponseActivity.class.getSimpleName();

    private SaMiViewModel saMiViewModel;
    private MeasurementsModel initialMeasurement;
    private MeasurementsModel responseMeasurement;
    private EditText measurementNameEdTxt;
    private EditText measurementTagEdTxt;
    private EditText temperatureTagEdTxt;
    private EditText gasTagEdTxt;
    private Button sendButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_response);

        measurementNameEdTxt = (EditText) findViewById(R.id.measurementName);
        measurementTagEdTxt = (EditText) findViewById(R.id.measurementTag);
        temperatureTagEdTxt = (EditText) findViewById(R.id.temperatureTag);
        gasTagEdTxt = (EditText) findViewById(R.id.gasTag);
        sendButton = (Button) findViewById(R.id.postButton);

        saMiViewModel = ViewModelProviders.of(PostResponseActivity.this).get(SaMiViewModel.class);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                initialMeasurement = null;

                // generating a measurement model
                initialMeasurement = new MeasurementsModel();

                if(measurementNameEdTxt.getText().toString().trim().length() > 0)
                    initialMeasurement.setObject(measurementNameEdTxt.getText().toString().trim());
                else
                    initialMeasurement.setObject("Safety control unit");

                if(measurementTagEdTxt.getText().toString().trim().length() > 0)
                    initialMeasurement.setTag(measurementTagEdTxt.getText().toString().trim());
                else
                    initialMeasurement.setTag("Some measurement");

                Date date = GregorianCalendar.getInstance().getTime();
                String currentDateAndTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
                        .format(date);
                Log.d(TAG, "Current date and time: " + currentDateAndTime);
                initialMeasurement.setTimestampISO8601(currentDateAndTime);
                //initialMeasurement.setTimestampISO8601("2018-04-28T17:48:19+03:00");
                List<DataModel> dataModelList = new ArrayList<>();
                initialMeasurement.setData(dataModelList);

                saMiViewModel.generateTemperatureMeasurementAsync();
            }
        });

        // retrieving the list of temperatures from the database
        saMiViewModel.getTemperatureMeasurementAsync().observe(PostResponseActivity.this, temperatures -> {

            try {
                if(temperatures != null) {
                    if(temperatures.size() > 0) {

                        Log.d(TAG, "GENERATING TEMPERATURE MEASUREMENT");

                        String tag = "";
                        if(temperatureTagEdTxt.getText().toString().trim().length() > 0)
                            tag = temperatureTagEdTxt.getText().toString().trim();
                        else
                            tag = "Some temperature";

                        // generating a measurement from the retrieved list of temperatures
                        for (int i = 0; i < temperatures.size(); i++) {
                            DataModel dm = new DataModel();
                            Log.d(TAG, "Temperature value: " + temperatures.get(i).getTemperatureValue());
                            dm.setTag(tag);
                            dm.setValue(temperatures.get(i).getTemperatureValue());
                            initialMeasurement.getData().add(dm);
                        }
                    }

                    saMiViewModel.generateGasMeasurementAsync();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });


        // retrieving the list of gases from the database and sending the measurement
        saMiViewModel.getGasMeasurementAsync().observe(PostResponseActivity.this, gases -> {

            try {
                if(gases != null) {
                    if(gases.size() > 0) {
                        Log.d(TAG, "GENERATING GAS MEASUREMENT");

                        String tag = "";
                        if(gasTagEdTxt.getText().toString().trim().length() > 0)
                            tag = gasTagEdTxt.getText().toString().trim();
                        else
                            tag = "Some gas";

                        // adding the retrieved list of gases to the measurement
                        for (int i = 0; i < gases.size(); i++) {
                            DataModel dm = new DataModel();
                            Log.d(TAG, "Gas value: " + gases.get(i).getGasValue());
                            dm.setTag(tag);
                            dm.setValue(gases.get(i).getGasValue());
                            initialMeasurement.getData().add(dm);
                        }
                    }

                    Log.d(TAG, "SENDING GENERATED TEMPERATURE AND/OR GAS MEASUREMENT VIA POST REQUEST");
                    // sending generated measurement to the SaMi cloud via POST request
                    saMiViewModel.makePostRequest("savoniatest", initialMeasurement);

                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });


        // result of the POST request
        saMiViewModel.getResponseStatus().observe(PostResponseActivity.this, response -> {
            if(response != 0) {
                if(response == 1) {
                    Toast.makeText(PostResponseActivity.this, "The data has been successfully saved in the SaMi cloud!",
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(PostResponseActivity.this, "Error occurred while sending the data. " +
                                    "Check your internet connection or contact Mikko Paakkonen!",
                            Toast.LENGTH_LONG).show();
                }
            }

        });

        saMiViewModel.getPostResponseMeasurement().observe(PostResponseActivity.this, postedMeasurement -> {
            if(postedMeasurement != null) {
                responseMeasurement = postedMeasurement.get(0);
                try {

                    for (int j = 0; j < responseMeasurement.getData().size(); j++) {
                        Log.d(TAG, "RESPONSE FROM POST REQUEST RECEIVED (" + j + "): " +
                                responseMeasurement.getData().get(j).getValue());
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

    }


}

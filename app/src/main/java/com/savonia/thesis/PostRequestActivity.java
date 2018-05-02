package com.savonia.thesis;

import android.arch.lifecycle.ViewModelProviders;
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
import java.util.Locale;

public class PostRequestActivity extends AppCompatActivity {

    private static final String TAG = PostRequestActivity.class.getSimpleName();

    private SaMiViewModel saMiViewModel;
    private List<Temperature> temperatureList;
    private List<Gas> gasList;
    private String measurementTag = "";
    private String measurementName = "";
    private MeasurementsModel responseMeasurement;
    private ArrayList<MeasurementsModel> initialMeasurements;
    private EditText measurementNameEdTxt;
    private EditText measurementTagEdTxt;
    private EditText temperatureTagEdTxt;
    private EditText gasTagEdTxt;
    private SimpleDateFormat simpleDateFormat;
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

        simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH);


        saMiViewModel = ViewModelProviders.of(PostRequestActivity.this).get(SaMiViewModel.class);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                initialMeasurements = null;
                initialMeasurements = new ArrayList<MeasurementsModel>();

                if(measurementNameEdTxt.getText().toString().trim().length() > 0)
                    measurementName = measurementNameEdTxt.getText().toString().trim();
                else
                    measurementName = getResources().getString(R.string.measurement_name);

                if(measurementTagEdTxt.getText().toString().trim().length() > 0)
                    measurementTag = measurementTagEdTxt.getText().toString().trim();
                else
                    measurementTag = getResources().getString(R.string.measurement_tag);


                saMiViewModel.generateTemperatureMeasurementAsync();
            }
        });

        // retrieving the list of temperatures from the database
        saMiViewModel.getTemperatureMeasurementAsync().observe(PostRequestActivity.this, temperatures -> {

            try {
                if(temperatures != null) {
                    temperatureList = temperatures;
                    saMiViewModel.generateGasMeasurementAsync();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });


        // retrieving the list of gases from the database and sending the measurement
        saMiViewModel.getGasMeasurementAsync().observe(PostRequestActivity.this, gases -> {

            try {
                if(gases != null) {
                    gasList = gases;
                    buildRequest();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });


        // result of the POST request
        saMiViewModel.getResponseStatus().observe(PostRequestActivity.this, response -> {
            if(response != 0) {
                if(response == 1) {
                    Toast.makeText(PostRequestActivity.this, "The data has been successfully saved in the SaMi cloud!",
                            Toast.LENGTH_LONG).show();
                    // closing activity
                    PostRequestActivity.this.finish();
                    // TODO: clear the database here
                } else {
                    Toast.makeText(PostRequestActivity.this, "Error occurred while sending the data. " +
                                    "Check your internet connection or contact Mikko Paakkonen!",
                            Toast.LENGTH_LONG).show();
                }
            }

        });

        saMiViewModel.getPostResponseMeasurement().observe(PostRequestActivity.this, postedMeasurement -> {
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

    public void buildRequest() {
        // TEMPERATURES
        if(temperatureList.size() > 0) {

            Log.d(TAG, "GENERATING TEMPERATURE MEASUREMENT");

            String tag = "";
            if(temperatureTagEdTxt.getText().toString().trim().length() > 0)
                tag = temperatureTagEdTxt.getText().toString().trim();
            else
                tag = getResources().getString(R.string.temperature_tag);


            // generating a measurement from the retrieved list of temperatures
            for (int i = 0; i < temperatureList.size(); i++) {
                MeasurementsModel mM = new MeasurementsModel();
                ArrayList<DataModel> dMList = new ArrayList<DataModel>();
                DataModel dM = new DataModel();

                // preparing MeasurementModel
                mM.setTag(measurementTag);
                mM.setObject(measurementName);
                mM.setTimestampISO8601(simpleDateFormat.format(temperatureList.get(i).getDate()));

                // preaparing DataModel
                Log.d(TAG, "Temperature value: " + temperatureList.get(i).getTemperatureValue());
                Log.d(TAG, "Temperature time: " + simpleDateFormat.format(temperatureList.get(i).getDate()));
                dM.setTag(tag);
                dM.setValue(temperatureList.get(i).getTemperatureValue());
                // adding DataModel to the list of DataModels
                dMList.add(dM);
                // adding a list of DataModels to MeasurementModel
                mM.setData(dMList);
                // adding MeasurementModel to the list of MeasurementModels
                initialMeasurements.add(mM);
            }

        }


        // GASES
        if(gasList.size() > 0) {
            Log.d(TAG, "GENERATING GAS MEASUREMENT");

            String tag = "";
            if(gasTagEdTxt.getText().toString().trim().length() > 0)
                tag = gasTagEdTxt.getText().toString().trim();
            else
                tag = getResources().getString(R.string.gas_tag);

            // adding the retrieved list of gases to the measurement
            if(initialMeasurements.size() > gasList.size()) {
                for (int i = 0; i < gasList.size(); i++) {
                    DataModel dM = new DataModel();

                    Log.d(TAG, "Gas value: " + gasList.get(i).getGasValue());
                    dM.setTag(tag);
                    dM.setValue(gasList.get(i).getGasValue());
                    initialMeasurements.get(i).getData().add(dM);
                }
            } else {
                for (int i = 0; i < gasList.size(); i++) {
                    DataModel dM = new DataModel();

                    if(i < initialMeasurements.size()) {
                        Log.d(TAG, "Gas value: " + gasList.get(i).getGasValue());
                        dM.setTag(tag);
                        dM.setValue(gasList.get(i).getGasValue());
                        initialMeasurements.get(i).getData().add(dM);
                    } else {
                        MeasurementsModel mM = new MeasurementsModel();
                        ArrayList<DataModel> dMList = new ArrayList<DataModel>();

                        // preparing MeasurementModel
                        mM.setTag(measurementTag);
                        mM.setObject(measurementName);
                        mM.setTimestampISO8601(simpleDateFormat.format(gasList.get(i).getDate()));

                        // preparing DataModel
                        Log.d(TAG, "Gas value: " + gasList.get(i).getGasValue());
                        dM.setTag(tag);
                        dM.setValue(gasList.get(i).getGasValue());

                        // adding DataModel to the list of DataModels
                        dMList.add(dM);

                        // adding the list of DataModels to the MeasurementModel
                        mM.setData(dMList);
                        // addig MeasurementModel to the list of MeasurementModels
                        initialMeasurements.add(mM);
                    }
                }
            }
        }

        // sending generated measurement to the SaMi cloud via POST request
        Log.d(TAG, "SENDING GENERATED TEMPERATURE AND/OR GAS MEASUREMENT VIA POST REQUEST");
        saMiViewModel.makePostRequest("savoniatest", initialMeasurements);
    }

}

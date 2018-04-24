package com.savonia.thesis;

import android.arch.lifecycle.ViewModelProviders;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.savonia.thesis.db.entity.Temperature;
import com.savonia.thesis.viewmodels.SaMiViewModel;
import com.savonia.thesis.webclient.measuremetsmodels.DataModel;
import com.savonia.thesis.webclient.measuremetsmodels.MeasurementsModel;

import java.util.ArrayList;
import java.util.List;

public class PostResponseActivity extends AppCompatActivity {

    private static final String TAG = PostResponseActivity.class.getSimpleName();

    private SaMiViewModel saMiViewModel;
    private MeasurementsModel initialMeasurement;
    private MeasurementsModel responseMeasurement;
    private List<Temperature> temperatureList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_response);

        saMiViewModel = ViewModelProviders.of(PostResponseActivity.this).get(SaMiViewModel.class);

        // retrieving the list of temperatures from the database
        saMiViewModel.generateTemperatureMeasurementAsync();
        saMiViewModel.getTemperatureMeasurementAsync().observe(PostResponseActivity.this, temperatures -> {

            try {
                if(temperatures != null) {
                    initialMeasurement = new MeasurementsModel();
                    temperatureList = temperatures;
                    Log.d(TAG, "GENERATING TEMPERATURE MEASUREMENT");

                    // generating a measurement from the retrieved list of temperatures
                    initialMeasurement.setTag("KondopogaTemperature");
                    initialMeasurement.setTimestampISO8601("2018-04-23 15:20:00");

                    List<DataModel> temps = new ArrayList<>();
                    for(int i = 0; i < temperatureList.size(); i++) {
                        DataModel dm = new DataModel();
                        Log.d(TAG, "Temperature value: " + temperatureList.get(i).getTemperatureValue());
                        dm.setValue(temperatureList.get(i).getTemperatureValue());
                        temps.add(dm);
                    }

                    initialMeasurement.setData(temps);

                    Log.d(TAG, "SENDING GENERATED TEMPERATURE MEASUREMENT VIA POST REQUEST");
                    // sending generated measurement to the SaMi cloud via POST request
                    saMiViewModel.makePostRequest("savoniatest", initialMeasurement);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
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

package com.savonia.thesis;

import android.arch.lifecycle.ViewModelProviders;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.savonia.thesis.viewmodels.SaMiViewModel;
import com.savonia.thesis.webclient.measuremetsmodels.MeasurementsModel;

public class PostResponseActivity extends AppCompatActivity {

    private static final String TAG = PostResponseActivity.class.getSimpleName();

    private SaMiViewModel saMiViewModel;
    private MeasurementsModel initialMeasurement;
    private MeasurementsModel responseMeasurement;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_response);

        saMiViewModel = ViewModelProviders.of(PostResponseActivity.this).get(SaMiViewModel.class);
        initialMeasurement = generateMeasurement();

        saMiViewModel.makePostRequest("savoniatest", initialMeasurement);
        saMiViewModel.getPostResponseMeasurement().observe(PostResponseActivity.this, postedMeasurement -> {
            responseMeasurement = postedMeasurement;
            try {

                for(int j = 0; j<responseMeasurement.getData().size(); j++) {
                    Log.d(TAG, "RESPONSE FROM POST REQUEST RECEIVED (" + j +"): " +
                            responseMeasurement.getData().get(j).getValue());
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

    }

    public MeasurementsModel generateMeasurement() {
        MeasurementsModel measurement = new MeasurementsModel();
        //TODO: create interface with TextViews to set up bounds for a measurement and select a sensor type
        return measurement;
    }
}

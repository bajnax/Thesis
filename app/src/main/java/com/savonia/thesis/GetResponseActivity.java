package com.savonia.thesis;

import android.arch.lifecycle.ViewModelProviders;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.PointsGraphSeries;
import com.savonia.thesis.db.entity.Gas;
import com.savonia.thesis.viewmodels.SaMiViewModel;
import com.savonia.thesis.webclient.measuremetsmodels.DataModel;
import com.savonia.thesis.webclient.measuremetsmodels.MeasurementsModel;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class GetResponseActivity extends AppCompatActivity {

    private static final String TAG = GetResponseActivity.class.getSimpleName();

    private SaMiViewModel saMiViewModel;
    private List<MeasurementsModel> measurementsModelList;

    private GraphView measurementsGraph;
    private PointsGraphSeries<DataPoint> gasSeries;
    private PointsGraphSeries<DataPoint> temperatureSeries;
    private SimpleDateFormat mDateFormatter;

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private Runnable mTimer1;
    private Runnable mTimer2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_response);

        measurementsGraph = (GraphView)findViewById(R.id.measurementsGraph);
        //mDateFormatter = new SimpleDateFormat("HH:mm:ss", Locale.US);

        measurementsGraph.setTitle("Current sensors data");
        measurementsGraph.setTitleColor(R.color.colorPrimaryDark);
        measurementsGraph.getGridLabelRenderer().setVerticalAxisTitle("Value");
        measurementsGraph.getGridLabelRenderer().setHorizontalAxisTitle("Id");

        // enabling horizontal zooming and scrolling
        measurementsGraph.getViewport().setScalable(true);

        measurementsGraph.getGridLabelRenderer().setLabelVerticalWidth(70);
        measurementsGraph.getGridLabelRenderer().setLabelHorizontalHeight(50);

        measurementsGraph.getViewport().setYAxisBoundsManual(true);
        measurementsGraph.getViewport().setMinY(0);
        measurementsGraph.getViewport().setMaxY(400);

        /*// set date label formatter
        measurementsGraph.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(GetResponseActivity.this, mDateFormatter));
        measurementsGraph.getGridLabelRenderer().setNumHorizontalLabels(4); // only 4 because of the space

        Calendar calendar = Calendar.getInstance();
        long t1 = calendar.getTimeInMillis();
        long t2 = calendar.getTimeInMillis() + 15000;

        measurementsGraph.getViewport().setXAxisBoundsManual(true);
        measurementsGraph.getViewport().setMinX((double)t1);
        measurementsGraph.getViewport().setMaxX(((double)t2));


        // as we use dates as labels, the human rounding to nice readable numbers
        // is not necessary
        measurementsGraph.getGridLabelRenderer().setHumanRounding(false);*/

        temperatureSeries = new PointsGraphSeries<>();
        measurementsGraph.addSeries(temperatureSeries);


        gasSeries = new PointsGraphSeries<>();
        // set gas at the second scale
        measurementsGraph.getSecondScale().addSeries(gasSeries);
        // the y bounds are always manual for second scale
        measurementsGraph.getSecondScale().setMinY(0);
        measurementsGraph.getSecondScale().setMaxY(400);
        gasSeries.setColor(Color.RED);
        measurementsGraph.getGridLabelRenderer().setVerticalLabelsSecondScaleColor(Color.RED);


        saMiViewModel = ViewModelProviders.of(GetResponseActivity.this).get(SaMiViewModel.class);

        saMiViewModel.makeGetRequest();
        saMiViewModel.getMeasurements().observe(GetResponseActivity.this, measurementsModels -> {
            measurementsModelList = measurementsModels;
            try {

                if(measurementsModelList != null) {

                    for (int i = 0; i < measurementsModelList.size(); i++) {
                        for (int j = 0; j < measurementsModelList.get(i).getData().size(); j++) {
                            Log.d(TAG, "RESPONSE FROM GET REQUEST RECEIVED (" + i + ", " + j + ")" +
                                    measurementsModelList.get(i).getData().get(j).getValue());
                        }
                    }

                    for (int i = 0; i < 2; i++) {
                        for (int j = 0; j < measurementsModelList.get(i).getData().size(); j++) {
                            if (i == 0) {//temp
                                displayTemperatures(measurementsModelList.get(i).getData());
                            } else { //gas
                                displayGases(measurementsModelList.get(i).getData());
                            }
                        }
                    }

                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

    }


    private void displayTemperatures(List<DataModel> temperatures) {

        mTimer1 = new Runnable()
        {
            @Override
            public void run() {
                try {
                    DataPoint[] values = new DataPoint[temperatures.size()];
                    for (int i = 0; i < temperatures.size(); i++){
                        DataPoint t = new DataPoint(i , temperatures.get(i).getValue());
                        values[i] = t;
                    }
                    temperatureSeries.resetData(values);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        mHandler.post(mTimer1);

    }


    private void displayGases(List<DataModel> gases) {

        mTimer2 = new Runnable()
        {
            @Override
            public void run() {
                try {
                    DataPoint[] values = new DataPoint[gases.size()];
                    for (int i = 0; i < gases.size(); i++){
                        DataPoint t = new DataPoint(i , gases.get(i).getValue());
                        values[i] = t;
                    }
                    gasSeries.resetData(values);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        mHandler.post(mTimer2);

    }


    @Override
    public void onStop() {
        super.onStop();
        mHandler.removeCallbacks(mTimer1);
        mHandler.removeCallbacks(mTimer2);
    }

}

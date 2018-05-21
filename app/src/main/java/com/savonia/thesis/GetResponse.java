package com.savonia.thesis;


import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.DataPointInterface;
import com.jjoe64.graphview.series.OnDataPointTapListener;
import com.jjoe64.graphview.series.PointsGraphSeries;
import com.jjoe64.graphview.series.Series;
import com.savonia.thesis.viewmodels.GetRequestViewModel;
import com.savonia.thesis.viewmodels.SaMiViewModel;
import com.savonia.thesis.webclient.measuremetsmodels.DataModel;
import com.savonia.thesis.webclient.measuremetsmodels.MeasurementsModel;

import java.util.ArrayList;
import java.util.List;


public class GetResponse extends Fragment {

    private static final String TAG = GetResponse.class.getSimpleName();
    private static final String ARG_PARAM1 = "param1";

    private String mParam1;

    private View rootView;
    private GetRequestViewModel getRequestViewModel;

    private GraphView measurementsGraph;
    private PointsGraphSeries<DataPoint> gasSeries;
    private PointsGraphSeries<DataPoint> temperatureSeries;

    private String temperatureTag;
    private String gasTag;
    private List<DataModel> temperaturesList;
    private List<DataModel> gasesList;


    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private Runnable mTimer1;
    private Runnable mTimer2;

    public GetResponse() {
        // Required empty public constructor
    }

    public static GetResponse newInstance(String param1) {
        GetResponse fragment = new GetResponse();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
        }
        setRetainInstance(true);
        getRequestViewModel = ViewModelProviders.of(getActivity()).get(GetRequestViewModel.class);

        getRequestViewModel.getTemperatureTag().observe(getActivity(), new Observer<String>() {
            @Override
            public void onChanged(@NonNull String tag) {
                temperatureTag = tag;
                Log.d(TAG, "Temperature tag: " + temperatureTag);
            }
        });

        getRequestViewModel.getGasTag().observe(getActivity(), new Observer<String>() {
            @Override
            public void onChanged(@NonNull String tag) {
                gasTag = tag;
                Log.d(TAG, "Gas tag: " + gasTag);
            }
        });

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_get_response, container, false);

        measurementsGraph = (GraphView)rootView.findViewById(R.id.measurementsGraph);

        measurementsGraph.setTitle("Get response data");
        measurementsGraph.setTitleColor(R.color.colorPrimaryDark);
        measurementsGraph.getGridLabelRenderer().setVerticalAxisTitle("Value");
        //measurementsGraph.getGridLabelRenderer().setHorizontalAxisTitle("Id");

        // enabling horizontal zooming and scrolling
        measurementsGraph.getViewport().setScalable(true);
        /*// enabling vertical zooming and scrolling
        measurementsGraph.getViewport().setScalableY(true);*/


        measurementsGraph.getGridLabelRenderer().setLabelVerticalWidth(70);
        measurementsGraph.getGridLabelRenderer().setLabelHorizontalHeight(50);
        measurementsGraph.getGridLabelRenderer().setLabelsSpace(20);
        measurementsGraph.getGridLabelRenderer().setPadding(65);
        measurementsGraph.getGridLabelRenderer().setHighlightZeroLines(true);
        measurementsGraph.getViewport().setYAxisBoundsManual(true);

        gasSeries = new PointsGraphSeries<>();
        temperatureSeries = new PointsGraphSeries<>();

        // shows toast regarding the point, on which the user clicked
        gasSeries.setOnDataPointTapListener(new OnDataPointTapListener() {
            @Override
            public void onTap(Series series, DataPointInterface dataPoint) {
                Toast.makeText(getActivity(), "Gas value (ppm): " + dataPoint.getY(), Toast.LENGTH_SHORT).show();
            }
        });

        // shows toast regarding the point, on which the user clicked
        temperatureSeries.setOnDataPointTapListener(new OnDataPointTapListener() {
            @Override
            public void onTap(Series series, DataPointInterface dataPoint) {
                Toast.makeText(getActivity(), "Temperature value (C): " + dataPoint.getY(), Toast.LENGTH_SHORT).show();
            }
        });

        // the y bounds are always manual for second scale
        measurementsGraph.getSecondScale().setMinY(-15);
        measurementsGraph.getSecondScale().setMaxY(50);
        gasSeries.setColor(Color.BLUE);
        measurementsGraph.getGridLabelRenderer().setVerticalLabelsSecondScaleColor(Color.BLUE);

        // set temperatures at the second scale
        measurementsGraph.getSecondScale().addSeries(temperatureSeries);

        // set gases at the first scale
        measurementsGraph.getViewport().setMinY(0);
        measurementsGraph.getViewport().setMaxY(2000);
        gasSeries.setColor(Color.RED);
        measurementsGraph.getGridLabelRenderer().setVerticalLabelsColor(Color.RED);
        measurementsGraph.addSeries(gasSeries);

        // set manual bounds for X-axes
        measurementsGraph.getViewport().setXAxisBoundsManual(true);
        measurementsGraph.getViewport().setMinX(0);
        measurementsGraph.getViewport().setMaxX(10);

        final SaMiViewModel saMiViewModel = ViewModelProviders.of(getActivity()).get(SaMiViewModel.class);

        saMiViewModel.getMeasurements().observe(getActivity(), measurementsModels -> {
            try {

                if(measurementsModels != null) {

                    Log.d(TAG, "Received GET response");

                    Log.d(TAG, "measurementsModels size: " + measurementsModels.size());

                    temperaturesList = new ArrayList<DataModel>();
                    gasesList = new ArrayList<DataModel>();

                    for (MeasurementsModel currentMeasModel: measurementsModels) {
                        for (DataModel currentDataModel: currentMeasModel.getData()) {
                            Log.d(TAG, "RESPONSE FROM GET REQUEST RECEIVED " + currentDataModel.getValue());
                            if(temperatureTag != null && currentDataModel.getTag().equals(temperatureTag)) {
                                temperaturesList.add(currentDataModel);
                            } else if(gasTag != null && currentDataModel.getTag().equals(gasTag)) {
                                gasesList.add(currentDataModel);
                            }

                        }
                    }
                    measurementsGraph.getViewport().setMaxX(measurementsModels.size());

                    if(temperaturesList.isEmpty() && gasesList.isEmpty()) {
                        Toast.makeText(getActivity(), "Received measurements that do not contain values!",
                                Toast.LENGTH_LONG).show();
                    }

                    displayTemperatures(temperaturesList);
                    displayGases(gasesList);

                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        return rootView;
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
    public void onDetach() {
        super.onDetach();
        mHandler.removeCallbacks(mTimer1);
        mHandler.removeCallbacks(mTimer2);
    }

    // closing keyboard when switching between tabs
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            try {
                InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(getView().getWindowToken(), 0);
                inputMethodManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
            } catch (Exception e) {
                Log.e(TAG, "setUserVisibleHint: ", e);
            }
        }
    }

}

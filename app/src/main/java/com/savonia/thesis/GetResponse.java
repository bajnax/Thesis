package com.savonia.thesis;


import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
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

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.PointsGraphSeries;
import com.savonia.thesis.viewmodels.GetRequestViewModel;
import com.savonia.thesis.viewmodels.SaMiViewModel;
import com.savonia.thesis.webclient.measuremetsmodels.DataModel;

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
            }
        });

        getRequestViewModel.getGasTag().observe(getActivity(), new Observer<String>() {
            @Override
            public void onChanged(@NonNull String tag) {
                gasTag = tag;
            }
        });

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_get_response, container, false);

        measurementsGraph = (GraphView)rootView.findViewById(R.id.measurementsGraph);
        //mDateFormatter = new SimpleDateFormat("HH:mm:ss", Locale.US);

        measurementsGraph.setTitle("Get response data");
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

        final SaMiViewModel saMiViewModel = ViewModelProviders.of(getActivity()).get(SaMiViewModel.class);

        saMiViewModel.getMeasurements().observe(getActivity(), measurementsModels -> {
            try {

                if(measurementsModels != null) {

                    Log.d(TAG, "Received GET response");

                    Log.d(TAG, "measurementsModels size: " + measurementsModels.size());
                    Log.d(TAG, "DataList size: " + measurementsModels.get(0).getData().size());

                    temperaturesList = new ArrayList<DataModel>();
                    gasesList = new ArrayList<DataModel>();

                    for (int i = 0; i < measurementsModels.size(); i++) {
                        for (int j = 0; j < measurementsModels.get(i).getData().size(); j++) {
                            Log.d(TAG, "RESPONSE FROM GET REQUEST RECEIVED (" + i + ", " + j + ")" +
                                    measurementsModels.get(i).getData().get(j).getValue());

                            if(temperatureTag != null && measurementsModels.get(i).getData().get(j).getTag().equals(temperatureTag)) {
                                temperaturesList.add(measurementsModels.get(i).getData().get(j));
                            } else if(gasTag != null && measurementsModels.get(i).getData().get(j).getTag().equals(gasTag)) {
                                gasesList.add(measurementsModels.get(i).getData().get(j));
                            }

                        }
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

}

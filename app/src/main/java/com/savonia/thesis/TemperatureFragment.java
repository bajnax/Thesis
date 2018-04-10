package com.savonia.thesis;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.PointsGraphSeries;
import com.savonia.thesis.db.entity.Temperature;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * Use the {@link TemperatureFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TemperatureFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String TAG = TemperatureFragment.class.getSimpleName();

    private String mParam1;
    private GraphView temperatureGraph;
    private View rootView;
    private PointsGraphSeries<DataPoint> temperatureSeries;
    private SimpleDateFormat mDateFormatter;
    private final Handler mHandler = new Handler();
    private Runnable mTimer1;
    private Runnable mTimer2;

    private OnFragmentInteractionListener mListener;

    public TemperatureFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @return A new instance of fragment TemperatureFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static TemperatureFragment newInstance(String param1) {

        Log.i(TAG, "TemperatureFragment newInstance");

        TemperatureFragment fragment = new TemperatureFragment();
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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {




        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_temperature, container, false);
        temperatureGraph = (GraphView) rootView.findViewById(R.id.temperatureGraph);
        mDateFormatter = new SimpleDateFormat("MM-dd HH:mm:ss");

        temperatureGraph.setTitle("Current sensor\'s data");
        temperatureGraph.setTitleColor(R.color.colorPrimaryDark);
        temperatureGraph.getGridLabelRenderer().setVerticalAxisTitle("Temperature (C)");
        temperatureGraph.getGridLabelRenderer().setHorizontalAxisTitle("Time");

        // enabling horizontal zooming and scrolling
        temperatureGraph.getViewport().setScalable(true);

        temperatureGraph.getGridLabelRenderer().setLabelVerticalWidth(75);
        temperatureGraph.getGridLabelRenderer().setLabelHorizontalHeight(75);

        temperatureGraph.getViewport().setYAxisBoundsManual(true);
        temperatureGraph.getViewport().setMinY(0);
        temperatureGraph.getViewport().setMaxY(40);

        //TODO: make the date labels on the X axis to be shown properly
        // set date label formatter
        temperatureGraph.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(getActivity(), mDateFormatter));
        temperatureGraph.getGridLabelRenderer().setNumHorizontalLabels(2); // only 2 because of the space

        Calendar calendar = Calendar.getInstance();
        long t1 = calendar.getTimeInMillis();
        long t2 = calendar.getTimeInMillis() + 15000;

        temperatureGraph.getViewport().setXAxisBoundsManual(true);
        temperatureGraph.getViewport().setMinX((double)t1);
        temperatureGraph.getViewport().setMaxX(((double)t2));


        // as we use dates as labels, the human rounding to nice readable numbers
        // is not necessary
        temperatureGraph.getGridLabelRenderer().setHumanRounding(false);

        temperatureSeries = new PointsGraphSeries<>();
        temperatureGraph.addSeries(temperatureSeries);

        final SensorsDataViewModel sensorsDataViewModel =
                ViewModelProviders.of(getActivity()).get(SensorsDataViewModel.class);

        sensorsDataViewModel.getTemperatures().observe(getActivity(), new Observer<List<Temperature>>() {
            @Override
            public void onChanged(@Nullable final List<Temperature> temperatures) {
                if(temperatures == null) {

                } else if(temperatures.size() > 0) {

                    // if temperatures are present in the database, but the graph is empty, then
                    // most probably activity had been recreated and the graph should be populated with the data again
                    if(temperatureSeries.isEmpty() && temperatures.size() > 1) {

                        temperatureGraph.getViewport().setMinX((double)temperatures.get(0).getTimestamp());
                        temperatureGraph.getViewport().setMaxX((double)temperatureGraph.getViewport().getMinX(false) + 15000);

                        displayTemperatures(temperatures);

                    } else {

                        // if populating the graph with data for the first time
                        if (temperatures.size() == 1) {
                            // set manual x bounds to have nice steps
                            temperatureGraph.getViewport().setMinX((double) temperatures.get(0).getTimestamp());
                            temperatureGraph.getViewport().setMaxX((double) temperatureGraph.getViewport().getMinX(false) + 15000);

                            Log.d(TAG, "Initial timestamp, MinLabelX: " + mDateFormatter.format((double) temperatures.get(0).getTimestamp()));
                            Log.d(TAG, "Final timestamp, MaxLabelX: " + mDateFormatter.format(((double) temperatureGraph.getViewport().getMinX(false) + 15000)));
                        }

                        // appending each data point to the graph upon addition to the database
                        Log.i(TAG, "Temperature ID: " + temperatures.get(temperatures.size() - 1).getId() + " and timestamp: " + temperatures.get(temperatures.size() - 1).getTimestamp());
                        displayTemperature(temperatures.get(temperatures.size() - 1));
                    }
                }

            }

        });

        return rootView;
    }


    private void displayTemperatures(List<Temperature> temperatures) {

        mTimer2 = new Runnable()
        {
            @Override
            public void run() {
                try {
                    for (int i = 0; i < temperatures.size(); i++){
                        Log.i(TAG, "temperature timestamp: " + temperatures.get(i).getTimestamp() + " and id: " + temperatures.get(i).getId());
                        temperatureSeries.appendData(new DataPoint(temperatures.get(i).getTimestamp(), temperatures.get(i).getTemperatureValue()), false, 1000);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        mHandler.post(mTimer2);

    }


    private void displayTemperature(Temperature temperature) {

        /*if(isAdded()) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Log.d(TAG, "temperature ID: " + temperature.getId() + " and value: " + temperature.getTemperatureValue());
                        temperatureSeries.appendData(new DataPoint(temperature.getTimestamp(), temperature.getTemperatureValue()), false, 1000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }*/

        // changes made when the app was paused are shown only after the configChanges
        mTimer1 = new Runnable()
        {
            @Override
            public void run()
            {
                try {
                    Log.i(TAG, "temperature timestamp: " + temperature.getTimestamp() + " and id: " + temperature.getId());
                    temperatureSeries.appendData(new DataPoint(temperature.getTimestamp(), temperature.getTemperatureValue()), false, 1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        mHandler.post(mTimer1);

    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(String tag, Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(tag, uri);
        }
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }


    @Override
    public void onDetach() {
        super.onDetach();
        mHandler.removeCallbacks(mTimer1);
        mHandler.removeCallbacks(mTimer2);
        mListener = null;
    }

}

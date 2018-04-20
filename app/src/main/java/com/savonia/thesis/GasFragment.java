package com.savonia.thesis;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
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
import com.savonia.thesis.db.entity.Gas;
import com.savonia.thesis.viewmodels.SensorsDataViewModel;
import com.savonia.thesis.viewmodels.SharedViewModel;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * Use the {@link GasFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class GasFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private final static String TAG = GasFragment.class.getSimpleName();

    // TODO: Rename and change types of parameters
    private String mParam1;
    private GraphView gasGraph;
    private View rootView;
    private boolean isScrollToEndChecked = false;
    private PointsGraphSeries<DataPoint> gasSeries;
    private SimpleDateFormat mDateFormatter;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private Runnable mTimer1;
    private Runnable mTimer2;

    private OnFragmentInteractionListener mListener;

    public GasFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @return A new instance of fragment GasFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static GasFragment newInstance(String param1) {

        Log.i(TAG, "GasFragment newInstance");

        GasFragment fragment = new GasFragment();
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

        SharedViewModel sharedViewModel= ViewModelProviders.of(getActivity()).get(SharedViewModel.class);

        sharedViewModel.getScrollToEnd().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@NonNull Boolean scrollToEnd) {
                isScrollToEndChecked = scrollToEnd;
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_gas, container, false);
        gasGraph = (GraphView) rootView.findViewById(R.id.gasGraph);
        mDateFormatter = new SimpleDateFormat("HH:mm:ss", Locale.US);

        gasGraph.setTitle("Current sensor\'s data");
        gasGraph.setTitleColor(R.color.colorPrimaryDark);
        gasGraph.getGridLabelRenderer().setVerticalAxisTitle("CO2 (ppm)");
        gasGraph.getGridLabelRenderer().setHorizontalAxisTitle("Time");

        // enabling horizontal zooming and scrolling
        gasGraph.getViewport().setScrollable(true);

        gasGraph.getGridLabelRenderer().setLabelVerticalWidth(70);
        gasGraph.getGridLabelRenderer().setLabelHorizontalHeight(50);

        gasGraph.getViewport().setYAxisBoundsManual(true);
        gasGraph.getViewport().setMinY(0);
        gasGraph.getViewport().setMaxY(400);

        //TODO: make the date labels on the X axis to be shown properly
        // set date label formatter
        gasGraph.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(getActivity(), mDateFormatter));
        gasGraph.getGridLabelRenderer().setNumHorizontalLabels(4); // only 4 because of the space

        Calendar calendar = Calendar.getInstance();
        long t1 = calendar.getTimeInMillis();
        long t2 = calendar.getTimeInMillis() + 15000;

        gasGraph.getViewport().setXAxisBoundsManual(true);
        gasGraph.getViewport().setMinX((double)t1);
        gasGraph.getViewport().setMaxX(((double)t2));


        // as we use dates as labels, the human rounding to nice readable numbers
        // is not necessary
        gasGraph.getGridLabelRenderer().setHumanRounding(false);

        gasSeries = new PointsGraphSeries<>();
        gasGraph.addSeries(gasSeries);

        final SensorsDataViewModel sensorsDataViewModel =
                ViewModelProviders.of(getActivity()).get(SensorsDataViewModel.class);

        sensorsDataViewModel.getGases().observe(getActivity(), new Observer<List<Gas>>() {
            @Override
            public void onChanged(@Nullable final List<Gas> gases) {
                if(gases == null){

                } else if(gases.size() > 0) {

                    // if gas values are present in the database, but the graph is empty, then
                    // most probably activity had been recreated and the graph should be populated with the data again
                    if (gasSeries.isEmpty() && gases.size() > 1) {
                        Log.i(TAG, "RESETTING GAS GRAPH AFTER CONFIGURATION CHANGES");

                        // set manual x bounds to have nice steps
                        gasGraph.getViewport().setMinX((double) gases.get(0).getTimestamp());
                        gasGraph.getViewport().setMaxX(gasGraph.getViewport().getMinX(false) + 15000);

                        displayGases(gases);

                    } else {

                        // populating the graph with data for the first time
                        if (gases.size() == 1) {
                            // set manual x bounds to have nice steps
                            gasGraph.getViewport().setMinX((double) gases.get(0).getTimestamp());
                            gasGraph.getViewport().setMaxX(gasGraph.getViewport().getMinX(false) + 15000);

                            Log.d(TAG, "Initial timestamp, MinLabelX: " + mDateFormatter.format((double) gases.get(0).getTimestamp()));
                            Log.d(TAG, "Final timestamp, MaxLabelX: " + mDateFormatter.format((gasGraph.getViewport().getMinX(false) + 15000)));
                        }

                        Log.i(TAG, "Gas ID: " + gases.get(gases.size() - 1).getId() + " and timestamp: " + gases.get(gases.size() - 1).getTimestamp());
                        displayGas(gases.get(gases.size() - 1));
                    }
                }
            }
        });


        return rootView;
    }

    private void displayGases(List<Gas> gases) {

        mTimer2 = new Runnable()
        {
            @Override
            public void run() {
                try {
                    DataPoint[] values = new DataPoint[gases.size()];
                    for (int i = 0; i < gases.size(); i++){
                        Log.i(TAG, "gas timestamp: "+ gases.get(i).getTimestamp() + " and id: " + gases.get(i).getId());
                        DataPoint t = new DataPoint(gases.get(i).getTimestamp(), gases.get(i).getGasValue());
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


    private void displayGas(Gas gas) {

        mTimer1 = new Runnable()
        {
            @Override
            public void run()
            {
                try {
                    Log.i(TAG, "gas timestamp: " + gas.getTimestamp() + " and id: " + gas.getId());
                    gasSeries.appendData(new DataPoint(gas.getTimestamp(), gas.getGasValue()), isScrollToEndChecked, 1000);
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

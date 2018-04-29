package com.savonia.thesis;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.savonia.thesis.viewmodels.SharedViewModel;


public class ServicesFragment extends Fragment {
    // the fragment initialization parameters
    private static final String ARG_PARAM1 = "param1";
    private static final String TAG = ServicesFragment.class.getSimpleName();
    private final static String RECEIVED_SERVICES = "ServicesReceived";
    private final static String CONNECTION_STATE = "ConnectionState";

    private String mParam1;
    private boolean hasReceivedServices;
    private int connectionState;

    private OnFragmentInteractionListener mListener;
    private View rootView;
    private TextView connectionStatus;
    private ProgressBar progressCircle;
    private TextView retrievalStatus;
    private ExpandableListView expListView;

    public ServicesFragment() {
        // Required empty public constructor
    }


    public static ServicesFragment newInstance(String param1) {

        Log.i(TAG, "ServicesFragment newInstance");

        ServicesFragment fragment = new ServicesFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "ServicesFragment onCreate");

        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
        }
        setRetainInstance(true);

        SharedViewModel sharedViewModel= ViewModelProviders.of(getActivity()).get(SharedViewModel.class);

        sharedViewModel.getConnectionState().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(@NonNull Integer connectState) {
                connectionState = connectState;
                setConnectionState(connectionState, hasReceivedServices);
            }
        });
        sharedViewModel.getHasReceivedServices().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@NonNull Boolean servicesStatus) {
                hasReceivedServices = servicesStatus;
                setConnectionState(connectionState, hasReceivedServices);
            }
        });
    }


    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save some data
        savedInstanceState.putBoolean(RECEIVED_SERVICES, hasReceivedServices);
        savedInstanceState.putInt(CONNECTION_STATE, connectionState);

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Log.i(TAG, "ServicesFragment onCreateView");

        // Inflate the layout for this fragment
        rootView = (View) inflater.inflate(R.layout.fragment_services, container, false);

        connectionStatus = rootView.findViewById(R.id.connectionStatus);
        retrievalStatus = rootView.findViewById(R.id.retrievalStatus);
        progressCircle = rootView.findViewById(R.id.progressCircle);
        expListView = rootView.findViewById(R.id.expListView);

        // setting up the progressBar
        Drawable progressDrawable = progressCircle.getIndeterminateDrawable().mutate();
        progressDrawable.setColorFilter(getActivity().getColor(R.color.colorPrimary), PorterDuff.Mode.MULTIPLY);
        progressCircle.setProgressDrawable(progressDrawable);
        progressCircle.setVisibility(View.GONE);
        retrievalStatus.setVisibility(View.GONE);

        if(savedInstanceState != null) {
            this.hasReceivedServices = savedInstanceState.getBoolean(RECEIVED_SERVICES);
            this.connectionState = savedInstanceState.getInt(CONNECTION_STATE);
        }

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        setConnectionState(this.connectionState, this.hasReceivedServices);
    }


    // Is used to send data from fragment to its Host activity
    public void onRefreshButtonPressed(String tag, Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(tag, uri);
        }
    }


    public void setConnectionState(int connectionState, boolean hasReceivedServices) {
        this.connectionState = connectionState;
        this.hasReceivedServices = hasReceivedServices;
        switch(connectionState) {
            case 0:
                connectionStatus.setText(R.string.device_connected);
                if(!hasReceivedServices) {
                    progressCircle.setVisibility(View.VISIBLE);
                    retrievalStatus.setText(R.string.waiting_services);
                    retrievalStatus.setVisibility(View.VISIBLE);
                } else {
                    progressCircle.setVisibility(View.GONE);
                    retrievalStatus.setVisibility(View.GONE);
                }
                break;
            case 1:
                connectionStatus.setText(R.string.device_disconnected);
                progressCircle.setVisibility(View.VISIBLE);
                retrievalStatus.setText(R.string.connection_lost);
                retrievalStatus.setVisibility(View.VISIBLE);
                break;
            case 2:
                connectionStatus.setText(R.string.device_connected);
                progressCircle.setVisibility(View.VISIBLE);
                retrievalStatus.setText(R.string.waiting_notified_characteristic);
                retrievalStatus.setVisibility(View.VISIBLE);
                break;
            case 3:
                connectionStatus.setText(R.string.device_connected);
                progressCircle.setVisibility(View.GONE);
                retrievalStatus.setVisibility(View.GONE);
                break;
            default:
                break;

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
        mListener = null;
    }

}

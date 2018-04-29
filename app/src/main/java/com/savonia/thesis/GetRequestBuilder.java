package com.savonia.thesis;


import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.savonia.thesis.viewmodels.SaMiViewModel;


public class GetRequestBuilder extends Fragment {

    // TODO: modify layout and compare tags to assign values to the graph appropriately (save them on button click and then compare received tags with the saved ones)

    private static final String ARG_PARAM1 = "param1";
    private static final String TAG = GetRequestBuilder.class.getSimpleName();
    private String mParam1;
    private Button getButton;
    private EditText measurementNameEdTxt;
    private EditText measurementTagEdTxt;
    private EditText temperatureTagEdTxt;
    private EditText gasTagEdTxt;
    private EditText takeAmountEdTxt;
    private EditText keyEdTxt;


    private View rootView;


    public GetRequestBuilder() {
        // Required empty public constructor
    }

    public static GetRequestBuilder newInstance(String param1) {

        Log.i(TAG, "GetRequestBuilder newInstance");

        GetRequestBuilder fragment = new GetRequestBuilder();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "GetRequestBuilder onCreate");
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
        }
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_get_request_builder, container, false);
        getButton = (Button) rootView.findViewById(R.id.getButton);

        measurementNameEdTxt = (EditText) rootView.findViewById(R.id.measurementName);
        measurementTagEdTxt = (EditText) rootView.findViewById(R.id.measurementTag);
        temperatureTagEdTxt = (EditText) rootView.findViewById(R.id.temperatureTag);
        takeAmountEdTxt = (EditText) rootView.findViewById(R.id.takeAmount);
        keyEdTxt = (EditText) rootView.findViewById(R.id.key);
        gasTagEdTxt = (EditText) rootView.findViewById(R.id.gasTag);

        final SaMiViewModel saMiViewModel = ViewModelProviders.of(GetRequestBuilder.this).get(SaMiViewModel.class);

        getButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: create proper get request from the edit texts
                saMiViewModel.makeGetRequest();
            }
        });
        return rootView;
    }

}

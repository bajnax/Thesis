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

import com.savonia.thesis.viewmodels.GetRequestViewModel;
import com.savonia.thesis.viewmodels.SaMiViewModel;


public class GetRequestBuilder extends Fragment {

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

    private GetRequestViewModel getRequestViewModel;

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
        getRequestViewModel = ViewModelProviders.of(getActivity()).get(GetRequestViewModel.class);
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

        final SaMiViewModel saMiViewModel = ViewModelProviders.of(getActivity()).get(SaMiViewModel.class);

        getButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {

                    Log.d(TAG, "Generating the GET request!");

                    String key = "";
                    String measurementName = "";
                    String measurementTag = "";
                    Integer takeAmount = 0;

                    if (keyEdTxt.getText().toString().trim().length() > 0) {
                        key = keyEdTxt.getText().toString().trim();
                        getRequestViewModel.setKey(keyEdTxt.getText().toString().trim());
                    }
                    else {
                        key = getResources().getString(R.string.key_password);
                        getRequestViewModel.setKey(getResources().getString(R.string.key_password));
                    }

                    if (measurementNameEdTxt.getText().toString().trim().length() > 0) {
                        measurementName = measurementNameEdTxt.getText().toString().trim();
                        getRequestViewModel.setMeasurementName(measurementNameEdTxt.getText().toString().trim());
                    }

                    if (measurementTagEdTxt.getText().toString().trim().length() > 0) {
                        measurementTag = measurementTagEdTxt.getText().toString().trim();
                        getRequestViewModel.setMeasurementTag(measurementTagEdTxt.getText().toString().trim());
                    }

                    if (takeAmountEdTxt.getText().toString().trim().length() > 0) {
                        takeAmount = Integer.parseInt(takeAmountEdTxt.getText().toString().trim());
                        getRequestViewModel.setTakeAmount(Integer.parseInt(takeAmountEdTxt.getText().toString().trim()));
                    }

                    String dataTags = "";
                    StringBuilder sb = new StringBuilder();

                    if (temperatureTagEdTxt.getText().toString().trim().length() > 0) {
                        sb.append(temperatureTagEdTxt.getText().toString().trim());
                        getRequestViewModel.setTemperatureTag(temperatureTagEdTxt.getText().toString().trim());
                    }

                    if (gasTagEdTxt.getText().toString().trim().length() > 0) {
                        if(sb.length() > 0)
                            sb.append(";" + gasTagEdTxt.getText().toString().trim());
                        else
                            sb.append(gasTagEdTxt.getText().toString().trim());

                        getRequestViewModel.setGasTag(gasTagEdTxt.getText().toString().trim());
                    }

                    Log.d(TAG, "String Builder: " + sb.toString());
                    dataTags = sb.toString();

                    // TODO: create proper get request from the edit texts (temperature and gas tags and a key must be always present!)
                    saMiViewModel.makeGetRequest(key.isEmpty() ? null : key, measurementName.isEmpty() ? null : measurementName,
                            measurementTag.isEmpty() ? null : measurementTag, takeAmount == 0 ? null : takeAmount,
                            dataTags.isEmpty() ? null : dataTags);

                } catch(Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        return rootView;
    }

}

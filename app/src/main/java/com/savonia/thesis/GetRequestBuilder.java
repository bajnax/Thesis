package com.savonia.thesis;


import android.app.Activity;
import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.savonia.thesis.viewmodels.GetRequestViewModel;
import com.savonia.thesis.viewmodels.SaMiViewModel;

import org.w3c.dom.Text;


public class GetRequestBuilder extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String TAG = GetRequestBuilder.class.getSimpleName();
    private String mParam1;
    private Button getButton;
    private TextInputLayout inputLayoutKey;
    private TextInputEditText keyEdTxt;
    private TextInputEditText measurementNameEdTxt;
    private TextInputEditText measurementTagEdTxt;
    private TextInputLayout inputLayoutTemperatureTag;
    private TextInputEditText temperatureTagEdTxt;
    private TextInputLayout inputLayoutGasTag;
    private TextInputEditText gasTagEdTxt;
    private TextInputEditText fromDate;
    private TextInputEditText toDate;
    private TextInputEditText takeAmountEdTxt;


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

        inputLayoutKey =(TextInputLayout) rootView.findViewById(R.id.input_layout_key);
        keyEdTxt = (TextInputEditText) rootView.findViewById(R.id.key);
        measurementNameEdTxt = (TextInputEditText) rootView.findViewById(R.id.measurementName);
        measurementTagEdTxt = (TextInputEditText) rootView.findViewById(R.id.measurementTag);
        fromDate = (TextInputEditText) rootView.findViewById(R.id.fromDate);
        toDate = (TextInputEditText) rootView.findViewById(R.id.toDate);
        takeAmountEdTxt = (TextInputEditText) rootView.findViewById(R.id.takeAmount);
        inputLayoutTemperatureTag =(TextInputLayout) rootView.findViewById(R.id.input_layout_temperature_tag);
        temperatureTagEdTxt = (TextInputEditText) rootView.findViewById(R.id.temperatureTag);
        inputLayoutGasTag =(TextInputLayout) rootView.findViewById(R.id.input_layout_gas_tag);
        gasTagEdTxt = (TextInputEditText) rootView.findViewById(R.id.gasTag);

        keyEdTxt.addTextChangedListener(new MyTextWatcher(keyEdTxt));
        temperatureTagEdTxt.addTextChangedListener(new MyTextWatcher(temperatureTagEdTxt));
        gasTagEdTxt.addTextChangedListener(new MyTextWatcher(gasTagEdTxt));

        final SaMiViewModel saMiViewModel = ViewModelProviders.of(getActivity()).get(SaMiViewModel.class);

        getButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {

                    Log.d(TAG, "Generating the GET request!");

                    String key = "";
                    String measurementName = "";
                    String measurementTag = "";
                    String fromDateString = "";
                    String toDateString = "";
                    Integer takeAmount = 0;

                    if (keyEdTxt.getText().toString().trim().length() > 0) {
                        key = keyEdTxt.getText().toString().trim();
                    }
                    else {
                        key = getResources().getString(R.string.key_password);
                    }

                    getRequestViewModel.setKey(key);

                    if (measurementNameEdTxt.getText().toString().trim().length() > 0) {
                        measurementName = measurementNameEdTxt.getText().toString().trim();
                        getRequestViewModel.setMeasurementName(measurementNameEdTxt.getText().toString().trim());
                    }

                    if (measurementTagEdTxt.getText().toString().trim().length() > 0) {
                        measurementTag = measurementTagEdTxt.getText().toString().trim();
                        getRequestViewModel.setMeasurementTag(measurementTagEdTxt.getText().toString().trim());
                    }

                    if(fromDate.getText().toString().trim().length() >= 10) {
                        fromDateString = fromDate.getText().toString().trim();
                    }

                    if(toDate.getText().toString().trim().length() >= 10) {
                        toDateString = toDate.getText().toString().trim();
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

                    if(submitForm()) {

                        Toast.makeText(getActivity(), "Retrieving data",
                                Toast.LENGTH_SHORT).show();

                        hideSoftKeyboard(getActivity());

                        saMiViewModel.makeGetRequest(key.isEmpty() ? null : key, measurementName.isEmpty() ? null : measurementName,
                                measurementTag.isEmpty() ? null : measurementTag, fromDateString.isEmpty() ? null : fromDateString,
                                toDateString.isEmpty() ? null : toDateString, takeAmount == 0 ? null : takeAmount,
                                dataTags.isEmpty() ? null : dataTags);
                    }

                } catch(Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        return rootView;
    }

    private void requestFocus(View view) {
        if (view.requestFocus()) {
            getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
    }

    private Boolean submitForm() {
        if (!validateKey()) {
            return false;
        }

        if (!validateTemperatureTag()) {
            return false;
        }

        if (!validateGasTag()) {
            return false;
        }
        return true;
    }

    private boolean validateKey() {
        if (keyEdTxt.getText().toString().trim().isEmpty()) {
            inputLayoutKey.setError(getResources().getString(R.string.key_error));
            requestFocus(keyEdTxt);
            return false;
        } else {
            inputLayoutKey.setErrorEnabled(false);
        }

        return true;
    }

    private boolean validateTemperatureTag() {
        if (temperatureTagEdTxt.getText().toString().trim().isEmpty()) {
            inputLayoutTemperatureTag.setError(getResources().getString(R.string.temperature_tag_error));
            requestFocus(temperatureTagEdTxt);
            return false;
        } else {
            inputLayoutTemperatureTag.setErrorEnabled(false);
        }

        return true;
    }

    private boolean validateGasTag() {
        if (gasTagEdTxt.getText().toString().trim().isEmpty()) {
            inputLayoutGasTag.setError(getResources().getString(R.string.gas_tag_error));
            requestFocus(gasTagEdTxt);
            return false;
        } else {
            inputLayoutGasTag.setErrorEnabled(false);
        }

        return true;
    }

    private class MyTextWatcher implements TextWatcher {

        private View view;

        private MyTextWatcher(View view) {
            this.view = view;
        }

        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        public void afterTextChanged(Editable editable) {
            switch (view.getId()) {
                case R.id.key:
                    validateKey();
                    break;
                case R.id.temperatureTag:
                    validateTemperatureTag();
                    break;
                case R.id.gasTag:
                    validateGasTag();
                    break;
            }
        }
    }

    private void hideSoftKeyboard(Activity activity) {
        try {
            InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
        } catch (NullPointerException ex) {
            ex.printStackTrace();
        }
    }

}

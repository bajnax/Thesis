package com.savonia.thesis;

import android.app.Activity;
import android.arch.lifecycle.ViewModelProviders;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.savonia.thesis.db.entity.Gas;
import com.savonia.thesis.db.entity.Temperature;
import com.savonia.thesis.viewmodels.SaMiViewModel;
import com.savonia.thesis.webclient.measuremetsmodels.DataModel;
import com.savonia.thesis.webclient.measuremetsmodels.MeasurementsModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

public class PostRequestActivity extends AppCompatActivity {

    private static final String TAG = PostRequestActivity.class.getSimpleName();

    private SaMiViewModel saMiViewModel;
    private List<Temperature> temperatureList;
    private List<Gas> gasList;
    private String measurementTag = "";
    private String measurementName = "";
    private MeasurementsModel responseMeasurement;
    private ArrayList<MeasurementsModel> initialMeasurements;
    private TextInputLayout inputLayoutMeasurementName;
    private TextInputEditText measurementNameEdTxt;
    private TextInputLayout inputLayoutMeasurementTag;
    private TextInputEditText measurementTagEdTxt;
    private TextInputLayout inputLayoutTemperatureTag;
    private TextInputEditText temperatureTagEdTxt;
    private TextInputLayout inputLayoutGasTag;
    private TextInputEditText gasTagEdTxt;
    private SimpleDateFormat simpleDateFormat;
    private Button sendButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_response);
        inputLayoutMeasurementName = (TextInputLayout) findViewById(R.id.input_layout_measurementName);
        measurementNameEdTxt = (TextInputEditText) findViewById(R.id.measurementName);
        inputLayoutMeasurementTag = (TextInputLayout) findViewById(R.id.input_layout_measurementTag);
        measurementTagEdTxt = (TextInputEditText) findViewById(R.id.measurementTag);
        inputLayoutTemperatureTag = (TextInputLayout) findViewById(R.id.input_layout_temperature_tag);
        temperatureTagEdTxt = (TextInputEditText) findViewById(R.id.temperatureTag);
        inputLayoutGasTag = (TextInputLayout) findViewById(R.id.input_layout_gas_tag);
        gasTagEdTxt = (TextInputEditText) findViewById(R.id.gasTag);
        sendButton = (Button) findViewById(R.id.postButton);

        measurementNameEdTxt.addTextChangedListener(new MyTextWatcher(measurementNameEdTxt));
        measurementTagEdTxt.addTextChangedListener(new MyTextWatcher(measurementTagEdTxt));
        temperatureTagEdTxt.addTextChangedListener(new MyTextWatcher(temperatureTagEdTxt));
        gasTagEdTxt.addTextChangedListener(new MyTextWatcher(gasTagEdTxt));

        simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH);


        saMiViewModel = ViewModelProviders.of(PostRequestActivity.this).get(SaMiViewModel.class);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(submitForm()) {

                    Toast.makeText(PostRequestActivity.this, "Sending data",
                            Toast.LENGTH_SHORT).show();

                    hideSoftKeyboard();

                    initialMeasurements = null;
                    initialMeasurements = new ArrayList<MeasurementsModel>();

                    if (measurementNameEdTxt.getText().toString().trim().length() > 0)
                        measurementName = measurementNameEdTxt.getText().toString().trim();
                    else
                        measurementName = getResources().getString(R.string.measurement_name);

                    if (measurementTagEdTxt.getText().toString().trim().length() > 0)
                        measurementTag = measurementTagEdTxt.getText().toString().trim();
                    else
                        measurementTag = getResources().getString(R.string.measurement_tag);


                    saMiViewModel.generateTemperatureMeasurementAsync();
                }
            }
        });

        // retrieving the list of temperatures from the database
        saMiViewModel.getTemperatureMeasurementAsync().observe(PostRequestActivity.this, temperatures -> {

            try {
                if(temperatures != null) {
                    temperatureList = temperatures;
                    saMiViewModel.generateGasMeasurementAsync();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });


        // retrieving the list of gases from the database and sending the measurement
        saMiViewModel.getGasMeasurementAsync().observe(PostRequestActivity.this, gases -> {

            try {
                if(gases != null) {
                    gasList = gases;
                    buildRequest();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });


        // result of the POST request
        saMiViewModel.getResponseStatus().observe(PostRequestActivity.this, response -> {
            if(response != 0) {
                if(response == 1) {
                    Toast.makeText(PostRequestActivity.this, "The data has been successfully saved in the SaMi cloud!",
                            Toast.LENGTH_LONG).show();
                    // closing activity
                    PostRequestActivity.this.finish();
                    // TODO: clear the database here
                    saMiViewModel.clearDatabase();

                } else {
                    Toast.makeText(PostRequestActivity.this, "Error occurred while sending the data. " +
                                    "Check your internet connection, availability of data in the DB or contact Mikko Paakkonen!",
                            Toast.LENGTH_LONG).show();
                }
            }
        });

    }

    public void buildRequest() {
        // TEMPERATURES
        if(temperatureList.size() > 0) {

            Log.d(TAG, "GENERATING TEMPERATURE MEASUREMENT");

            String tag = "";
            if(temperatureTagEdTxt.getText().toString().trim().length() > 0)
                tag = temperatureTagEdTxt.getText().toString().trim();
            else
                tag = getResources().getString(R.string.temperature_tag);


            // generating a measurement from the retrieved list of temperatures
            for (int i = 0; i < temperatureList.size(); i++) {
                MeasurementsModel mM = new MeasurementsModel();
                ArrayList<DataModel> dMList = new ArrayList<DataModel>();
                DataModel dM = new DataModel();

                // preparing MeasurementModel
                mM.setTag(measurementTag);
                mM.setObject(measurementName);
                mM.setTimestampISO8601(simpleDateFormat.format(temperatureList.get(i).getDate()));

                // preaparing DataModel
                Log.d(TAG, "Temperature value: " + temperatureList.get(i).getTemperatureValue());
                Log.d(TAG, "Temperature time: " + simpleDateFormat.format(temperatureList.get(i).getDate()));
                dM.setTag(tag);
                dM.setValue(temperatureList.get(i).getTemperatureValue());
                // adding DataModel to the list of DataModels
                dMList.add(dM);
                // adding a list of DataModels to MeasurementModel
                mM.setData(dMList);
                // adding MeasurementModel to the list of MeasurementModels
                initialMeasurements.add(mM);
            }

        }


        // GASES
        if(gasList.size() > 0) {
            Log.d(TAG, "GENERATING GAS MEASUREMENT");

            String tag = "";
            if(gasTagEdTxt.getText().toString().trim().length() > 0)
                tag = gasTagEdTxt.getText().toString().trim();
            else
                tag = getResources().getString(R.string.gas_tag);

            // adding the retrieved list of gases to the measurement
            if(initialMeasurements.size() > gasList.size()) {
                for (int i = 0; i < gasList.size(); i++) {
                    DataModel dM = new DataModel();

                    Log.d(TAG, "Gas value: " + gasList.get(i).getGasValue());
                    dM.setTag(tag);
                    dM.setValue(gasList.get(i).getGasValue());
                    initialMeasurements.get(i).getData().add(dM);
                }
            } else {
                for (int i = 0; i < gasList.size(); i++) {
                    DataModel dM = new DataModel();

                    if(i < initialMeasurements.size()) {
                        Log.d(TAG, "Gas value: " + gasList.get(i).getGasValue());
                        dM.setTag(tag);
                        dM.setValue(gasList.get(i).getGasValue());
                        initialMeasurements.get(i).getData().add(dM);
                    } else {
                        MeasurementsModel mM = new MeasurementsModel();
                        ArrayList<DataModel> dMList = new ArrayList<DataModel>();

                        // preparing MeasurementModel
                        mM.setTag(measurementTag);
                        mM.setObject(measurementName);
                        mM.setTimestampISO8601(simpleDateFormat.format(gasList.get(i).getDate()));

                        // preparing DataModel
                        Log.d(TAG, "Gas value: " + gasList.get(i).getGasValue());
                        dM.setTag(tag);
                        dM.setValue(gasList.get(i).getGasValue());

                        // adding DataModel to the list of DataModels
                        dMList.add(dM);

                        // adding the list of DataModels to the MeasurementModel
                        mM.setData(dMList);
                        // addig MeasurementModel to the list of MeasurementModels
                        initialMeasurements.add(mM);
                    }
                }
            }
        }

        // sending generated measurement to the SaMi cloud via POST request
        Log.d(TAG, "SENDING GENERATED TEMPERATURE AND/OR GAS MEASUREMENT VIA POST REQUEST");
        saMiViewModel.makePostRequest("savoniatest", initialMeasurements);
    }


    private void requestFocus(View view) {
        if (view.requestFocus()) {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
    }

    private Boolean submitForm() {
        if (!validateMeasurementName()) {
            return false;
        }

        if (!validateMeasurementTag()) {
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

    private boolean validateMeasurementName() {
        if (measurementNameEdTxt.getText().toString().trim().isEmpty()) {
            inputLayoutMeasurementName.setError(getResources().getString(R.string.measurement_name_error));
            requestFocus(measurementNameEdTxt);
            return false;
        } else {
            inputLayoutMeasurementName.setErrorEnabled(false);
        }

        return true;
    }

    private boolean validateMeasurementTag() {
        if (measurementTagEdTxt.getText().toString().trim().isEmpty()) {
            inputLayoutMeasurementTag.setError(getResources().getString(R.string.measurement_tag_error));
            requestFocus(measurementTagEdTxt);
            return false;
        } else {
            inputLayoutMeasurementTag.setErrorEnabled(false);
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
                case R.id.measurementName:
                    validateMeasurementName();
                    break;
                case R.id.measurementTag:
                    validateMeasurementTag();
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

    private void hideSoftKeyboard() {
        try {
            InputMethodManager inputMethodManager = (InputMethodManager) PostRequestActivity.this.getSystemService(Activity.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(PostRequestActivity.this.getCurrentFocus().getWindowToken(), 0);
        } catch (NullPointerException ex) {
            ex.printStackTrace();
        }
    }

}

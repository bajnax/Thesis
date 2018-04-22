package com.savonia.thesis.viewmodels;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;
import com.savonia.thesis.db.SensorsValuesDatabase;
import com.savonia.thesis.repository.CentralRepository;
import com.savonia.thesis.webclient.measuremetsmodels.MeasurementsModel;
import java.util.List;

public class SaMiViewModel extends AndroidViewModel {

    private LiveData<List<MeasurementsModel>> measurementsList;
    private CentralRepository mRepository;
    private LiveData<MeasurementsModel> measurement;


    public SaMiViewModel(@NonNull Application application) {
        super(application);
        mRepository = CentralRepository.
                getInstance(SensorsValuesDatabase.getDatabase(application));
        measurementsList = mRepository.getWebMeasurements();
        measurement = mRepository.getPostResponse();
    }

    // TODO: make get requests for specific conditions
    /*
    public void initGetCall(Date initialDate, Date lastDate) {
        measurementsList = mRepository.getWebMeasurements(initialDate, lastDate);
    }*/



    public void makeGetRequest() {
        mRepository.makeGetRequest();
    }

    public LiveData<List<MeasurementsModel>> getMeasurements() {
        return measurementsList;
    }

    public void makePostRequest(String key, MeasurementsModel postedMeasurement) {
        mRepository.makePostRequest(key, postedMeasurement);
    }

    public LiveData<MeasurementsModel> getPostResponseMeasurement() {
        return measurement;
    }
}

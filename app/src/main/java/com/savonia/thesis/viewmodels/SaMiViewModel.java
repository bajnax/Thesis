package com.savonia.thesis.viewmodels;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;
import com.savonia.thesis.db.SensorsValuesDatabase;
import com.savonia.thesis.db.entity.Gas;
import com.savonia.thesis.db.entity.Temperature;
import com.savonia.thesis.repository.CentralRepository;
import com.savonia.thesis.webclient.measuremetsmodels.MeasurementsModel;
import java.util.List;

public class SaMiViewModel extends AndroidViewModel {

    private LiveData<List<MeasurementsModel>> measurementsList;
    private CentralRepository mRepository;
    private LiveData<List<MeasurementsModel>> measurement;
    private LiveData<List<Temperature>> temperatureListAsync;
    private LiveData<List<Gas>> gasListAsync;
    private LiveData<Integer> responseStatus;


    public SaMiViewModel(@NonNull Application application) {
        super(application);
        mRepository = CentralRepository.
                getInstance(SensorsValuesDatabase.getDatabase(application));
        measurementsList = mRepository.getWebMeasurements();
        measurement = mRepository.getPostResponse();
        temperatureListAsync = mRepository.getTemperatureMeasurementAsync();
        gasListAsync = mRepository.getGasMeasurementAsync();
        responseStatus = mRepository.getResponseStatus();
    }


    // GET request
    public void makeGetRequest(String key, String measName, String measTag, Integer takeAmount, String dataTags) {
        mRepository.makeGetRequest(key, measName, measTag, takeAmount, dataTags);
    }

    public LiveData<List<MeasurementsModel>> getMeasurements() {
        return measurementsList;
    }


    // POST request
    public void generateTemperatureMeasurementAsync() {
        mRepository.generateTemperatureMeasurementAsync();
    }

    public LiveData<List<Temperature>> getTemperatureMeasurementAsync() {
        return temperatureListAsync;
    }

    public void generateGasMeasurementAsync() {
        mRepository.generateGasMeasurementAsync();
    }

    public LiveData<List<Gas>> getGasMeasurementAsync() {
        return gasListAsync;
    }

    public void makePostRequest(String key, MeasurementsModel postedMeasurement) {
        mRepository.makePostRequest(key, postedMeasurement);
    }

    public LiveData<List<MeasurementsModel>> getPostResponseMeasurement() {
        return measurement;
    }

    public LiveData<Integer> getResponseStatus() {
        return responseStatus;
    }
}

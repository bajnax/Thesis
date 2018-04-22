package com.savonia.thesis.repository;


import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;
import android.arch.lifecycle.MutableLiveData;

import com.savonia.thesis.db.SensorsValuesDatabase;
import com.savonia.thesis.db.entity.Gas;
import com.savonia.thesis.db.entity.Temperature;
import com.savonia.thesis.webclient.WebClient;
import com.savonia.thesis.webclient.measuremetsmodels.MeasurementsModel;
import com.savonia.thesis.webclient.measuremetsmodels.SaMiClient;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CentralRepository {
    private static CentralRepository sInstance;

    private final SensorsValuesDatabase mDatabase;
    private final SaMiClient saMiClient;
    private MediatorLiveData<List<Temperature>> mObservableTemperatures;
    private MediatorLiveData<List<Gas>> mObservableGases;
    private MutableLiveData<List<MeasurementsModel>> mObservableMeasurementsModel;
    private MutableLiveData<MeasurementsModel> mObservablePostResponse;

    private CentralRepository(final SensorsValuesDatabase database) {
        mDatabase = database;
        mObservableTemperatures = new MediatorLiveData<>();
        mObservableGases = new MediatorLiveData<>();
        mObservableMeasurementsModel = new MutableLiveData<>();
        mObservablePostResponse = new MutableLiveData<>();
        saMiClient = WebClient.getWebClient();


        mObservableTemperatures.addSource(mDatabase.getTemperatureDao().getAllTemperatureValues(),
                temperatureEntities -> {
                    if (mDatabase.getDatabaseCreated() != null) {
                        mObservableTemperatures.postValue(temperatureEntities);
                    }
                });

        mObservableGases.addSource(mDatabase.getGasDao().getAllGasValues(),
                gasEntities -> {
                    if (mDatabase.getDatabaseCreated() != null) {
                        mObservableGases.postValue(gasEntities);
                    }
                });

    }

    public static CentralRepository getInstance(final SensorsValuesDatabase database) {
        if (sInstance == null) {
            synchronized (CentralRepository.class) {
                if (sInstance == null) {
                    sInstance = new CentralRepository(database);
                }
            }
        }
        return sInstance;
    }


    /**
     * Get the list of temperatures from the database and get notified when the data changes.
     */
    public LiveData<List<Temperature>> getTemperatures() {
        return mObservableTemperatures;
    }


    public void insertTemperature(Temperature temperature) {
        mDatabase.getTemperatureDao().insert(temperature);
    }


    /**
     * Get the list of gas values from the database and get notified when the data changes.
     */
    public LiveData<List<Gas>> getGases() {
        return mObservableGases;
    }


    public void insertGas(Gas gas) {
        mDatabase.getGasDao().insert(gas);
    }


    public LiveData<List<MeasurementsModel>> getWebMeasurements() {
        return mObservableMeasurementsModel;
    }


    public void makeGetRequest() {

        saMiClient.getMeasurements().enqueue(new Callback<List<MeasurementsModel>>() {
            @Override
            public void onResponse(Call<List<MeasurementsModel>> call, Response<List<MeasurementsModel>> response) {
                if(response.isSuccessful())
                    mObservableMeasurementsModel.postValue(response.body());
            }

            @Override
            public void onFailure(Call<List<MeasurementsModel>> call, Throwable throwable) {
                System.out.println(throwable);
            }
        });
    }


    public void makePostRequest(String key, MeasurementsModel measurement) {

        saMiClient.postMeasurements(key, measurement).enqueue(new Callback<MeasurementsModel>() {
            @Override
            public void onResponse(Call<MeasurementsModel> call, Response<MeasurementsModel> response) {
                if(response.isSuccessful()) {
                    mObservablePostResponse.postValue(response.body());
                }
            }

            @Override
            public void onFailure(Call<MeasurementsModel> call, Throwable throwable) {
                System.out.println(throwable);
            }
        });
    }

    public LiveData<MeasurementsModel> getPostResponse() {
        return mObservablePostResponse;
    }


}

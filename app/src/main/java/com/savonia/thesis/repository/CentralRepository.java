package com.savonia.thesis.repository;


import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;

import com.savonia.thesis.db.SensorsValuesDatabase;
import com.savonia.thesis.db.entity.Gas;
import com.savonia.thesis.db.entity.Temperature;
import com.savonia.thesis.webclient.measuremetsmodels.MeasurementsModel;

import java.util.List;

public class CentralRepository {
    private static CentralRepository sInstance;

    private final SensorsValuesDatabase mDatabase;
    private MediatorLiveData<List<Temperature>> mObservableTemperatures;
    private MediatorLiveData<List<Gas>> mObservableGases;
    private MediatorLiveData<List<MeasurementsModel>> mObservableMeasurementsModel;

    private CentralRepository(final SensorsValuesDatabase database) {
        mDatabase = database;
        mObservableTemperatures = new MediatorLiveData<>();
        mObservableGases = new MediatorLiveData<>();


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


}

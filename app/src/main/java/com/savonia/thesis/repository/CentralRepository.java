package com.savonia.thesis.repository;


import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;

import com.savonia.thesis.db.SensorsValuesDatabase;
import com.savonia.thesis.db.entity.Temperature;

import java.util.List;

public class CentralRepository {
    private static CentralRepository sInstance;

    private final SensorsValuesDatabase mDatabase;
    private MediatorLiveData<List<Temperature>> mObservableTemperatures;

    private CentralRepository(final SensorsValuesDatabase database) {
        mDatabase = database;
        mObservableTemperatures = new MediatorLiveData<>();

        mObservableTemperatures.addSource(mDatabase.getTemperatureDao().getAllTemperatureValues(),
                temperatureEntities -> {
                    if (mDatabase.getDatabaseCreated() != null) {
                        mObservableTemperatures.postValue(temperatureEntities);
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
    public LiveData<List<Temperature>> getProducts() {
        return mObservableTemperatures;
    }

}

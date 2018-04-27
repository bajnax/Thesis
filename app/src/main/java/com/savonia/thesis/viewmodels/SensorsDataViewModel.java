package com.savonia.thesis.viewmodels;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;
import android.support.annotation.NonNull;

import com.savonia.thesis.db.SensorsValuesDatabase;
import com.savonia.thesis.db.entity.Gas;
import com.savonia.thesis.db.entity.Temperature;
import com.savonia.thesis.repository.CentralRepository;

import java.util.List;

public class SensorsDataViewModel extends AndroidViewModel {
    // MediatorLiveData can observe other LiveData objects and react on their emissions.
    private final MediatorLiveData<List<Temperature>> mObservableTemperatures;
    private final MediatorLiveData<List<Gas>> mObservableGases;

    private CentralRepository mRepository;

    public SensorsDataViewModel(@NonNull Application application) {
        super(application);

        mObservableTemperatures = new MediatorLiveData<>();
        // set by default null, until we get data from the database.
        mObservableTemperatures.setValue(null);

        mObservableGases = new MediatorLiveData<>();
        mObservableGases.setValue(null);

        mRepository = CentralRepository.
                getInstance(SensorsValuesDatabase.getDatabase(application));

        LiveData<List<Temperature>> temperaturesList = mRepository.getTemperatures();

        // observe the changes of the temperatures from the database and forward them
        mObservableTemperatures.addSource(temperaturesList, mObservableTemperatures::setValue);

        LiveData<List<Gas>> gasesList = mRepository.getGases();
        // observe the changes of the gases from the database and forward them
        mObservableGases.addSource(gasesList, mObservableGases::setValue);

    }

    /**
     * Expose the LiveData Temperatures query so the UI can observe it.
     */
    public LiveData<List<Temperature>> getTemperatures() {
        return mObservableTemperatures;
    }

    public void insertTemperature(Temperature temperature) {
        mRepository.insertTemperature(temperature);
    }

    /**
     * Expose the LiveData Gases query so the UI can observe it.
     */
    public LiveData<List<Gas>> getGases() {
        return mObservableGases;
    }

    public void insertGas(Gas gas) {
        mRepository.insertGas(gas);
    }

}

package com.savonia.thesis;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;
import android.support.annotation.NonNull;

import com.savonia.thesis.db.SensorsValuesDatabase;
import com.savonia.thesis.db.entity.Temperature;
import com.savonia.thesis.repository.CentralRepository;

import java.util.List;

public class SensorsDataViewModel extends AndroidViewModel {
    // MediatorLiveData can observe other LiveData objects and react on their emissions.
    private final MediatorLiveData<List<Temperature>> mObservableTemperatures;
    private CentralRepository mRepository;

    public SensorsDataViewModel(Application application) {
        super(application);

        mObservableTemperatures = new MediatorLiveData<>();
        // set by default null, until we get data from the database.
        mObservableTemperatures.setValue(null);

        mRepository = CentralRepository.
                getInstance(SensorsValuesDatabase.getDatabase(application));

        LiveData<List<Temperature>> products = mRepository.getTemperatures();

        /*LiveData<List<Temperature>> products = CentralRepository.
                getInstance(SensorsValuesDatabase.getDatabase(application)).getTemperatures();*/

        // observe the changes of the products from the database and forward them
        mObservableTemperatures.addSource(products, mObservableTemperatures::setValue);
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
}

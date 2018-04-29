package com.savonia.thesis.repository;


import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;
import android.arch.lifecycle.MutableLiveData;
import android.os.AsyncTask;
import android.util.Log;

import com.savonia.thesis.db.SensorsValuesDatabase;
import com.savonia.thesis.db.dao.GasDao;
import com.savonia.thesis.db.dao.TemperatureDao;
import com.savonia.thesis.db.entity.Gas;
import com.savonia.thesis.db.entity.Temperature;
import com.savonia.thesis.webclient.WebClient;
import com.savonia.thesis.webclient.measuremetsmodels.MeasurementsModel;
import com.savonia.thesis.webclient.measuremetsmodels.MeasurementsPackage;
import com.savonia.thesis.webclient.measuremetsmodels.SaMiClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CentralRepository {
    private static CentralRepository sInstance;
    private final static String TAG = CentralRepository.class.getSimpleName();

    private final SensorsValuesDatabase mDatabase;
    private final SaMiClient saMiClient;
    private MediatorLiveData<List<Temperature>> mObservableTemperatures;
    private MediatorLiveData<List<Gas>> mObservableGases;
    private MutableLiveData<List<MeasurementsModel>> mObservableGetMeasurements;
    private MutableLiveData<List<MeasurementsModel>> mObservablePostResponse;
    private MutableLiveData<List<Temperature>> mObservableAsyncTemperaturesList;
    private MutableLiveData<List<Gas>> mObservableAsyncGasesList;
    private MutableLiveData<Integer> responseStatus;



    private CentralRepository(final SensorsValuesDatabase database) {
        mDatabase = database;
        mObservableTemperatures = new MediatorLiveData<>();
        mObservableGases = new MediatorLiveData<>();
        mObservableGetMeasurements = new MutableLiveData<>();
        mObservablePostResponse = new MutableLiveData<>();
        mObservableAsyncTemperaturesList = new MutableLiveData<>();
        mObservableAsyncGasesList = new MutableLiveData<>();
        responseStatus = new MutableLiveData<>();
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
        //mDatabase.getTemperatureDao().insert(temperature); // old synchronous way
        new insertAsyncTemperature(mDatabase.getTemperatureDao()).execute(temperature);
    }


    /**
     * Get the list of gas values from the database and get notified when the data changes.
     */
    public LiveData<List<Gas>> getGases() {
        return mObservableGases;
    }


    public void insertGas(Gas gas) {
        //mDatabase.getGasDao().insert(gas); // old synchronous way
        new insertAsyncGas(mDatabase.getGasDao()).execute(gas);
    }

    private static class insertAsyncGas extends AsyncTask<Gas, Void, Void> {

        private GasDao mAsyncGasDao;

        insertAsyncGas(GasDao dao) {
            mAsyncGasDao = dao;
        }

        @Override
        protected Void doInBackground(final Gas... params) {
            mAsyncGasDao.insert(params[0]);
            return null;
        }
    }

    private static class insertAsyncTemperature extends AsyncTask<Temperature, Void, Void> {

        private TemperatureDao mAsyncTemperatureDao;

        insertAsyncTemperature(TemperatureDao dao) {
            mAsyncTemperatureDao = dao;
        }

        @Override
        protected Void doInBackground(final Temperature... params) {
            mAsyncTemperatureDao.insert(params[0]);
            return null;
        }
    }


    public void clearDatabase() {
        new ClearDbAsync(mDatabase).execute();
    }


    private static class ClearDbAsync extends AsyncTask<Void, Void, Void> {

        private final GasDao gasDao;
        private final TemperatureDao temperatureDao;

        ClearDbAsync(SensorsValuesDatabase db) {
            gasDao = db.getGasDao();
            temperatureDao = db.getTemperatureDao();
        }

        @Override
        protected Void doInBackground(final Void... params) {
            gasDao.deleteAll();
            temperatureDao.deleteAll();
            return null;
        }
    }

    // retrieving the list of temperatures from the database
    public class GenerateTemperatureMeasurementAsyncTask extends AsyncTask<Void, Void, List<Temperature>> {

        private final TemperatureDao temperatureDao;

        GenerateTemperatureMeasurementAsyncTask(SensorsValuesDatabase db) {
            temperatureDao = db.getTemperatureDao();
        }

        @Override
        protected List<Temperature> doInBackground(Void... params) {
            return temperatureDao.getAllTemperatureValuesAsync();
        }

        @Override
        protected void onPostExecute(List<Temperature> temp) {
            mObservableAsyncTemperaturesList.setValue(temp);
        }
    }

    public void generateTemperatureMeasurementAsync() {
        mObservableAsyncTemperaturesList.setValue(null);
        new GenerateTemperatureMeasurementAsyncTask(mDatabase).execute();
    }


    public LiveData<List<Temperature>> getTemperatureMeasurementAsync() {
        return mObservableAsyncTemperaturesList;
    }



    // retrieving the list of gases from the database
    public class GenerateGasMeasurementAsyncTask extends AsyncTask<Void, Void, List<Gas>> {

        private final GasDao gasDao;

        GenerateGasMeasurementAsyncTask(SensorsValuesDatabase db) {
            gasDao = db.getGasDao();
        }

        @Override
        protected List<Gas> doInBackground(Void... params) {
            return gasDao.getAllGasValuesAsync();
        }

        @Override
        protected void onPostExecute(List<Gas> gas) {
            mObservableAsyncGasesList.setValue(gas);
        }
    }

    public void generateGasMeasurementAsync() {
        mObservableAsyncGasesList.setValue(null);
        new GenerateGasMeasurementAsyncTask(mDatabase).execute();
    }


    public LiveData<List<Gas>> getGasMeasurementAsync() {
        return mObservableAsyncGasesList;
    }


    // WebService related methods
    public void makeGetRequest() {
        mObservableGetMeasurements.setValue(null);

        saMiClient.getMeasurements().enqueue(new Callback<List<MeasurementsModel>>() {
            @Override
            public void onResponse(Call<List<MeasurementsModel>> call, Response<List<MeasurementsModel>> response) {
                if(response.isSuccessful())
                    mObservableGetMeasurements.setValue(response.body());
            }

            @Override
            public void onFailure(Call<List<MeasurementsModel>> call, Throwable throwable) {
                System.out.println(throwable.toString());
            }
        });
    }


    public LiveData<List<MeasurementsModel>> getWebMeasurements() {
        return mObservableGetMeasurements;
    }

    public void makePostRequest(String key, MeasurementsModel measurement) {
        mObservablePostResponse.setValue(null);

        List<MeasurementsModel> measList = new ArrayList<MeasurementsModel>();
        measList.add(measurement);
        MeasurementsPackage measurementsPackage = new MeasurementsPackage();
        measurementsPackage.setKey(key);
        measurementsPackage.setMeasurements(measList);

        saMiClient.postMeasurements(measurementsPackage).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {

                if(response.isSuccessful()) {
                    // TODO: read server response as a text!?
                    //mObservablePostResponse.setValue(response.body().getMeasurements());
                    Log.d(TAG, "success " + response.body());
                    responseStatus.setValue(1);
                } else {
                    Log.d(TAG, "success?? " + response.body());
                    responseStatus.setValue(1); //??
                }

                Log.d(TAG, "RESPONSE MESSAGE: " + response.message());
                mObservableAsyncTemperaturesList.setValue(null);
                mObservableAsyncGasesList.setValue(null);
                responseStatus.setValue(0);
            }

            @Override
            public void onFailure(Call<String> call, Throwable throwable) {
                System.out.println(throwable.toString());
                Log.d(TAG, "FAILED TO POST");
                responseStatus.setValue(2);
                mObservableAsyncTemperaturesList.setValue(null);
                mObservableAsyncGasesList.setValue(null);
                responseStatus.setValue(0);
            }
        });
    }

    public LiveData<List<MeasurementsModel>> getPostResponse() {
        return mObservablePostResponse;
    }

    public LiveData<Integer> getResponseStatus() {
        return responseStatus;
    }


}

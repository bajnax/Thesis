package com.savonia.thesis.db;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.TypeConverters;
import android.content.Context;

import com.savonia.thesis.db.dao.GasDao;
import com.savonia.thesis.db.dao.TemperatureDao;
import com.savonia.thesis.db.entity.Gas;
import com.savonia.thesis.db.entity.Temperature;

@Database(entities = {Temperature.class, Gas.class}, version = 1)
@TypeConverters({DateConverter.class})
public abstract class SensorsValuesDatabase extends RoomDatabase {

    private final MutableLiveData<Boolean> mIsDatabaseCreated = new MutableLiveData<>();
    private static volatile SensorsValuesDatabase INSTANCE;
    private static final String DATABASE_NAME = "sensors_values_database.db";

    public abstract TemperatureDao getTemperatureDao();
    public abstract GasDao getGasDao();

    //Singleton pattern
    public static synchronized SensorsValuesDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            // Creates database here
            INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                    SensorsValuesDatabase.class, DATABASE_NAME).build();
            INSTANCE.updateDatabaseCreated(context.getApplicationContext());
        }
        return INSTANCE;
    }

    private void updateDatabaseCreated(final Context context) {
        if (context.getDatabasePath(DATABASE_NAME).exists()) {
            setDatabaseCreated();
        }
    }

    private void setDatabaseCreated(){
        mIsDatabaseCreated.postValue(true);
    }

    public LiveData<Boolean> getDatabaseCreated() {
        return mIsDatabaseCreated;
    }

}




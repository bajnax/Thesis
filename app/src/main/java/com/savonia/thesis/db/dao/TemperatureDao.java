package com.savonia.thesis.db.dao;


import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import com.savonia.thesis.db.entity.Temperature;
import java.util.List;

@Dao
public interface TemperatureDao {

    @Insert
    void insert(Temperature temperature);

    @Query("DELETE FROM temperature_table")
    void deleteAll();

    @Query("SELECT * from temperature_table ORDER BY id")
    LiveData<List<Temperature>> getAllTemperatureValues();

    @Query("SELECT * from temperature_table ORDER BY id")
    List<Temperature> getAllTemperatureValuesAsync();

}

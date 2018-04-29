package com.savonia.thesis.db.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import com.savonia.thesis.db.entity.Gas;

import java.util.List;

@Dao
public interface GasDao {

    @Insert
    void insert(Gas gas);

    @Query("DELETE FROM gas_table")
    void deleteAll();

    @Query("SELECT * from gas_table ORDER BY id")
    LiveData<List<Gas>> getAllGasValues();

    @Query("SELECT * from gas_table ORDER BY id")
    List<Gas> getAllGasValuesAsync();
}

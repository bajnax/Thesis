package com.savonia.thesis.db.entity;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverters;
import android.support.annotation.NonNull;

import com.savonia.thesis.db.DateConverter;
import java.util.Date;

@Entity(tableName = "temperature_table")
public class Temperature {
    @PrimaryKey(autoGenerate = true)
    private long id;

    @NonNull
    @ColumnInfo(name = "TemperatureValue")
    private double mTemperature;

    @ColumnInfo(name = "Date")
    private Date date;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Temperature(double mTemperature) {
        this.mTemperature = mTemperature;
    }

    public Double getTemperature() {
        return this.mTemperature;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}

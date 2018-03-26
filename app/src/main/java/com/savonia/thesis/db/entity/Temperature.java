package com.savonia.thesis.db.entity;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

import java.time.LocalDateTime;

@Entity(tableName = "temperature_table")
public class Temperature {
    @PrimaryKey(autoGenerate = true)
    private int id;

    @NonNull
    @ColumnInfo(name = "TemperatureValue")
    private double mTemperature;

    @ColumnInfo(name = "Date")
    private LocalDateTime date;

    public Temperature(double mTemperature) {
        this.mTemperature = mTemperature;
    }

    public Double getTemperature() {
        return this.mTemperature;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public int getId() {
        return id;
    }

}

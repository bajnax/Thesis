package com.savonia.thesis.db.entity;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.Date;

@Entity(tableName = "temperature_table")
public class Temperature {

    @NonNull
    @PrimaryKey(autoGenerate = true)
    private long id;

    @NonNull
    @ColumnInfo(name = "TemperatureValue")
    private double mTemperatureValue;

    @NonNull
    @ColumnInfo(name = "Date")
    private Date date;

    public void setId(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public void setTemperatureValue(double mTemperature) {
        this.mTemperatureValue = mTemperature;
    }

    public Double getTemperatureValue() {
        return this.mTemperatureValue;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Date getDate() {
        return date;
    }

    public long getTimestamp() {
        return date.getTime();
    }

    public Temperature() {}

    public Temperature(double mTemperature, Date date) {
        this.mTemperatureValue = mTemperature;
        this.date = date;
    }

    public Temperature(double mTemperature) {
        this.mTemperatureValue = mTemperature;
        this.date = new Date(System.currentTimeMillis());
    }

}

package com.savonia.thesis.db.entity;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

import java.util.Date;

@Entity(tableName = "gas_table")
public class Gas {

    @NonNull
    @PrimaryKey(autoGenerate = true)
    private long id;

    @NonNull
    @ColumnInfo(name = "GasValue")
    private double mGasValue;

    @NonNull
    @ColumnInfo(name = "Date")
    private Date date;

    public void setId(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public void setGasValue(double mGasValue) {
        this.mGasValue = mGasValue;
    }

    public Double getGasValue() {
        return this.mGasValue;
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

    public Gas() {}

    public Gas(double mGasValue, Date date) {
        this.mGasValue = mGasValue;
        this.date = date;
    }

    public Gas(double mGasValue) {
        this.mGasValue = mGasValue;
        this.date = new Date(System.currentTimeMillis());
    }

}

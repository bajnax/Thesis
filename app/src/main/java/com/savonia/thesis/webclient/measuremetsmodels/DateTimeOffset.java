package com.savonia.thesis.webclient.measuremetsmodels;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class DateTimeOffset {

    @SerializedName("DateTime")
    @Expose
    private String dateTime;

    @SerializedName("OffsetMinutes")
    @Expose
    private int offsetMinutes;


    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public int getOffsetMinutes() {
        return offsetMinutes;
    }

    public void setOffsetMinutes(int offsetMinutes) {
        this.offsetMinutes = offsetMinutes;
    }

}

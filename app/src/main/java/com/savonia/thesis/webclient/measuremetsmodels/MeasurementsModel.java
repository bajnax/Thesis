package com.savonia.thesis.webclient.measuremetsmodels;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class MeasurementsModel {
    @SerializedName("Data")
    @Expose
    private ArrayList<DataModel> data;
    @SerializedName("Location")
    @Expose
    private Object location;
    @SerializedName("Note")
    @Expose
    private String note;
    @SerializedName("Object")
    @Expose
    private String object;
    @SerializedName("Tag")
    @Expose
    private String tag;
    @SerializedName("TimestampISO8601")
    @Expose
    private String timestampISO8601;
    // 2018-04-27T12:36:22+03:00

    /*@SerializedName("Timestamp")
    @Expose
    private String timestamp;*/



    public MeasurementsModel() {}

    public ArrayList<DataModel> getData() {
        return data;
    }

    public void setData(ArrayList<DataModel> data) {
        this.data = data;
    }

    public Object getLocation() {
        return location;
    }

    public void setLocation(Object location) {
        this.location = location;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getTimestampISO8601() {
        return timestampISO8601;
    }

    public void setTimestampISO8601(String timestampISO8601) {
        this.timestampISO8601 = timestampISO8601;
    }

    /*public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }*/

}

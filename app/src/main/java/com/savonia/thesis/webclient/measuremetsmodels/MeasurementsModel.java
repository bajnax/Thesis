package com.savonia.thesis.webclient.measuremetsmodels;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


import java.time.OffsetDateTime;
import java.util.List;

public class MeasurementsModel {
    @SerializedName("Data")
    @Expose
    private List<DataModel> data = null;
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
    public MeasurementsModel() {}

    public List<DataModel> getData() {
        return data;
    }

    public void setData(List<DataModel> data) {
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

}

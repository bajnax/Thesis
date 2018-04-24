package com.savonia.thesis.webclient.measuremetsmodels;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class MeasurementsPackage {

    @SerializedName("key")
    @Expose
    private String key;

    @SerializedName("measurements")
    @Expose
    private List<MeasurementsModel> measurements = null;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public List<MeasurementsModel> getMeasurements() {
        return measurements;
    }

    public void setMeasurements(List<MeasurementsModel> measurements) {
        this.measurements = measurements;
    }

}

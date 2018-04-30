package com.savonia.thesis.viewmodels;

import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;

public class GetRequestViewModel extends ViewModel {

    public MutableLiveData<String> getMeasurementName() {
        return measurementName;
    }
    public void setMeasurementName(String measName) {
        measurementName.setValue(measName);
    }

    public MutableLiveData<String> getMeasurementTag() {
        return measurementTag;
    }
    public void setMeasurementTag(String measTag) {
        measurementTag.setValue(measTag);
    }

    public MutableLiveData<String> getTemperatureTag() {
        return temperatureTag;
    }
    public void setTemperatureTag(String tempTag) {
        temperatureTag.setValue(tempTag);
    }

    public MutableLiveData<String> getGasTag() {
        return gasTag;
    }
    public void setGasTag(String gTag) {
        gasTag.setValue(gTag);
    }

    public MutableLiveData<String> getKey() {
        return key;
    }
    public void setKey(String keyName) {
        key.setValue(keyName);
    }

    public MutableLiveData<Integer> getTakeAmount() {
        return takeAmount;
    }
    public void setTakeAmount(Integer take) {
        takeAmount.setValue(take);
    }

    private final MutableLiveData<String> measurementName = new MutableLiveData<String>();
    private final MutableLiveData<String> measurementTag = new MutableLiveData<String>();
    private final MutableLiveData<String> temperatureTag = new MutableLiveData<String>();
    private final MutableLiveData<String> gasTag = new MutableLiveData<String>();
    private final MutableLiveData<String> key = new MutableLiveData<String>();
    private final MutableLiveData<Integer> takeAmount = new MutableLiveData<Integer>();


}

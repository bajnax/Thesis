package com.savonia.thesis.viewmodels;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;

public class SharedViewModel extends ViewModel {
    private final MutableLiveData<Boolean> isScrollToEndSelected = new MutableLiveData<Boolean>();
    private final MutableLiveData<Boolean> hasReceivedServices = new MutableLiveData<Boolean>();
    private final MutableLiveData<Integer> connectionState = new MutableLiveData<Integer>();

    public void setScrollToEnd(Boolean isScrollToEndSelected) {
        this.isScrollToEndSelected.setValue(isScrollToEndSelected);
    }

    public LiveData<Boolean> getScrollToEnd() {
        return isScrollToEndSelected;
    }

    public void setHasReceivedServices(Boolean hasReceivedServices) {
        this.hasReceivedServices.setValue(hasReceivedServices);
    }

    public LiveData<Boolean> getHasReceivedServices() {
        return hasReceivedServices;
    }

    public void setConnectionState(Integer connectionState) {
        this.connectionState.setValue(connectionState);
    }

    public LiveData<Integer> getConnectionState() {
        return connectionState;
    }
}

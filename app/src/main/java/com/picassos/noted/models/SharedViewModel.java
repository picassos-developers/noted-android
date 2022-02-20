package com.picassos.noted.models;


import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.io.Serializable;

public class SharedViewModel extends ViewModel {
    private final MutableLiveData<Integer> requestCode = new MutableLiveData<>();
    private final MutableLiveData<Serializable> data = new MutableLiveData<>();

    public void setRequestCode(Integer item) {
        requestCode.setValue(item);
    }

    public LiveData<Integer> getRequestCode() {
        return requestCode;
    }

    public void setData(Serializable item) {
        data.setValue(item);
    }

    public LiveData<Serializable> getData() {
        return data;
    }
}

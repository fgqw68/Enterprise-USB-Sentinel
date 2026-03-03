package com.sabir.usbsentinel.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.sabir.usbsentinel.data.entity.UsbDeviceEntity;
import com.sabir.usbsentinel.repository.UsbDeviceRepository;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UsbDeviceViewModel extends AndroidViewModel {

    private final UsbDeviceRepository repository;
    private final MutableLiveData<List<UsbDeviceEntity>> deviceList;
    private final ExecutorService executorService;

    public UsbDeviceViewModel(@NonNull Application application) {
        super(application);
        repository = new UsbDeviceRepository(application.getApplicationContext());
        deviceList = new MutableLiveData<>();
        executorService = Executors.newSingleThreadExecutor();
        loadDevices();
    }

    private void loadDevices() {
        executorService.execute(() -> {
            List<UsbDeviceEntity> devices = repository.getAllDevices();
            deviceList.postValue(devices);
        });
    }

    public LiveData<List<UsbDeviceEntity>> getDeviceList() {
        return deviceList;
    }

    public void insertDevice(int vendorId, int productId, String deviceName, boolean isBlocked) {
        executorService.execute(() -> {
            UsbDeviceEntity device = new UsbDeviceEntity(vendorId, productId, deviceName, isBlocked);
            repository.insertDevice(device);
            loadDevices();
        });
    }

    public void deleteDevice(UsbDeviceEntity device) {
        executorService.execute(() -> {
            repository.deleteDevice(device);
            loadDevices();
        });
    }

    public boolean isDeviceBlocked(int vid, int pid) {
        return repository.isDeviceBlocked(vid, pid);
    }
}
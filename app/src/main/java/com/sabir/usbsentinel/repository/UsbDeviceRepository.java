package com.sabir.usbsentinel.repository;

import android.content.Context;

import com.sabir.usbsentinel.data.dao.UsbDeviceDao;
import com.sabir.usbsentinel.data.database.AppDatabase;
import com.sabir.usbsentinel.data.entity.UsbDeviceEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UsbDeviceRepository {

    private final UsbDeviceDao usbDeviceDao;
    private final ExecutorService executorService;

    public UsbDeviceRepository(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        this.usbDeviceDao = database.usbDeviceDao();
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public void insertDevice(UsbDeviceEntity device) {
        executorService.execute(() -> usbDeviceDao.insert(device));
    }

    public void deleteDevice(UsbDeviceEntity device) {
        executorService.execute(() -> usbDeviceDao.delete(device));
    }

    public boolean isDeviceBlocked(int vid, int pid) {
        return usbDeviceDao.isDeviceBlocked(vid, pid);
    }

    public List<UsbDeviceEntity> getAllDevices() {
        return usbDeviceDao.getAllDevices();
    }

    public UsbDeviceEntity getDeviceByVidPid(int vid, int pid) {
        return usbDeviceDao.getDeviceByVidPid(vid, pid);
    }
}
package com.sabir.usbsentinel.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.sabir.usbsentinel.data.dao.UsbDeviceDao;
import com.sabir.usbsentinel.data.database.AppDatabase;
import com.sabir.usbsentinel.data.entity.UsbDeviceEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UsbDeviceRepository {

    private final UsbDeviceDao usbDeviceDao;
    private final AppDatabase database;
    private final ExecutorService executorService;

    public UsbDeviceRepository(Context context) {
        this.database = AppDatabase.getInstance(context);
        this.usbDeviceDao = database.usbDeviceDao();
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public void insertDevice(UsbDeviceEntity device) {
        executorService.execute(() -> usbDeviceDao.insert(device));
    }

    public void bulkInsert(List<UsbDeviceEntity> devices) {
        executorService.execute(() -> usbDeviceDao.bulkInsert(devices));
    }

    public void deleteDevice(UsbDeviceEntity device) {
        executorService.execute(() -> usbDeviceDao.delete(device));
    }

    public void deleteByVidPid(int vid, int pid) {
        executorService.execute(() -> usbDeviceDao.deleteByVidPid(vid, pid));
    }

    public void clearAll() {
        executorService.execute(() -> usbDeviceDao.clearAll());
    }

    public boolean isDeviceBlocked(int vid, int pid) {
        return usbDeviceDao.isDeviceBlocked(vid, pid);
    }

    public List<UsbDeviceEntity> getAllDevices() {
        return usbDeviceDao.getAllDevices();
    }

    public LiveData<List<UsbDeviceEntity>> getAllDevicesLiveData() {
        return usbDeviceDao.getAllDevicesLiveData();
    }

    public UsbDeviceEntity getDeviceByVidPid(int vid, int pid) {
        return usbDeviceDao.getDeviceByVidPid(vid, pid);
    }

    /**
     * Apply the Admin Master Override policy atomically.
     * This method clears all existing blocked devices and replaces them with the provided list
     * in a single atomic transaction.
     *
     * Transaction steps:
     * 1. Clear the entire usb_devices table (DELETE FROM usb_devices)
     * 2. Bulk insert the new blocked devices
     *
     * @param devices The list of devices to block. If null or empty, clears all restrictions.
     */
    public void applyBlockedDeviceAdminPolicy(List<UsbDeviceEntity> devices) {
        executorService.execute(() -> {
            database.runInTransaction(() -> {
                // Step 1: Clear the entire usb_devices table
                usbDeviceDao.clearAll();

                // Step 2: Bulk insert the new blocked devices
                if (devices != null && !devices.isEmpty()) {
                    usbDeviceDao.bulkInsert(devices);
                }
            });
        });
    }
}
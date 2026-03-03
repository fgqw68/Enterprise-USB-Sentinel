package com.sabir.usbsentinel.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.sabir.usbsentinel.data.entity.UsbDeviceEntity;

import java.util.List;

@Dao
public interface UsbDeviceDao {

    @Insert
    void insert(UsbDeviceEntity device);

    @Delete
    void delete(UsbDeviceEntity device);

    @Query("SELECT * FROM usb_devices")
    List<UsbDeviceEntity> getAllDevices();

    @Query("SELECT isBlocked FROM usb_devices WHERE vendorId = :vid AND productId = :pid LIMIT 1")
    boolean isDeviceBlocked(int vid, int pid);

    @Query("SELECT * FROM usb_devices WHERE vendorId = :vid AND productId = :pid LIMIT 1")
    UsbDeviceEntity getDeviceByVidPid(int vid, int pid);
}
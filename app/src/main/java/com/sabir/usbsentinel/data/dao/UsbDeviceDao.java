package com.sabir.usbsentinel.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.sabir.usbsentinel.data.entity.UsbDeviceEntity;

import java.util.List;

@Dao
public interface UsbDeviceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(UsbDeviceEntity device);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void bulkInsert(List<UsbDeviceEntity> devices);

    @Delete
    void delete(UsbDeviceEntity device);

    @Query("DELETE FROM usb_devices WHERE vendorId = :vid AND productId = :pid")
    void deleteByVidPid(int vid, int pid);

    @Query("DELETE FROM usb_devices")
    void clearAll();

    @Query("SELECT * FROM usb_devices")
    List<UsbDeviceEntity> getAllDevices();

    @Query("SELECT * FROM usb_devices")
    LiveData<List<UsbDeviceEntity>> getAllDevicesLiveData();

    @Query("SELECT COUNT(*) > 0 FROM usb_devices WHERE vendorId = :vid AND productId = :pid")
    boolean isDeviceBlocked(int vid, int pid);

    @Query("SELECT * FROM usb_devices WHERE vendorId = :vid AND productId = :pid LIMIT 1")
    UsbDeviceEntity getDeviceByVidPid(int vid, int pid);
}
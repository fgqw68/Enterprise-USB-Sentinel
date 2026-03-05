package com.sabir.usbsentinel.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "usb_devices")
public class UsbDeviceEntity {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private int vendorId;

    private int productId;

    private String deviceName;

    public UsbDeviceEntity(int vendorId, int productId, String deviceName) {
        this.vendorId = vendorId;
        this.productId = productId;
        this.deviceName = deviceName;
    }

    // Getters and Setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getVendorId() {
        return vendorId;
    }

    public void setVendorId(int vendorId) {
        this.vendorId = vendorId;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }
}
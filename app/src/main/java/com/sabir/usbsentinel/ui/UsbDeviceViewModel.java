package com.sabir.usbsentinel.ui;

import android.app.Application;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.sabir.usbsentinel.api.VendorApi;
import com.sabir.usbsentinel.data.entity.UsbDeviceEntity;
import com.sabir.usbsentinel.repository.UsbDeviceRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * UsbDeviceViewModel manages the USB device policy enforcement.
 *
 * Dual-List Logic:
 * - Active List: MediatorLiveData that filters physical devices (from UsbManager)
 *                against the Room Blacklist, displaying only allowed devices.
 * - Blocked List: Direct Room LiveData<List<UsbDeviceEntity>> showing all blocked devices.
 */
public class UsbDeviceViewModel extends AndroidViewModel {

    private final UsbDeviceRepository repository;
    private final VendorApi vendorApi;
    private final UsbManager usbManager;
    private final ExecutorService executorService;

    // Active List: Physical devices filtered against blacklist
    private final MutableLiveData<List<UsbDevice>> physicalDevicesSource;
    private final MediatorLiveData<List<UsbDevice>> activeDevices;

    // Blocked List: Direct from Room database
    private final LiveData<List<UsbDeviceEntity>> blockedDevices;

    public UsbDeviceViewModel(@NonNull Application application) {
        super(application);
        repository = new UsbDeviceRepository(application.getApplicationContext());
        vendorApi = VendorApi.getInstance();
        usbManager = (UsbManager) application.getSystemService(Application.USB_SERVICE);
        executorService = Executors.newSingleThreadExecutor();

        // Initialize VendorApi
        vendorApi.initialize(application.getApplicationContext());

        // Sources for MediatorLiveData
        physicalDevicesSource = new MutableLiveData<>();
        blockedDevices = repository.getAllDevicesLiveData();

        // Active Devices: Physical devices filtered against blacklist
        activeDevices = new MediatorLiveData<>();
        activeDevices.addSource(physicalDevicesSource, this::filterActiveDevices);
        activeDevices.addSource(blockedDevices, devices -> filterActiveDevices(physicalDevicesSource.getValue()));

        // Load initial physical devices
        refreshPhysicalDevices();
    }

    /**
     * Refresh the list of physical USB devices from UsbManager.
     * This triggers the MediatorLiveData to re-filter against the blacklist.
     */
    public void refreshPhysicalDevices() {
        executorService.execute(() -> {
            List<UsbDevice> physicalList = new ArrayList<>();
            if (usbManager != null) {
                HashMap<String, UsbDevice> deviceMap = usbManager.getDeviceList();
                if (deviceMap != null) {
                    physicalList.addAll(deviceMap.values());
                }
            }
            physicalDevicesSource.postValue(physicalList);
        });
    }

    /**
     * Filter physical devices against the blocked list to create the active list.
     * Only devices NOT in the blacklist are included.
     */
    private void filterActiveDevices(List<UsbDevice> physicalDevices) {
        executorService.execute(() -> {
            List<UsbDevice> filtered = new ArrayList<>();
            List<UsbDeviceEntity> blocked = blockedDevices.getValue();

            if (physicalDevices == null) {
                activeDevices.postValue(filtered);
                return;
            }

            if (blocked == null || blocked.isEmpty()) {
                // No blocked devices, all physical devices are active
                activeDevices.postValue(physicalDevices);
                return;
            }

            // Filter out blocked devices
            for (UsbDevice usbDevice : physicalDevices) {
                int vid = usbDevice.getVendorId();
                int pid = usbDevice.getProductId();

                boolean isBlocked = false;
                for (UsbDeviceEntity blockedEntity : blocked) {
                    if (blockedEntity.getVendorId() == vid && blockedEntity.getProductId() == pid) {
                        isBlocked = true;
                        break;
                    }
                }

                if (!isBlocked) {
                    filtered.add(usbDevice);
                }
            }

            activeDevices.postValue(filtered);
        });
    }

    /**
     * Get the Active List: physical devices filtered against the blacklist.
     * This should be observed by the Home Screen RecyclerView.
     */
    public LiveData<List<UsbDevice>> getActiveDevices() {
        return activeDevices;
    }

    /**
     * Get the Blocked List: all devices in the Room blacklist.
     * This should be observed by the Blocked View RecyclerView.
     */
    public LiveData<List<UsbDeviceEntity>> getBlockedDevices() {
        return blockedDevices;
    }

    /**
     * Block a physical USB device.
     * 1) Inserts into Room Blacklist
     * 2) Calls VendorApi.blockPeripheral(vid, pid)
     */
    public void blockDevice(UsbDevice usbDevice) {
        executorService.execute(() -> {
            int vid = usbDevice.getVendorId();
            int pid = usbDevice.getProductId();

            // 1) Insert into Room Blacklist
            UsbDeviceEntity entity = new UsbDeviceEntity(vid, pid, usbDevice.getDeviceName());
            repository.insertDevice(entity);

            // 2) Call VendorApi to block at system level
            vendorApi.blockPeripheral(vid, pid);

            // 3) Refresh the physical devices list to trigger re-filtering
            refreshPhysicalDevices();
        });
    }

    /**
     * Unblock a device.
     * 1) Deletes from Room Blacklist
     * 2) Calls VendorApi.unblockPeripheral(vid, pid)
     */
    public void unblockDevice(UsbDeviceEntity entity) {
        executorService.execute(() -> {
            int vid = entity.getVendorId();
            int pid = entity.getProductId();

            // 1) Delete from Room Blacklist
            repository.deleteByVidPid(vid, pid);

            // 2) Call VendorApi to unblock at system level
            vendorApi.unblockPeripheral(vid, pid);

            // 3) Refresh the physical devices list to trigger re-filtering
            refreshPhysicalDevices();
        });
    }

    /**
     * Bulk block devices from admin configuration.
     * When a Bundle with the key blocked_usb_list is pushed,
     * bulk-insert those IDs into Room and trigger the VendorApi for each.
     *
     * Bundle format:
     * - int[] vids: Array of vendor IDs
     * - int[] pids: Array of product IDs
     * - String[] deviceNames: Array of device names (optional)
     */
    public void bulkBlockDevices(Bundle deviceBundle) {
        executorService.execute(() -> {
            int[] vids = deviceBundle.getIntArray("vids");
            int[] pids = deviceBundle.getIntArray("pids");
            String[] deviceNames = deviceBundle.getStringArray("deviceNames");

            if (vids == null || pids == null) {
                return;
            }

            if (vids.length != pids.length) {
                return;
            }

            List<UsbDeviceEntity> devicesToBlock = new ArrayList<>();

            for (int i = 0; i < vids.length; i++) {
                int vid = vids[i];
                int pid = pids[i];
                String name = (deviceNames != null && i < deviceNames.length && deviceNames[i] != null)
                        ? deviceNames[i] : "Admin Blocked Device";

                UsbDeviceEntity entity = new UsbDeviceEntity(vid, pid, name);
                devicesToBlock.add(entity);

                // Call VendorApi to block at system level
                vendorApi.blockPeripheral(vid, pid);
            }

            // Bulk insert all devices into the blacklist
            repository.bulkInsert(devicesToBlock);

            // Refresh the physical devices list to trigger re-filtering
            refreshPhysicalDevices();
        });
    }

    /**
     * Manually block a device by VID/PID (for pre-blocking devices not currently connected).
     */
    public void blockDeviceByVidPid(int vid, int pid, String deviceName) {
        executorService.execute(() -> {
            UsbDeviceEntity entity = new UsbDeviceEntity(vid, pid, deviceName);
            repository.insertDevice(entity);
            vendorApi.blockPeripheral(vid, pid);
            refreshPhysicalDevices();
        });
    }

    /**
     * Check if a device is blocked in the Room blacklist.
     */
    public boolean isDeviceBlocked(int vid, int pid) {
        return repository.isDeviceBlocked(vid, pid);
    }

    /**
     * Refresh both lists - call this when resuming the activity.
     */
    public void refreshAll() {
        refreshPhysicalDevices();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }
}
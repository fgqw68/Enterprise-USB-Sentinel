package com.sabir.usbsentinel.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.sabir.usbsentinel.api.VendorApi;
import com.sabir.usbsentinel.repository.UsbDeviceRepository;
import com.sabir.usbsentinel.utils.NotificationHelper;

/**
 * UsbReceiver enforces USB device policy by intercepting device attach events.
 *
 * Policy Enforcement Logic:
 * - On ACTION_USB_DEVICE_ATTACHED, check the Room Blacklist.
 * - If a match is found, immediately call VendorApi.blockPeripheral()
 * - Do NOT request system permissions for blocked devices.
 * - Show a notification for blocked devices.
 */
public class UsbReceiver extends BroadcastReceiver {

    private static final String TAG = "UsbReceiver";

    private UsbDeviceRepository repository;
    private VendorApi vendorApi;

    @Override
    public void onReceive(Context context, Intent intent) {
        // Initialize repository and vendor API if needed
        if (repository == null) {
            repository = new UsbDeviceRepository(context.getApplicationContext());
        }
        if (vendorApi == null) {
            vendorApi = VendorApi.getInstance();
        }

        // Ensure notification channel exists
        NotificationHelper.createNotificationChannel(context);

        String action = intent.getAction();

        if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
            // Extract the UsbDevice object from the intent
            @SuppressWarnings("deprecation")
            UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

            if (usbDevice != null) {
                handleUsbDeviceAttached(context, usbDevice);
            }
        }
    }

    /**
     * Handle USB device attached event with policy enforcement.
     *
     * @param context The application context
     * @param usbDevice The USB device that was attached
     */
    private void handleUsbDeviceAttached(Context context, UsbDevice usbDevice) {
        int vendorId = usbDevice.getVendorId();
        int productId = usbDevice.getProductId();
        String deviceName = usbDevice.getDeviceName();

        Log.i(TAG, String.format("USB device attached: VID=0x%04X, PID=0x%04X, Name=%s",
                vendorId, productId, deviceName));

        // Check if device is in blocked list
        boolean isBlocked = repository.isDeviceBlocked(vendorId, productId);

        if (isBlocked) {
            // Device is blocked - enforce policy immediately
            enforceBlockedPolicy(context, usbDevice);
        } else {
            // Device is not blocked - allow normal access
            allowDevice(usbDevice);
        }
    }

    /**
     * Enforce blocked policy for a USB device.
     * This calls VendorApi.blockPeripheral() and shows a notification.
     * No system permission request is made for blocked devices.
     *
     * @param context The application context
     * @param usbDevice The USB device to block
     */
    private void enforceBlockedPolicy(Context context, UsbDevice usbDevice) {
        int vendorId = usbDevice.getVendorId();
        int productId = usbDevice.getProductId();

        Log.w(TAG, String.format("Blocking USB device: VID=0x%04X, PID=0x%04X",
                vendorId, productId));

        // 1) Call VendorApi to block at system level
        vendorApi.blockPeripheral(vendorId, productId);

        // 2) Show notification for the blocked device
        showBlockedDeviceNotification(context, usbDevice);

        // 3) Do NOT request system permissions for this device
        // The VendorApi call handles the blocking at system level
    }

    /**
     * Allow normal access to a USB device.
     *
     * @param usbDevice The USB device to allow
     */
    private void allowDevice(UsbDevice usbDevice) {
        Log.i(TAG, String.format("Allowing USB device: VID=0x%04X, PID=0x%04X",
                usbDevice.getVendorId(), usbDevice.getProductId()));

        // Device is not blocked - allow normal access
        // The system will handle permission requests automatically
        // Optionally, could insert into history or log here
    }

    /**
     * Show a notification for a blocked device.
     *
     * @param context The application context
     * @param usbDevice The blocked USB device
     */
    private void showBlockedDeviceNotification(Context context, UsbDevice usbDevice) {
        NotificationHelper.showBlockedDeviceNotification(context, usbDevice);
    }
}
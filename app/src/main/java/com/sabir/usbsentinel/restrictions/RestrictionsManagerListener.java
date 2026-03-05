package com.sabir.usbsentinel.restrictions;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.RestrictionsManager;
import android.os.Bundle;
import android.util.Log;

import com.sabir.usbsentinel.api.VendorApi;
import com.sabir.usbsentinel.data.entity.UsbDeviceEntity;
import com.sabir.usbsentinel.repository.UsbDeviceRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * RestrictionsManagerListener handles administrator policy overrides for USB blocking.
 * It implements a "Master Override" system where Admin policies completely replace
 * the existing blacklist whenever a new configuration is pushed.
 *
 * Managed Configuration Schema:
 * - AllowAllUSB (Boolean, default true): Master switch. When true, clears all restrictions.
 * - BlockList (Bundle Array): Contains nested bundles with vendorId, productId, deviceName.
 *
 * The Admin policy acts as a "Master Override" that resets the database whenever updated.
 * Users can still manually add/remove devices after the policy is applied.
 * Manual changes persist until the next Admin configuration update.
 */
public class RestrictionsManagerListener extends BroadcastReceiver {

    private static final String TAG = "RestrictionsManager";

    // Managed Configuration keys
    private static final String KEY_ALLOW_ALL_USB = "AllowAllUSB";
    private static final String KEY_BLOCK_LIST = "BlockList";

    private UsbDeviceRepository repository;
    private VendorApi vendorApi;
    private ExecutorService executorService;

    /**
     * Check and apply current restrictions from the RestrictionsManager.
     * This should be called on app startup to handle pre-existing configurations.
     *
     * @param context The application context
     */
    public void checkAndApplyRestrictions(Context context) {
        RestrictionsManager restrictionsManager =
                (RestrictionsManager) context.getSystemService(Context.RESTRICTIONS_SERVICE);
        if (restrictionsManager == null) {
            Log.w(TAG, "RestrictionsManager not available");
            return;
        }

        Bundle restrictions = restrictionsManager.getApplicationRestrictions();
        if (restrictions == null || restrictions.isEmpty()) {
            Log.d(TAG, "No restrictions found");
            return;
        }

        Log.i(TAG, "Applying Master Override restrictions from MDM/EMM");
        processRestrictions(context, restrictions);
    }

    /**
     * Register this listener for dynamic restriction change notifications.
     * Must be called from Activity.onStart() and unregistered in onStop().
     *
     * @param context The context to register with
     */
    public void register(Context context) {
        IntentFilter filter = new IntentFilter(Intent.ACTION_APPLICATION_RESTRICTIONS_CHANGED);
        context.registerReceiver(this, filter);
        Log.d(TAG, "Registered for restriction changes");
    }

    /**
     * Unregister this listener to prevent memory leaks.
     * Must be called from Activity.onStop().
     *
     * @param context The context to unregister from
     */
    public void unregister(Context context) {
        try {
            context.unregisterReceiver(this);
            Log.d(TAG, "Unregistered from restriction changes");
        } catch (IllegalArgumentException e) {
            // Receiver wasn't registered, ignore
            Log.w(TAG, "Receiver not registered: " + e.getMessage());
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }

        Log.i(TAG, "Restrictions changed, applying Master Override");
        checkAndApplyRestrictions(context);
    }

    /**
     * Initialize dependencies if needed.
     */
    private void ensureInitialized(Context context) {
        if (repository == null) {
            repository = new UsbDeviceRepository(context.getApplicationContext());
        }
        if (vendorApi == null) {
            vendorApi = VendorApi.getInstance();
        }
        if (executorService == null) {
            executorService = Executors.newSingleThreadExecutor();
        }
    }

    /**
     * Process restrictions from the MDM/EMM configuration.
     * Implements the "Master Override" logic:
     * 1. Check the Master Switch (AllowAllUSB)
     * 2. If true: Clear all restrictions
     * 3. If false: Replace entire blacklist with BlockList
     *
     * @param context The application context
     * @param restrictions The bundle containing restriction configuration
     */
    private void processRestrictions(final Context context, Bundle restrictions) {
        ensureInitialized(context);

        executorService.execute(() -> {
            try {
                // Check the Master Switch
                boolean allowAllUSB = restrictions.getBoolean(KEY_ALLOW_ALL_USB, true);

                if (allowAllUSB) {
                    // Admin is resetting security - clear all blocked devices
                    Log.i(TAG, "AllowAllUSB is true - clearing all restrictions");
                    applyMasterOverrideClear();
                } else {
                    // Apply the BlockList - atomic transaction to replace entire list
                    Log.i(TAG, "AllowAllUSB is false - applying BlockList");
                    applyMasterOverrideWithBlockList(restrictions);
                }

            } catch (Exception e) {
                Log.e(TAG, "Error processing Master Override restrictions: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Apply Master Override: Clear all restrictions.
     * This is called when AllowAllUSB is true.
     */
    private void applyMasterOverrideClear() {
        try {
            // Get all current blocked devices to unblock at vendor level
            List<UsbDeviceEntity> existingDevices = repository.getAllDevices();

            // Clear the database via repository
            repository.clearAll();

            // Unblock all devices at vendor API level
            for (UsbDeviceEntity device : existingDevices) {
                vendorApi.unblockPeripheral(device.getVendorId(), device.getProductId());
            }

            Log.i(TAG, String.format("Cleared %d devices from blacklist (Master Override - AllowAllUSB=true)",
                    existingDevices.size()));

        } catch (Exception e) {
            Log.e(TAG, "Error clearing restrictions: " + e.getMessage(), e);
        }
    }

    /**
     * Apply Master Override with BlockList.
     * This is called when AllowAllUSB is false.
     *
     * Delegates to the repository's applyBlockedDeviceAdminPolicy method which
     * uses an atomic transaction to clear and insert in a single operation.
     *
     * @param restrictions The restrictions bundle containing the BlockList
     */
    private void applyMasterOverrideWithBlockList(Bundle restrictions) {
        // Step 0: Parse the BlockList first
        List<UsbDeviceEntity> newBlockedList = parseBlockList(restrictions);

        // Get existing devices to unblock at vendor level (before atomic transaction)
        List<UsbDeviceEntity> existingDevices = repository.getAllDevices();

        // Apply the Master Override via repository (atomic transaction)
        repository.applyBlockedDeviceAdminPolicy(newBlockedList);

        // Update vendor API state
        // Unblock devices that are no longer in the list
        for (UsbDeviceEntity oldDevice : existingDevices) {
            boolean stillBlocked = false;
            for (UsbDeviceEntity newDevice : newBlockedList) {
                if (oldDevice.getVendorId() == newDevice.getVendorId()
                        && oldDevice.getProductId() == newDevice.getProductId()) {
                    stillBlocked = true;
                    break;
                }
            }
            if (!stillBlocked) {
                vendorApi.unblockPeripheral(oldDevice.getVendorId(), oldDevice.getProductId());
            }
        }

        // Block new devices
        for (UsbDeviceEntity newDevice : newBlockedList) {
            vendorApi.blockPeripheral(newDevice.getVendorId(), newDevice.getProductId());
            Log.i(TAG, String.format("Master Override: Blocking VID=0x%04X, PID=0x%04X - %s",
                    newDevice.getVendorId(), newDevice.getProductId(), newDevice.getDeviceName()));
        }

        Log.i(TAG, String.format("Master Override applied: Replaced %d old entries with %d new entries",
                existingDevices.size(), newBlockedList.size()));
    }

    /**
     * Parse the BlockList from restrictions.
     * BlockList is a Bundle array with each entry containing:
     * - vendorId (int)
     * - productId (int)
     * - deviceName (String)
     *
     * @param restrictions The restrictions bundle
     * @return List of UsbDeviceEntity to block
     */
    private List<UsbDeviceEntity> parseBlockList(Bundle restrictions) {
        List<UsbDeviceEntity> devicesToBlock = new ArrayList<>();

        try {
            android.os.Parcelable[] blockListArray = restrictions.getParcelableArray(KEY_BLOCK_LIST);
            if (blockListArray == null) {
                Log.w(TAG, "BlockList is null or not a ParcelableArray");
                return devicesToBlock;
            }

            for (android.os.Parcelable deviceEntry : blockListArray) {
                if (deviceEntry instanceof Bundle) {
                    Bundle deviceBundle = (Bundle) deviceEntry;
                    int vendorId = deviceBundle.getInt("vendorId", -1);
                    int productId = deviceBundle.getInt("productId", -1);
                    String deviceName = deviceBundle.getString("deviceName", "Admin Blocked Device");

                    if (vendorId != -1 && productId != -1) {
                        devicesToBlock.add(new UsbDeviceEntity(vendorId, productId, deviceName));
                    } else {
                        Log.w(TAG, String.format("Invalid device entry: vendorId=%d, productId=%d",
                                vendorId, productId));
                    }
                }
            }

            Log.i(TAG, String.format("Parsed %d valid devices from BlockList", devicesToBlock.size()));

        } catch (Exception e) {
            Log.e(TAG, "Error parsing BlockList: " + e.getMessage(), e);
        }

        return devicesToBlock;
    }

    /**
     * Clear all admin-imposed USB restrictions.
     */
    public void clearRestrictions() {
        if (executorService == null) {
            executorService = Executors.newSingleThreadExecutor();
        }

        executorService.execute(() -> {
            try {
                applyMasterOverrideClear();
                Log.i(TAG, "Cleared all admin restrictions via API");
            } catch (Exception e) {
                Log.e(TAG, "Error clearing restrictions: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Shutdown the listener and cleanup resources.
     */
    public void shutdown() {
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}
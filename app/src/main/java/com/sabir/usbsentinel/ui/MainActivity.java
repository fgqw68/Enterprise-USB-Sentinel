package com.sabir.usbsentinel.ui;

import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sabir.usbsentinel.R;
import com.sabir.usbsentinel.api.VendorApi;
import com.sabir.usbsentinel.data.entity.UsbDeviceEntity;
import com.sabir.usbsentinel.restrictions.RestrictionsManagerListener;
import com.sabir.usbsentinel.utils.NotificationHelper;

/**
 * MainActivity displays both Active and Blocked USB device lists.
 *
 * UI Wiring:
 * - Active List: Shows physically connected devices filtered against the blacklist.
 *                Binds to UsbDeviceViewModel.getActiveDevices()
 *                Has a 'Block' button on each item.
 *
 * - Blocked List: Shows all devices in the Room blacklist.
 *                Binds to UsbDeviceViewModel.getBlockedDevices()
 *                Has an 'Unblock' button on each item.
 *                Includes a form for pre-blocking devices by VID/PID.
 */
public class MainActivity extends AppCompatActivity {

    private UsbDeviceViewModel viewModel;

    // Managed Configurations listener
    private RestrictionsManagerListener restrictionsListener;

    // Adapters
    private DeviceAdapter activeAdapter;
    private BlockedDeviceAdapter blockedAdapter;

    // Views for Active List
    private LinearLayout sectionActive;
    private RecyclerView recyclerViewActive;

    // Views for Blocked List
    private LinearLayout sectionBlocked;
    private RecyclerView recyclerViewBlocked;
    private EditText etVendorId;
    private EditText etProductId;
    private EditText etDeviceName;
    private Button btnBlockDevice;

    // Tab buttons
    private Button btnTabActive;
    private Button btnTabBlocked;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewModel = new ViewModelProvider(this).get(UsbDeviceViewModel.class);

        initViews();
        setupAdapters();
        setupTabSwitching();
        observeViewModel();

        // Initialize VendorApi and notification channel
        VendorApi.getInstance().initialize(this);
        NotificationHelper.createNotificationChannel(this);

        // Check for any Managed Configurations already pushed by MDM/EMM
        restrictionsListener = new RestrictionsManagerListener();
        restrictionsListener.checkAndApplyRestrictions(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh both lists when activity resumes
        viewModel.refreshAll();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Register for Managed Configuration updates
        if (restrictionsListener != null) {
            restrictionsListener.register(this);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unregister to prevent memory leaks
        if (restrictionsListener != null) {
            restrictionsListener.unregister(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cleanup resources
        if (restrictionsListener != null) {
            restrictionsListener.shutdown();
        }
    }

    private void initViews() {
        // Tab buttons
        btnTabActive = findViewById(R.id.btn_tab_active);
        btnTabBlocked = findViewById(R.id.btn_tab_blocked);

        // Active list views
        sectionActive = findViewById(R.id.section_active);
        recyclerViewActive = findViewById(R.id.recycler_view_active);

        // Blocked list views
        sectionBlocked = findViewById(R.id.section_blocked);
        recyclerViewBlocked = findViewById(R.id.recycler_view_blocked);
        etVendorId = findViewById(R.id.et_vendor_id);
        etProductId = findViewById(R.id.et_product_id);
        etDeviceName = findViewById(R.id.et_device_name);
        btnBlockDevice = findViewById(R.id.btn_block_device);

        btnBlockDevice.setOnClickListener(v -> blockDeviceByVidPid());
    }

    private void setupAdapters() {
        // Setup Active Devices Adapter (with Block button)
        activeAdapter = new DeviceAdapter();
        recyclerViewActive.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewActive.setAdapter(activeAdapter);

        activeAdapter.setOnItemClickListener(usbDevice -> {
            viewModel.blockDevice(usbDevice);
            Toast.makeText(this, "Device blocked", Toast.LENGTH_SHORT).show();
        });

        // Setup Blocked Devices Adapter (with Unblock button)
        blockedAdapter = new BlockedDeviceAdapter();
        recyclerViewBlocked.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewBlocked.setAdapter(blockedAdapter);

        blockedAdapter.setOnItemClickListener(entity -> {
            viewModel.unblockDevice(entity);
            Toast.makeText(this, "Device unblocked", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupTabSwitching() {
        // Initially show Active tab
        switchToActiveTab();

        btnTabActive.setOnClickListener(v -> switchToActiveTab());
        btnTabBlocked.setOnClickListener(v -> switchToBlockedTab());
    }

    private void switchToActiveTab() {
        sectionActive.setVisibility(View.VISIBLE);
        sectionBlocked.setVisibility(View.GONE);
        btnTabActive.setBackgroundColor(getResources().getColor(android.R.color.darker_gray, getTheme()));
        btnTabBlocked.setBackgroundColor(Color.TRANSPARENT);
    }

    private void switchToBlockedTab() {
        sectionActive.setVisibility(View.GONE);
        sectionBlocked.setVisibility(View.VISIBLE);
        btnTabActive.setBackgroundColor(Color.TRANSPARENT);
        btnTabBlocked.setBackgroundColor(getResources().getColor(android.R.color.darker_gray, getTheme()));
    }

    private void observeViewModel() {
        // Observe Active List (physical devices filtered against blacklist)
        viewModel.getActiveDevices().observe(this, usbDevices -> {
            if (usbDevices != null) {
                activeAdapter.submitList(usbDevices);
            }
        });

        // Observe Blocked List (all devices in Room blacklist)
        viewModel.getBlockedDevices().observe(this, blockedDevices -> {
            if (blockedDevices != null) {
                blockedAdapter.submitList(blockedDevices);
            }
        });
    }

    /**
     * Manually block a device by VID/PID.
     * This is useful for pre-blocking devices that aren't currently connected.
     */
    private void blockDeviceByVidPid() {
        String vidStr = etVendorId.getText().toString().trim();
        String pidStr = etProductId.getText().toString().trim();
        String deviceName = etDeviceName.getText().toString().trim();

        if (vidStr.isEmpty() || pidStr.isEmpty()) {
            Toast.makeText(this, "Please enter VID and PID", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int vendorId;
            int productId;

            // Support both decimal and hex input (e.g., "1234" or "0x1234" or "1234" as hex)
            if (vidStr.startsWith("0x") || vidStr.startsWith("0X")) {
                vendorId = Integer.parseInt(vidStr.substring(2), 16);
            } else {
                // Try parsing as hex first (common for USB IDs)
                try {
                    vendorId = Integer.parseInt(vidStr, 16);
                } catch (NumberFormatException e) {
                    vendorId = Integer.parseInt(vidStr, 10);
                }
            }

            if (pidStr.startsWith("0x") || pidStr.startsWith("0X")) {
                productId = Integer.parseInt(pidStr.substring(2), 16);
            } else {
                try {
                    productId = Integer.parseInt(pidStr, 16);
                } catch (NumberFormatException e) {
                    productId = Integer.parseInt(pidStr, 10);
                }
            }

            if (deviceName.isEmpty()) {
                deviceName = "Pre-blocked Device";
            }

            // Block the device by adding it to the blacklist
            viewModel.blockDeviceByVidPid(vendorId, productId, deviceName);

            // Clear input fields
            etVendorId.setText("");
            etProductId.setText("");
            etDeviceName.setText("");

            Toast.makeText(this, String.format("Device blocked: VID=0x%04X, PID=0x%04X",
                    vendorId, productId), Toast.LENGTH_SHORT).show();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid VID or PID. Use decimal or hex (e.g., 1234 or 0x1234)",
                    Toast.LENGTH_SHORT).show();
        }
    }
}
package com.sabir.usbsentinel.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sabir.usbsentinel.R;
import com.sabir.usbsentinel.data.entity.UsbDeviceEntity;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private UsbDeviceViewModel viewModel;
    private DeviceAdapter adapter;

    private EditText etVendorId;
    private EditText etProductId;
    private EditText etDeviceName;
    private Button btnAddDevice;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewModel = new ViewModelProvider(this).get(UsbDeviceViewModel.class);

        initViews();
        setupRecyclerView();
        observeViewModel();
    }

    private void initViews() {
        etVendorId = findViewById(R.id.et_vendor_id);
        etProductId = findViewById(R.id.et_product_id);
        etDeviceName = findViewById(R.id.et_device_name);
        btnAddDevice = findViewById(R.id.btn_add_device);
        recyclerView = findViewById(R.id.recycler_view);

        btnAddDevice.setOnClickListener(v -> addDevice());
    }

    private void setupRecyclerView() {
        adapter = new DeviceAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(device -> {
            viewModel.deleteDevice(device);
            Toast.makeText(this, "Device deleted", Toast.LENGTH_SHORT).show();
        });
    }

    private void observeViewModel() {
        viewModel.getDeviceList().observe(this, devices -> {
            if (devices != null) {
                adapter.submitList(devices);
            }
        });
    }

    private void addDevice() {
        String vidStr = etVendorId.getText().toString().trim();
        String pidStr = etProductId.getText().toString().trim();
        String deviceName = etDeviceName.getText().toString().trim();

        if (vidStr.isEmpty() || pidStr.isEmpty() || deviceName.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int vendorId = Integer.parseInt(vidStr);
            int productId = Integer.parseInt(pidStr);

            viewModel.insertDevice(vendorId, productId, deviceName, false);

            etVendorId.setText("");
            etProductId.setText("");
            etDeviceName.setText("");

            Toast.makeText(this, "Device added", Toast.LENGTH_SHORT).show();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid VID or PID", Toast.LENGTH_SHORT).show();
        }
    }
}
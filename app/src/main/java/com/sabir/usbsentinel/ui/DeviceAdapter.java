package com.sabir.usbsentinel.ui;

import android.hardware.usb.UsbDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.sabir.usbsentinel.R;

import java.util.Objects;

public class DeviceAdapter extends ListAdapter<UsbDevice, DeviceAdapter.DeviceViewHolder> {

    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(UsbDevice usbDevice);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public DeviceAdapter() {
        super(new DiffUtil.ItemCallback<UsbDevice>() {
            @Override
            public boolean areItemsTheSame(@NonNull UsbDevice oldItem, @NonNull UsbDevice newItem) {
                return Objects.equals(oldItem.getDeviceName(), newItem.getDeviceName());
            }

            @Override
            public boolean areContentsTheSame(@NonNull UsbDevice oldItem, @NonNull UsbDevice newItem) {
                return oldItem.getVendorId() == newItem.getVendorId() &&
                        oldItem.getProductId() == newItem.getProductId() &&
                        Objects.equals(oldItem.getDeviceName(), newItem.getDeviceName());
            }
        });
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_device, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        UsbDevice usbDevice = getItem(position);
        holder.bind(usbDevice);
    }

    class DeviceViewHolder extends RecyclerView.ViewHolder {
        private TextView tvDeviceName;
        private TextView tvVidPid;
        private Button btnAction;

        public DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDeviceName = itemView.findViewById(R.id.tv_device_name);
            tvVidPid = itemView.findViewById(R.id.tv_vid_pid);
            btnAction = itemView.findViewById(R.id.btn_action);

            btnAction.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onItemClick(getItem(position));
                }
            });
        }

        public void bind(UsbDevice usbDevice) {
            String deviceName = usbDevice.getDeviceName();
            if (deviceName == null || deviceName.isEmpty()) {
                deviceName = "Unknown USB Device";
            }
            tvDeviceName.setText(deviceName);
            tvVidPid.setText(String.format("VID: 0x%04X | PID: 0x%04X",
                    usbDevice.getVendorId(), usbDevice.getProductId()));

            // All displayed devices are allowed, so button always says "Block"
            btnAction.setText("Block");
        }
    }
}
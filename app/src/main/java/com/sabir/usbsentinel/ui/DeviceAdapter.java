package com.sabir.usbsentinel.ui;

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
import com.sabir.usbsentinel.data.entity.UsbDeviceEntity;

public class DeviceAdapter extends ListAdapter<UsbDeviceEntity, DeviceAdapter.DeviceViewHolder> {

    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(UsbDeviceEntity device);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public DeviceAdapter() {
        super(new DiffUtil.ItemCallback<UsbDeviceEntity>() {
            @Override
            public boolean areItemsTheSame(@NonNull UsbDeviceEntity oldItem, @NonNull UsbDeviceEntity newItem) {
                return oldItem.getId() == newItem.getId();
            }

            @Override
            public boolean areContentsTheSame(@NonNull UsbDeviceEntity oldItem, @NonNull UsbDeviceEntity newItem) {
                return oldItem.getDeviceName().equals(newItem.getDeviceName()) &&
                        oldItem.getVendorId() == newItem.getVendorId() &&
                        oldItem.getProductId() == newItem.getProductId() &&
                        oldItem.isBlocked() == newItem.isBlocked();
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
        UsbDeviceEntity device = getItem(position);
        holder.bind(device);
    }

    class DeviceViewHolder extends RecyclerView.ViewHolder {
        private TextView tvDeviceName;
        private TextView tvVidPid;
        private TextView tvStatus;
        private Button btnAction;

        public DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDeviceName = itemView.findViewById(R.id.tv_device_name);
            tvVidPid = itemView.findViewById(R.id.tv_vid_pid);
            tvStatus = itemView.findViewById(R.id.tv_status);
            btnAction = itemView.findViewById(R.id.btn_action);

            btnAction.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onItemClick(getItem(position));
                }
            });
        }

        public void bind(UsbDeviceEntity device) {
            tvDeviceName.setText(device.getDeviceName());
            tvVidPid.setText(String.format("VID: 0x%04X | PID: 0x%04X",
                    device.getVendorId(), device.getProductId()));

            if (device.isBlocked()) {
                tvStatus.setText("Status: Blocked");
                tvStatus.setTextColor(itemView.getContext().getColor(android.R.color.holo_red_dark));
                btnAction.setText("Unblock");
            } else {
                tvStatus.setText("Status: Allowed");
                tvStatus.setTextColor(itemView.getContext().getColor(android.R.color.holo_green_dark));
                btnAction.setText("Block");
            }
        }
    }
}
package com.sabir.usbsentinel.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.sabir.usbsentinel.R;
import com.sabir.usbsentinel.ui.MainActivity;

public class NotificationHelper {

    public static final String CHANNEL_ID = "usb_blocked_channel";
    public static final int BLOCKED_DEVICE_NOTIFICATION_ID = 1001;

    /**
     * Create the notification channel (required for Android O+)
     */
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "USB Device Blocked";
            String description = "Notifications for blocked USB devices";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;

            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    name,
                    importance
            );
            channel.setDescription(description);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Show a notification when a blocked USB device is attached
     */
    public static void showBlockedDeviceNotification(Context context, UsbDevice device) {
        // Create an intent to open the app when the notification is tapped
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE
        );

        String deviceName = device.getDeviceName();
        if (deviceName == null || deviceName.isEmpty()) {
            deviceName = "Unknown Device";
        }

        String contentText = String.format(
                "Device blocked - VID: 0x%04X, PID: 0x%04X",
                device.getVendorId(),
                device.getProductId()
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setContentTitle("USB Device Blocked")
                .setContentText(contentText)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(contentText + "\n" + deviceName))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        try {
            notificationManager.notify(BLOCKED_DEVICE_NOTIFICATION_ID, builder.build());
        } catch (SecurityException e) {
            // POST_NOTIFICATIONS permission not granted
            // Fallback: Could use Toast or log in production
        }
    }
}
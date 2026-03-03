package com.sabir.usbsentinel.data.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.sabir.usbsentinel.data.dao.UsbDeviceDao;
import com.sabir.usbsentinel.data.entity.UsbDeviceEntity;

@Database(entities = {UsbDeviceEntity.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;

    public abstract UsbDeviceDao usbDeviceDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    AppDatabase.class,
                    "usb_sentinel_database"
            ).allowMainThreadQueries()
             .fallbackToDestructiveMigration()
             .build();
        }
        return instance;
    }
}
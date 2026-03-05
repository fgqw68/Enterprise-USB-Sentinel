package com.sabir.usbsentinel.data.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.sabir.usbsentinel.data.dao.UsbDeviceDao;
import com.sabir.usbsentinel.data.entity.UsbDeviceEntity;

@Database(entities = {UsbDeviceEntity.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;

    public abstract UsbDeviceDao usbDeviceDao();

    // Migration from version 1 to 2: Remove isBlocked column and keep only blocked devices
    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Create new table without isBlocked column
            database.execSQL(
                    "CREATE TABLE usb_devices_new (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "vendorId INTEGER NOT NULL, " +
                    "productId INTEGER NOT NULL, " +
                    "deviceName TEXT NOT NULL)"
            );

            // Copy only blocked devices (isBlocked = 1)
            database.execSQL(
                    "INSERT INTO usb_devices_new (id, vendorId, productId, deviceName) " +
                    "SELECT id, vendorId, productId, deviceName " +
                    "FROM usb_devices WHERE isBlocked = 1"
            );

            // Drop old table and rename new table
            database.execSQL("DROP TABLE usb_devices");
            database.execSQL("ALTER TABLE usb_devices_new RENAME TO usb_devices");
        }
    };

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    AppDatabase.class,
                    "usb_sentinel_database"
            )//.allowMainThreadQueries()
             .addMigrations(MIGRATION_1_2)
             .fallbackToDestructiveMigration()
             .build();
        }
        return instance;
    }
}
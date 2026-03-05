# 🛡️ USB Sentinel: Enterprise-Grade Hardware Security

## 📦 Product Overview
USB Sentinel is a specialized Android system utility designed for high-security environments (clinics, hospitals, and corporate offices). Its primary function is to **detect, identify, and block unauthorized USB peripherals** (flash drives, keyloggers, unauthorized input devices) from interacting with the host Android device.

It acts as a digital gatekeeper, ensuring that only "Whitelisted" or "Admin-Approved" hardware can communicate with the system.

## ✨ Core Features & Functionality

* **Real-time Peripheral Monitoring:** Uses a high-priority `UsbReceiver` to intercept hardware attachment events the moment a device is plugged in.
* **MDM-Driven Policy Enforcement:** Integrates with Enterprise Mobility Management (EMM) consoles (like Microsoft Intune) to push global blocklists to an entire fleet of devices simultaneously.
* **Dynamic Blacklist Management:** A local UI allowing authorized staff to manually add or remove devices based on Vendor ID (VID) and Product ID (PID).
* **Vendor-Level Peripheral Control:** Interfaces with low-level `VendorApi` to physically toggle the connection state of the USB port.
* **Atomic Security Sync:** A "Master Override" logic that ensures the device security state is updated in a single, unbreakable transaction.


### 1. Atomic "Master Override" Synchronization
I engineered an "all-or-nothing" synchronization pattern for Admin security policies. Using **Room Database Transactions**, the system executes a `Delete-then-Insert` sync to ensure the database never exists in a partial or insecure state.
* **Problem Solved:** Prevented a "security gap" where a restricted device could be authorized during a millisecond-long database update.

### 2. Reactive Repository Pattern & Clean Architecture
* **The Ear:** A dynamic `BroadcastReceiver` captures `ACTION_APPLICATION_RESTRICTIONS_CHANGED`.
* **The Brain:** The `UsbDeviceRepository` abstracts the `RestrictionsManager` parsing from the Data Access Layer.
* **The Flow:** UI components observe `LiveData` streams, ensuring the local UI updates instantly when an Admin pushes a policy.

---

## ⚠️ System Limitations & Constraints

* **Host Mode Requirement:** The application requires the Android device to support **USB Host Mode** (OTG) to identify and interact with connected peripherals.
* **Platform Security (Android 10+):** Due to modern Android privacy restrictions, the app requires specific system-level permissions (or Device Owner status) to silently block certain HID (Human Interface Devices) without user prompts.
* **Hardware-Specific Blocking:** The "Hard Blocking" feature is dependent on the device's kernel and the availability of a `VendorApi`. On generic consumer devices, the app functions as a high-level permission gatekeeper.
* **Power Consumption:** While optimized via `SingleThreadExecutor`, constant monitoring of the USB bus carries a negligible but non-zero impact on battery life in extreme "high-traffic" hardware environments.

## 🛠️ Technical Stack

* **Language:** Java / Android SDK
* **Persistence:** Room Persistence Library (SQLite) with Migration versioning.
* **Concurrency:** Java ExecutorService (SingleThread & FixedPool).
* **Architecture:** MVVM / Repository Pattern / Jetpack (LiveData, ViewModel).
* **Enterprise:** Android RestrictionsManager & IPC via Parcelable Arrays.

## 👨‍💻 Developed By
**Dr. Sabir VT.**
*Senior Developer*
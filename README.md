# Secure Dialer 📞 

[![License: MIT](https://img.shields.io/badge/License-MIT-blue?style=flat-square)](LICENSE)
[![Kotlin Version](https://img.shields.io/badge/Kotlin-2.0.0-purple?style=flat-square&logo=kotlin)](https://kotlinlang.org/)
[![Android SDK](https://img.shields.io/badge/Android-SDK%2024%20to%2036-green?style=flat-square&logo=android)](https://developer.android.com)
[![F-Droid Compatible](https://img.shields.io/badge/F--Droid-Compatible-brightgreen?style=flat-square&logo=f-droid)](https://f-droid.org)
[![Security Rating](https://img.shields.io/badge/Security-Privacy--First-darkgreen?style=flat-square&logo=shield)](.github/SECURITY.md)

> **Trust, Clarity, and Uncompromising Privacy.**  
> A lightweight, modern, and offline-first phone dialer built with Jetpack Compose, Material 3, and native Android Framework components.

---

## 🌟 The Philosophy of Secure Phone Apps

In an era of digital surveillance, monetization of telemetry, and commercialization of personal interactions, **Secure Dialer** is built with an absolute, unbreakable promise: **your contacts, call logs, and phone calls are strictly your own business.**

As the flagship application of the **[secure-phone-apps](https://github.com/secure-phone-apps)** ecosystem, Secure Dialer avoids wrapping heavy, untrusted third-party SDKs that bleed telemetry. Instead, it interacts directly and safely with the native Android Operating System. 

The result is a lightning-fast, privacy-hardened app that respects your system resources, preserves your battery, and guarantees that your data stays on your device.

---

## 🔒 Complete Offline & Zero-Internet Guarantee (Auditable)

Unlike standard dialers or commercial caller-ID apps that require full network privileges to function, **Secure Dialer does not request the Android Internet Permission (`android.permission.INTERNET`)**.

This simple, verifiable design makes it **physically impossible** for the app to:
* Transmit your dial history or caller IDs to remote cloud servers.
* Leak contact cards, address details, or user notes.
* Run background advertising SDKs or tracking beacons.
* Participate in shadow profiling.

### How to Verify:
Open [app/src/main/AndroidManifest.xml](app/src/main/AndroidManifest.xml) and inspect the declared permissions. You will find standard telephony permissions, but **absolutely no** `<uses-permission android:name="android.permission.INTERNET" />`.

---

## ✨ Features

* **🛡️ Default Dialer Role:** Seamlessly integrates as the system default call handler via Android's `RoleManager` (API 29+) or legacy system intents.
* **📱 Call Blocking & Screening:** Robust incoming call screening to automatically filter out spam, unknown callers, and blocked contacts on-device without exposing your blocklist to external services.
* **🌟 Smart Favorites:** Easily favorite and toggle key contacts on your dashboard.
* **🕰️ Transactional Call History:** Manage Recents with a granular transactional history layout. Tap any recent caller to view their historical calls and easily delete any single transition or clear everything.
* **🎨 Material 3 Edge-to-Edge Design:** Fully optimized with dynamic light/dark schemas, comfortable 48dp+ accessibility touch-targets, and responsive, fluid Jetpack Compose layouts.
* **📂 Local Address Book Sync:** Integrates directly with standard Android `ContactsContract` using memory-efficient cursors.

---

## 📐 Architecture & Security

Secure Dialer is crafted with clean, modern development patterns:
* **UI Layer:** Jetpack Compose (Material Design 3) following edge-to-edge system insets.
* **Architecture:** Model-View-ViewModel (MVVM) for modular state flows.
* **Persistence:** Android Local Room database to track custom configuration parameters, call blocking, and offline preferences securely.
* **Framework APIs:** Uses direct native Android `TelecomManager`, `InCallService`, and `CallScreeningService` pipelines for standard phone calling operations, avoiding custom, untested background handlers.

---

## 🚀 How to Build and Run

### Prerequisites
* **Android Studio** (Koala or newer recommended)
* **JDK 17**
* **Android SDK 24** (Minimum) to **SDK 36** (Target)

### Build Instructions
1. **Clone the repository:**
   ```bash
   git clone https://github.com/secure-phone-apps/secure-dialer.git
   ```
2. **Open the project** in Android Studio and let Gradle synchronize.
3. **Build the Debug APK:**
   ```bash
   ./gradlew assembleDebug
   ```
4. **Deploy** to your physical Android device.
5. Set **Secure Dialer** as your default calling app to activate background screening, interactive calling overlays, and logs management.

---

## 🤝 Contributing & Security Reports

We welcome open-source contributions to make the secure mobile ecosystem stronger! Before contributing, please review our [Security Policy](.github/SECURITY.md).

* **Bug Reports & Features:** Please use our standardized templates when opening an [issue](https://github.com/secure-phone-apps/secure-dialer/issues).
* **Vulnerability Disclosure:** Please report security issues privately via GitHub Security Advisories or by emailing `movstore.online@gmail.com`.

---

## 📄 License

This project is licensed under the **MIT License**. It is fully open-source, permissive, and built to be shared. See the [LICENSE](LICENSE) file for the complete text.

---

*Secure Dialer is built and maintained by **[Secure Phone Apps](https://github.com/secure-phone-apps)**. Simple, transparent, offline utility apps for Android.*

# Welcome to the Secure Dialer Wiki 📚

Welcome to the official technical documentation and wiki for **Secure Dialer**—a modern, zero-internet, privacy-first open-source Android calling application built with Jetpack Compose, Kotlin 2.0, and Room Database with SQLCipher encryption.

<p align="center">
  <img src="https://img.shields.io/badge/API-24%2B-22C55E?style=flat&logo=android&logoColor=white&labelColor=15803D" alt="Android API Support" />
  <img src="https://img.shields.io/badge/Kotlin-2.0-8A2BE2?style=flat&logo=kotlin&logoColor=white" alt="Kotlin 2.0" />
  <img src="https://img.shields.io/badge/Compose-M3-4285F4?style=flat&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose Material 3" />
  <img src="https://img.shields.io/github/license/Secure-Phone-apps/Secure-Dialer?style=flat&color=A3E635" alt="Open Source FOSS GPLv3" />
  <a href="https://apps.obtainium.im/add?url=https://github.com/Secure-Phone-apps/Secure-Dialer"><img src="https://img.shields.io/badge/Obtainium-Add_App-00BCD4?style=flat&logo=android&logoColor=white" alt="Install via Obtainium" /></a>
</p>

---

## ⚡ Wiki Quick Navigation

| Documentation Section | Description & Highlights |
| :--- | :--- |
| 🏆 **[[Wall of Honor|Wall of Honor (Sponsors)]]** | Automated showcase of generous community sponsors across our four support tiers. |
| 📦 **[[Installation and Obtainium Guide|Installation & Obtainium Guide]]** | Step-by-step setup with Obtainium auto-updates, CPU architecture APK selection, and SHA-256 verification. |
| ❓ **[[FAQ and Troubleshooting|FAQ & Troubleshooting]]** | Dual-SIM routing, setting default dialer on Samsung/Pixel/Xiaomi, GrapheneOS/LineageOS compatibility, and backup/restore. |
| 🔑 **[[Permissions and Privacy Explained|Permissions & Privacy Breakdown]]** | Transparent review of Android permissions, dynamic runtime requests, and strict exclusion of `INTERNET`. |
| 🛡️ **[[Security and Encryption Architecture|Security & Encryption Architecture]]** | AES-256 GCM database encryption, Android KeyStore, hardware TEE/StrongBox, and offline sandboxing. |

---

## 🌟 Core Features & Highlights

* **🔒 Zero-Internet Security:** Compiled without the `android.permission.INTERNET` permission. Your call history, contacts database, and dialing activity physically cannot leave your device.
* **⚡ Lightning-Fast T9 Search:** Dialpad search algorithm supports instant contact lookups by name, phone number, or initials.
* **🛡️ Local Encrypted Contacts Vault:** Choose to store sensitive contacts in an isolated, SQLCipher-encrypted vault hidden from other apps.
* **🚫 Offline Call Screening & Spam Rejection:** Block nuisance calls and unwanted numbers using Android's native `CallScreeningService` without cloud tracking.
* **📶 Dual-SIM Smart Selector:** Full support for multi-SIM Android devices with automatic carrier preference memory and manual toggles.
* **🎨 Material You Dynamic Theming:** Fluid Jetpack Compose Material Design 3 interface with true AMOLED dark theme support.

---

## 🛠️ Technology Stack Overview

| Layer | Component / Framework |
| :--- | :--- |
| **Language** | Kotlin 2.0+ with Coroutines & StateFlow |
| **UI Engine** | Jetpack Compose (Material Design 3) |
| **Database** | Room Persistence Library + SQLCipher (AES-256 GCM) |
| **Key Management** | Android KeyStore System Provider (Hardware-backed TEE / StrongBox) |
| **Telephony Integration** | Android TelecomManager, InCallService, CallScreeningService |
| **Target Platforms** | Android 7.0+ (API 24 to 35+), GrapheneOS, CalyxOS, LineageOS |

---

## 💬 Community & Support

* 🐙 **GitHub Repository:** [Secure-Phone-apps/Secure-Dialer](https://github.com/Secure-Phone-apps/Secure-Dialer)
* 💬 **Community Discussions:** [GitHub Discussions](https://github.com/Secure-Phone-apps/Secure-Dialer/discussions)
* 🐛 **Report Issues:** [GitHub Issue Tracker](https://github.com/Secure-Phone-apps/Secure-Dialer/issues)
* 💖 **Sponsor Project:** [GitHub Sponsors](https://github.com/sponsors/Secure-Phone-apps)

---

📍 **Quick Links:** [[Wall of Honor]] | [[Installation and Obtainium Guide]] | [[FAQ and Troubleshooting]] | [[Permissions and Privacy Explained]] | [[Security and Encryption Architecture]]



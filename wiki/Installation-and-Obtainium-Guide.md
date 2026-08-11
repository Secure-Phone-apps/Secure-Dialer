# 📦 Installation & Obtainium Update Guide

Secure Dialer is distributed directly as signed, open-source Android Application Packages (APKs). Because Secure Dialer does not connect to the internet, installing and updating via open-source tools like **Obtainium** or manual releases provides maximum privacy and control.

---

## 🚀 Method 1: Automatic App Updates via Obtainium (Recommended)

[Obtainium](https://github.com/ImranR98/Obtainium) is an open-source Android app manager that allows you to install and automatically receive update notifications directly from GitHub Releases—**without relying on Google Play Store or proprietary services**.

<p align="center">
  <a href="https://apps.obtainium.im/add?url=https://github.com/Secure-Phone-apps/Secure-Dialer">
    <img src="https://raw.githubusercontent.com/ImranR98/Obtainium/main/assets/graphics/badge_obtainium.png" height="52" alt="Get it on Obtainium" />
  </a>
</p>

### Step-by-Step Obtainium Setup:
1. Download and install **[Obtainium](https://github.com/ImranR98/Obtainium)** on your Android device.
2. Open Obtainium and tap **Add App**.
3. Enter the official GitHub repository URL:
   ```text
   https://github.com/Secure-Phone-apps/Secure-Dialer
   ```
4. Tap **Add**. Obtainium will analyze the release assets, select the optimal CPU architecture APK for your smartphone, and notify you automatically whenever a new version is released!

> [!TIP]
> **One-Click Add:** If you are reading this page on your Android phone with Obtainium installed, simply tap the **[Get It On Obtainium](https://apps.obtainium.im/add?url=https://github.com/Secure-Phone-apps/Secure-Dialer)** button above!

---

## 📱 Method 2: Manual Direct APK Download

You can download compiled and cryptographically signed APK files directly from our **[GitHub Releases Page](https://github.com/Secure-Phone-apps/Secure-Dialer/releases)**.

### CPU Architecture Selection Guide:

| APK File Name | CPU Target | Applicable Device Examples |
| :--- | :--- | :--- |
| `app-arm64-v8a-release.apk` | **64-bit ARM** (`arm64-v8a`) | **95% of modern smartphones** (Google Pixel 6/7/8/9, Samsung Galaxy S20–S24, OnePlus, Xiaomi, Nothing Phone) |
| `app-armeabi-v7a-release.apk` | **32-bit ARM** (`armeabi-v7a`) | Older Android phones & entry-level budget devices |
| `app-x86_64-release.apk` | **64-bit Intel/AMD** (`x86_64`) | Android Studio Emulators, ChromeOS devices, & Android-x86 PCs |
| `app-universal-release.apk` | **Universal Multi-Arch** | Compatible with all Android devices (slightly larger file size) |

---

## 🔒 Method 3: Cryptographic SHA-256 Checksum Verification

To ensure your downloaded APK has not been modified or corrupted, verify its SHA-256 hash against the official published `checksums.txt` file attached to each GitHub Release.

### Verification Commands:

* **Linux / macOS Terminal:**
  ```bash
  sha256sum app-arm64-v8a-release.apk
  ```
* **Windows PowerShell:**
  ```powershell
  Get-FileHash .\app-arm64-v8a-release.apk -Algorithm SHA256
  ```
* **Android Termux:**
  ```bash
  sha256sum /sdcard/Download/app-arm64-v8a-release.apk
  ```

Compare the calculated 64-character hash string with the value in `checksums.txt`. The output must match identically.

---

## 🛠️ Common Installation Troubleshooting

### 1. "App Not Installed" or "Package Conflict" Error
* **Cause:** Attempting to update or install over an existing app signed with a different key (e.g. debug build vs official release).
* **Fix:** Uninstall any existing or development version of Secure Dialer before installing the official release APK.

### 2. "Blocked by Play Protect" Warning
* **Cause:** Android Google Play Protect flags sideloaded APKs from open-source repositories when they aren't downloaded via the Google Play Store.
* **Fix:** Tap **More Details** -> **Install Anyway**. Secure Dialer contains zero malicious code or network callers.

### 3. "Unknown Apps Permission Denied"
* **Cause:** Android requires explicit user consent before a browser or file manager can install APKs.
* **Fix:** Go to **System Settings** -> **Apps** -> **Special App Access** -> **Install Unknown Apps** and toggle **Allow** for your browser or Obtainium.

---

📍 **Quick Links:** [[Home]] | [[FAQ and Troubleshooting|FAQ & Troubleshooting]] | [[Permissions and Privacy Explained|Permissions & Privacy]] | [[Security and Encryption Architecture|Security Architecture]]



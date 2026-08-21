# 📦 Installation & Obtainium Guide

Hey everyone! Here is a simple guide on how to install **Secure Dialer**, set up automatic update notifications with **Obtainium**, and pick the right APK file for your phone.

---

## 🚀 Method 1: Auto-Updates with Obtainium (Recommended)

[Obtainium](https://github.com/ImranR98/Obtainium) is an open-source Android app that lets you download and receive automatic updates directly from our GitHub Releases without needing the Google Play Store.

<p align="center">
  <a href="https://apps.obtainium.imranr.dev/add?url=https://github.com/Secure-Phone-apps/Secure-Dialer">
    <img src="https://raw.githubusercontent.com/ImranR98/Obtainium/main/assets/graphics/badge_obtainium.png" height="52" alt="Get it on Obtainium" />
  </a>
</p>

### How to set it up:
1. Download and install **[Obtainium](https://github.com/ImranR98/Obtainium)** on your Android device.
2. Open Obtainium and tap **Add App**.
3. Paste our GitHub repository URL:
   ```text
   https://github.com/Secure-Phone-apps/Secure-Dialer
   ```
4. Tap **Add**. Obtainium will automatically detect new releases, select the best APK for your phone processor, and notify you when an update is available!

---

## 📱 Method 2: Direct APK Download from GitHub Releases

You can also download signed APK files directly from our **[GitHub Releases Page](https://github.com/Secure-Phone-apps/Secure-Dialer/releases)**.

### Which APK file should you choose?

| APK File Name | Which Phones Is It For? |
| :--- | :--- |
| **`secure-dialer-v1.3.0-arm64-v8a.apk`** | **Most modern Android phones** (Google Pixel, Samsung Galaxy, OnePlus, Xiaomi, Motorola from the last 6+ years). **Download this one if you are unsure!** |
| **`secure-dialer-v1.3.0-armeabi-v7a.apk`** | **Older 32-bit Android phones** and entry-level budget phones. |
| **`secure-dialer-v1.3.0-x86_64.apk`** | **64-bit Emulators**, ChromeOS, or Android-x86 PC setups. |
| **`secure-dialer-v1.3.0-universal.apk`** | **Universal build** that runs on all Android devices. |

---

## 🔒 Verifying APK Checksums (SHA-256)

If you want to verify that your downloaded APK is genuine and untampered, you can check its SHA-256 hash against the `checksums.txt` file attached to each release:

* **Linux / Mac Terminal:**
  ```bash
  sha256sum secure-dialer-v1.3.0-arm64-v8a.apk
  ```
* **Windows PowerShell:**
  ```powershell
  Get-FileHash .\secure-dialer-v1.3.0-arm64-v8a.apk -Algorithm SHA256
  ```

---

## 🛠️ Quick Troubleshooting Tips

* **"App Not Installed" Error:** If you previously had a debug build or older version signed with a different key, uninstall it first before installing the official release APK.
* **"Blocked by Play Protect" Warning:** Since the app is downloaded from GitHub and not the Play Store, tap **More Details -> Install Anyway**. Secure Dialer is 100% open source and contains zero network or tracking code.
* **"Unknown Apps" Prompt:** When downloading via browser or Obtainium, make sure you allow "Install Unknown Apps" in your device's system settings.

---

📍 **Quick Links:** [[Home]] | [[FAQ and Troubleshooting]] | [[Permissions and Privacy Explained]] | [[Security and Encryption Architecture]] | [[Wall of Honor]]

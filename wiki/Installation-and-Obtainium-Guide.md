# 📦 Installation & Obtainium Update Guide

Secure Dialer is distributed directly as open-source Android Application Packages (APKs) signed with official release keys.

---

## 🚀 Method 1: Automatic Updates via Obtainium (Recommended)

[Obtainium](https://github.com/ImranR98/Obtainium) is an open-source Android app that allows you to install and automatically receive update notifications directly from GitHub Releases—without relying on proprietary app stores.

### Quick Setup Steps:
1. Download and install **[Obtainium](https://github.com/ImranR98/Obtainium)** on your Android device.
2. Open Obtainium and tap **Add App**.
3. Copy and paste the GitHub repository URL:
   ```text
   https://github.com/Secure-Phone-apps/Secure-Dialer
   ```
4. Tap **Add**. Obtainium will automatically detect the latest release, download the correct APK for your CPU architecture, and alert you whenever new updates are released!

---

## 📱 Method 2: Manual Direct APK Download

You can download signed APK files directly from our **[GitHub Releases Page](https://github.com/Secure-Phone-apps/Secure-Dialer/releases)**.

### Architecture Selection Guide:
Choose the APK file tailored to your smartphone's CPU architecture:

| APK File Name | Processor Architecture | Typical Phone Models |
| :--- | :--- | :--- |
| `app-arm64-v8a-release.apk` | 64-bit ARM (arm64-v8a) | **95% of modern phones** (Pixel 6-8, Samsung S20-S24, Xiaomi, OnePlus) |
| `app-armeabi-v7a-release.apk` | 32-bit ARM (armeabi-v7a) | Older budget smartphones & legacy devices |
| `app-x86_64-release.apk` | 64-bit Intel/AMD | Android Emulators & x86 Chromebooks / PCs |
| `app-universal-release.apk` | Universal Multi-Arch | Compatible with all devices (slightly larger file size) |

---

## 🔒 Method 3: Verifying SHA-256 Checksums

For security-conscious users who want to verify the cryptographic authenticity of downloaded APK files:

1. Download `checksums.txt` alongside your APK file from the GitHub release page.
2. Open a terminal / command prompt on your computer and run:
   * **Linux / macOS:**
     ```bash
     sha256sum app-arm64-v8a-release.apk
     ```
   * **Windows PowerShell:**
     ```powershell
     Get-FileHash app-arm64-v8a-release.apk -Algorithm SHA256
     ```
3. Compare the generated 64-character hash string with the value published in `checksums.txt`. They must match exactly!

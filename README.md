# Secure Dialer 📞 — Pure, Private, & Offline-First Open-Source Android Dialer App

![Secure Dialer Hero Banner](assets/secure_dialer_hero.jpg)

<p align="center">
  <img src="assets/screenshots/dialpad.jpg" width="31%" alt="Secure Dialer - Modern Android T9 dialpad interface with predictive contact search, speed dial, and Material You design." />
  <img src="assets/screenshots/contacts.jpg" width="31%" alt="Secure Dialer - Local and system contacts manager screen to add, edit, or delete contacts with privacy-first offline storage." />
  <img src="assets/screenshots/recents.jpg" width="31%" alt="Secure Dialer - Interactive recents call history log displaying incoming, outgoing, and missed calls with quick action buttons." />
</p>
<p align="center">
  <img src="assets/screenshots/call_log.jpg" width="31%" alt="Secure Dialer - Advanced call history details screen with callback reminder scheduler, contact summary, and call duration logs." />
  <img src="assets/screenshots/calling.jpg" width="31%" alt="Secure Dialer - Minimalist and eye-safe active outgoing/incoming calling screen UI with Material 3 dynamic color integration." />
  <img src="assets/screenshots/setting.jpg" width="31%" alt="Secure Dialer - App preferences and configuration panel featuring dark mode, dynamic color schemes, and speed dial setup." />
</p>

<p align="center">
  <img src="https://img.shields.io/badge/API-24%2B-22C55E?style=flat&logo=android&logoColor=white&labelColor=15803D" alt="Android API Support" />
  <img src="https://img.shields.io/badge/Kotlin-2.0-8A2BE2?style=flat&logo=kotlin&logoColor=white" alt="Kotlin 2.0" />
  <img src="https://img.shields.io/badge/Compose-M3-4285F4?style=flat&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose Material 3" />
  <img src="https://img.shields.io/github/license/Secure-Phone-apps/Secure-Dialer?style=flat&color=A3E635" alt="Open Source FOSS GPLv3" />
</p>

Welcome to **Secure Dialer**, your reliable, lightning-fast, and privacy-hardened calling companion and contacts manager for Android. 

Commercial phone applications track your dialing habits, harvest your contact lists, and upload personal details to remote clouds for data mining. Secure Dialer is built on a transparent, zero-compromise promise: **your call history, contacts database, and phone activity never leave your own physical device.**

---

## 🔍 Why Choose Secure Dialer over Stock Apps?

Whether you are looking for a secure **Google Dialer alternative**, a cleaner **Samsung Dialer replacement**, a privacy-focused successor to **Simple Dialer / Fossify**, or a completely offline, ad-free alternative to **Truecaller**, Secure Dialer is designed to provide professional utility without surveillance.

### 🛡️ Feature Comparison Matrix

| Feature / Security Parameter | **Secure Dialer (FOSS)** | **Google / Samsung Dialer** | **Truecaller / Commercial** | **Simple / Fossify** |
| :--- | :---: | :---: | :---: | :---: |
| **Open Source (GPLv3)** | **Yes (100% Free)** | No (Proprietary) | No (Proprietary) | Yes (FOSS) |
| **Zero Internet Permission** | **Yes (Fully Offline)** | No (Telemetry Uplinks) | No (Collects & Sells Data) | Yes (Fully Offline) |
| **Offline Spam Blocklist** | **Yes (CallScreeningService)**| Yes (Requires Cloud) | Yes (Requires Contact Upload) | No / Limited |
| **AES-256 Database Encryption** | **Yes (SQLCipher)** | No (Stored in Cleartext) | No (Stored on Remote Servers)| No (Stored in Cleartext) |
| **Hardware Key Security** | **Yes (Android Keystore)** | No | No | No |
| **Fake Call & Escape Simulator** | **Yes (High-Fidelity UI)**| No | No | No |
| **vCard/VCF Import & Export** | **Yes (Offline Backup)** | No (Pushes to Google Cloud) | No | Yes (Basic) |

---

## 🔑 Key Features & Architecture

Built natively from the ground up using modern **Kotlin 2.0**, **Jetpack Compose (Material 3)**, and **Room Database**.

### 1. Smart T9 Dialing & Outgoing Calls
* **T9 Search Integration:** Fast search through your contacts directly from the dialpad (similar to Google and Samsung dialers).
* **Tactile Haptic Dialpad:** Rich, responsive vibration feedback with high-fidelity DTMF tones for navigating automated menus.
* **Quick Clipboard Filters:** Long-press to paste numbers instantly; automatically strips out parentheses, spaces, and unwanted symbols.
* **Speed Dial Keys (1-9):** Long-press keys `1` to `9` to dial your critical contacts or trigger custom numbers immediately.
* **Dual-SIM Capability:** Full SIM-selection prompt (SIM 1 or SIM 2) or pre-configured preferred SIM for automatic routing.

### 2. Deep Security & Privacy Shield
* **No Internet Permission:** Literally **zero internet access** declared in the manifest. There is physical exclusion of network capability, meaning absolutely zero ads, zero telemetry, and zero leaks.
* **Encrypted Storage:** All call notes, blacklist configurations, and custom templates are stored locally under strong **AES-256 encryption** using SQLCipher.
* **Hardware-Backed Keystore:** Encryption keys are tied to the device's hardware enclave (Android Keystore System) to guard your logs against physical extraction.
* **Secure Backups:** Export and import encrypted JSON backups protected via PBKDF2 key derivation (10,000 iterations) and AES-GCM authenticated encryption.

### 3. Integrated Utilities (The Rescue/Escape Suite)
* **🎭 Fake Call Simulator:** Trigger high-fidelity, interactive incoming call screens with custom delay timers (5s, 10s, 30s) and multi-call schedules to exit awkward meetings.
* **📝 In-Call Call Recorder & Notes:** Safely record audio conversations locally or jot down private call notes within the active call interface.
* **👋 Hand Motion Gestures:** Turn face-down to silence a call, or lift-to-ear to automatically pick up an incoming caller.
* **⏰ Call Back Reminders:** Instantly set gentle local reminders to call back missed contacts, complete with direct notification buttons.
* **📊 Visual Call Analytics:** Review clean, beautiful Material 3 analytics and charts showing call durations, frequent dialers, and patterns completely offline.

### 4. Smart Spam Filtering & vCard Migrations
* **Robocall Screening:** Natively silence and reject automated spam callers offline using Android's native `CallScreeningService`.
* **vCard / VCF Native Migrations:** Securely import or export your entire contact directory offline via industry-standard `.vcf` formats.
* **De-duplication engine:** Scan your phone local storage to merge, rename, and clean up repeating or duplicate contact entries effortlessly.

---

## 📋 Standard Android Permissions Explained

To act as your **Default Phone App** and provide native calling capabilities, Secure Dialer requests these Android framework permissions:

| Permission | Functionality | Privacy Context |
| :--- | :--- | :--- |
| **`READ_CONTACTS`** | Populate Address Book | Stays 100% on-device to show names, avatars, and contact cards. |
| **`WRITE_CONTACTS`** | Manage Directory | Allows creation, editing, and deleting contacts locally. |
| **`CALL_PHONE`** | Direct Outgoing Calls | Required to execute phone calls when you tap a number or speed dial. |
| **`READ_CALL_LOG`** | Display History | Builds your Recents feed and missed calls history. |
| **`WRITE_CALL_LOG`** | Clear History | Necessary for deleting single entries or wiping the entire call history. |
| **`MODIFY_AUDIO_SETTINGS`**| Audio Routing | Safely shifts audio output to earpiece, speaker, or paired Bluetooth headsets. |
| **`USE_FULL_SCREEN_INTENT`**| Active Caller UI | Essential to overlay the ringing screen when your phone is locked. |
| **`POST_NOTIFICATIONS`**| Ongoing Call Status | Keeps active call bubbles and controls visible in your pull-down drawer. |
| **`SEND_SMS`** | Quick Replies | Sends instant decline-by-text SMS (e.g., "In a meeting") directly when rejecting a call. |
| **`READ_PHONE_STATE`** | Detect SIM & Lines | Used for dual-SIM detection and handling active line states. |
| **`VIBRATE`** | System Haptics | Powers the physical tactile vibration for key presses and dials. |

---

## 🛠️ Build and Developer Setup

### Prerequisites
* **Android Studio** (Koala or newer recommended)
* **JDK 17**
* **Minimum SDK Support:** Android 5.0 (API 24+) up to Android 15 (API 36)

### How to Compile & Run
1. **Clone the repository:**
   ```bash
   git clone https://github.com/secure-phone-apps/secure-dialer.git
   ```
2. **Open the project** in Android Studio.
3. **Assemble the Debug APK:**
   ```bash
   ./gradlew assembleDebug
   ```
4. Install the generated APK on your device, navigate to your Android device's **Settings -> Apps -> Default Apps**, and set **Secure Dialer** as your primary **Default Phone App** and **Call Screening App**.

---

## 📦 Release APK Guide & Architecture Breakdown

When you download a release from our Releases page, you will see several optimized APK variants. Choose the file matching your phone's processor architecture:

| APK File Name | Target Device & Architecture |
| :--- | :--- |
| **`secure-dialer-v1.3.0-arm64-v8a.apk`** | **Modern 64-bit Android Phones** (Samsung Galaxy, Google Pixel, OnePlus, Xiaomi, Motorola, etc. manufactured in the last 6+ years). **Recommended for 95% of users.** |
| **`secure-dialer-v1.3.0-armeabi-v7a.apk`** | **Older 32-bit Android Devices** & legacy budget phones. |
| **`secure-dialer-v1.3.0-x86.apk`** | **32-bit Emulators** & Intel-based Android tablets/Chromebooks. |
| **`secure-dialer-v1.3.0-x86_64.apk`** | **64-bit Emulators** & PC environments (Windows Subsystem for Android / ChromeOS). |
| **`secure-dialer-v1.3.0-universal.apk`** | **Universal APK** containing binaries for *all* architectures combined into a single file. |

---

## 🤝 Support & Security

* **🛡️ Vulnerability Disclosure:** We maintain a zero-tolerance posture toward security flaws. Please consult our **[Security Policy](SECURITY.md)** to report vulnerabilities securely.
* **📢 Reclaim Privacy:** Help spread the word! Tell your friends and family about offline-first communication tools.
* **💻 Contributions:** Want to add translations, resolve UI issues, or optimize database reads? Read our **[Contributing Guidelines](CONTRIBUTING.md)** and submit a Pull Request!

---

*Secure Dialer is lovingly crafted by the **[Secure Phone Apps](https://github.com/secure-phone-apps)** team. Clean, fast, offline-first mobile utilities for Android.*

---

## ❓ Frequently Asked Questions (FAQ)

<details>
<summary><b>1. Does Secure Dialer require an internet connection?</b></summary>
<br>
No. Secure Dialer operates <b>100% offline</b>. It does not declare the <code>android.permission.INTERNET</code> permission in its manifest, meaning it is physically sandboxed on your device and cannot make network calls. Your contacts, call history, and preferences never leave your physical hardware.
</details>

<details>
<summary><b>2. Is my local database encrypted on the device?</b></summary>
<br>
Yes! Your local database is fully encrypted using <b>SQLCipher</b> and a dynamically generated <b>AES-256 database master key</b>. This master key is generated with cryptographically strong <code>SecureRandom</code> and protected using hardware-backed <b>Android KeyStore</b> (using AES in GCM mode with no padding). Even on rooted devices, your private dialer database is safeguarded against unauthorized extractions.
</details>

<details>
<summary><b>3. Can I use Secure Dialer as my default phone app?</b></summary>
<br>
Yes. Secure Dialer is fully compliant with modern Android requirements to act as your <b>Default Phone Handler</b>. It implements a custom native <code>InCallService</code> and <code>CallManager</code> to fully display incoming/outgoing calling overlays, control speakers, toggle microphones, and process dialing signals locally.
</details>

<details>
<summary><b>4. What is the difference between Local-First and System Contacts?</b></summary>
<br>
Secure Dialer offers an innovative dual-tier contact model:
<ul>
  <li><b>System Contacts</b>: Syncs directly with your Android OS standard contact book (requires standard permissions).</li>
  <li><b>Local-First Contacts</b>: Stored solely inside our app's encrypted SQLite sandbox. They are completely separated from Android's system contact pool. This guarantees that other applications on your device—even those with full system contact permissions—cannot read or harvest these private entries.</li>
</ul>
</details>

<details>
<summary><b>5. Does this app feature T9 predictive dialer search?</b></summary>
<br>
Yes! The Dialpad supports classical <b>T9 predictive keypad search</b>. As you tap digit keys on the keypad, the app's internal repository instantly maps characters into matching keypad numbers (e.g. tapping "564" resolves to "JOH", showing matching names immediately) to find contacts in fractions of a millisecond.
</details>

<details>
<summary><b>6. How do Callback Reminders work without background server networks?</b></summary>
<br>
Instead of relying on heavy cloud push servers that track when you return calls, Secure Dialer leverages standard on-device <b>Android WorkManager</b> or local system alarm scheduler frameworks. Reminders are scheduled locally and trigger localized push alerts directly on your operating system at the exact specified time, requiring zero battery-draining socket connections.
</details>

<details>
<summary><b>7. Does this app support blocking spam calls or unwanted numbers?</b></summary>
<br>
Yes. Secure Dialer contains an offline, on-device SQLite blocklist. If an incoming number matches any entry in your local blocklist, the app automatically declines the call at the system level. This filtering happens entirely on-device without exposing the caller's identity or forwarding telemetry data to any third-party spam-tracking databases.
</details>

<details>
<summary><b>8. Does this app support dual-SIM (Multi-SIM) devices?</b></summary>
<br>
Yes. Secure Dialer fully queries active subscription lines on multi-SIM hardware. When starting an outbound call, the dialer prompts you with a clean dialog to choose the calling SIM, or follows your preset standard carrier SIM preferences established in the system or dialer settings.
</details>

<details>
<summary><b>9. Why does the app request Contacts and Call Log permissions?</b></summary>
<br>
These permissions are <b>strictly essential</b> to fulfill primary telephone handler roles. Android requires <code>READ_CONTACTS</code> to resolve names, <code>READ_CALL_LOG</code> to show your recent calling records, and <code>WRITE_CALL_LOG</code> to let you delete elements. Because our app does not possess network permissions, we guarantee this data cannot be leaked.
</details>

<details>
<summary><b>10. Can I backup, restore, or migrate my offline data?</b></summary>
<br>
Yes! Secure Dialer features a password-protected <b>Local Backup & Export</b> engine inside Settings. This engine compiles your blocklist, configurations, and speed-dials into an encrypted payload using <b>PBKDF2 derivation</b> and <b>AES-GCM encryption</b> keys generated from your custom password. You can export this backup string as a text file and restore it securely on another device.
</details>

<details>
<summary><b>11. Is there a cost, tracking script, or ads inside the app?</b></summary>
<br>
No. Secure Dialer contains <b>zero ads, zero tracking scripts, and zero analytical trackers</b> (such as Firebase Analytics or AdMob). The app is completely free, does not offer premium subscriptions, and respects your privacy absolutely from the core architecture up.
</details>

<details>
<summary><b>12. How can I audit the security of the application?</b></summary>
<br>
The codebase is <b>100% open source under the GPLv3 license</b>. We invite privacy advocates, cybersecurity firms, and open-source developers to fully audit our repository, check the Room SQL database schemas, inspect the KeyStore encryption classes, verify the complete lack of internet permissions, and compile their own build from source.
</details>

---

## 📄 License

This program is free software: you can redistribute it and/or modify it under the terms of the **GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.** See the [LICENSE](LICENSE) file for more details.




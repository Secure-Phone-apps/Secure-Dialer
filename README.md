# Secure Dialer 📞 — Pure, Private, & Offline-First Open-Source Android Dialer App

<p align="center">
  <a href="https://developer.android.com/about/versions/nougat/android-7.0"><img src="https://img.shields.io/badge/API-24%2B-22C55E?style=flat&logo=android&logoColor=white&labelColor=15803D" alt="Android API Support 24+" /></a>
  <a href="https://kotlinlang.org/"><img src="https://img.shields.io/badge/Kotlin-2.0-8A2BE2?style=flat&logo=kotlin&logoColor=white" alt="Kotlin 2.0" /></a>
  <a href="https://developer.android.com/develop/ui/compose"><img src="https://img.shields.io/badge/Compose-M3-4285F4?style=flat&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose Material 3" /></a>
  <a href="https://github.com/Secure-Phone-apps/Secure-Dialer/blob/main/LICENSE"><img src="https://img.shields.io/badge/license-GPL--3.0-A3E635?style=flat&logo=opensourceinitiative&logoColor=white" alt="Open Source FOSS GPLv3" /></a>
  <a href="https://apps.obtainium.imranr.dev/redirect.html?r=obtainium://add/https://github.com/Secure-Phone-apps/Secure-Dialer"><img src="https://img.shields.io/badge/Obtainium-Add_App-00BCD4?style=flat&logo=android&logoColor=white" alt="Install via Obtainium" /></a>
  <a href="https://github.com/Secure-Phone-apps/Secure-Dialer/discussions"><img src="https://img.shields.io/badge/Community-Discussions-1F6FEB?style=flat&logo=github&logoColor=white" alt="GitHub Discussions" /></a>
  <a href="https://github.com/sponsors/Secure-Phone-apps"><img src="https://img.shields.io/badge/Sponsor-GitHub_Sponsors-EA4AAA?style=flat&logo=githubsponsors&logoColor=white" alt="Sponsor Project" /></a>
</p>

<p align="center">
  <a href="https://apps.obtainium.imranr.dev/redirect.html?r=obtainium://add/https://github.com/Secure-Phone-apps/Secure-Dialer">
    <img src="https://raw.githubusercontent.com/ImranR98/Obtainium/main/assets/graphics/badge_obtainium.png" height="80" alt="Get it on Obtainium" />
  </a>
</p>

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

Hey everyone! Welcome to **Secure Dialer**, my open-source, privacy-first, 100% offline Android phone app.

I built this dialer from scratch because I believe your phone dialer is the single most important app on any phone. Every phone call you make or receive should stay completely private and safe between you and the other person. Most pre-installed stock dialers and commercial caller ID apps track your call records, upload your address book to remote servers, and drain your battery with background telemetry and ads. Secure Dialer is built with a simple, honest promise: **zero internet permission, zero ads, zero trackers, and complete on-device privacy.**

```text
┌──────────────────────────────────────────────────────────┐
│                   Android OS Framework                   │
└────────────────────────────┬─────────────────────────────┘
                             │ (Native InCall IPC)
┌────────────────────────────▼─────────────────────────────┐
│             MyInCallService : InCallService              │
└────────────────────────────┬─────────────────────────────┘
                             │
       ┌─────────────────────┼─────────────────────┐
       ▼                     ▼                     ▼
┌──────────────┐     ┌──────────────┐     ┌──────────────────┐
│  Dialer Repo │     │ Room SQLite  │     │ Android KeyStore │
│ (T9 Search)  │     │ (SQLCipher)  │     │ (Hardware Root)  │
└──────────────┘     └──────────────┘     └──────────────────┘
```

---

## 💡 The Backstory: Why I Built This

I am not originally from an Android development background, but I was looking for a dialer that is lightweight, super fast, privacy-oriented, and beautiful. I kept searching for an open-source dialer that fit all my everyday needs, but couldn't find one that felt completely right.

There are great open-source dialers out there like Fossify Dialer, Koler, and others—everyone of them is good enough and has their own features and use cases. 

What I tried to make is a dialer that is **fully functional, secure, fast, and lightweight**, while bringing the clean simplicity of **Material 3 and Material 3 Expressive** into your daily phone calls. 

I wanted an app that:
* **Works flawlessly on modern flagship phones (Android 14, 15, 16)** with expressive dynamic colors and smooth 120Hz animations.
* **Runs smoothly on older devices (Android 9, 10, 11, 12)** that we all have at home and don't want to throw away.
* **Is simple enough for elders and parents to use without confusion**, modern for younger users, and packed with practical tools for power users.


---

## 🛡️ Quick Comparison: Secure Dialer vs Other Options

| Feature / Security Point | **Secure Dialer (FOSS)** | **Google / Samsung Dialer** | **Truecaller / Commercial** | **Simple / Fossify** |
| :--- | :---: | :---: | :---: | :---: |
| **100% Free & Open Source (GPLv3)** | **Yes** | No (Closed source) | No (Closed source) | Yes |
| **Zero Internet Permission** | **Yes (100% Offline)** | No (Background telemetry) | No (Uploads contacts) | Yes |
| **Local Offline Spam Screening** | **Yes (CallScreeningService)** | Needs Cloud Sync | Needs Cloud & Upload | Limited |
| **On-Device Database Encryption** | **Yes (SQLCipher AES-256)** | Plaintext SQLite | Stored on Cloud Servers | Plaintext SQLite |
| **Hardware Key Protection** | **Yes (Android KeyStore)** | No | No | No |
| **Emergency Fake Call Simulator** | **Yes (Built-in)** | No | No | No |
| **Material 3 Expressive UI** | **Yes (Jetpack Compose)** | Stock Material | Cluttered / Ads | Classic M2 / M3 |
| **Works on Older & Newer Phones** | **Yes (API 24 to 36)** | OEM Restricted | Heavy resource usage | Yes |

---

## ⚡ What Secure Dialer Brings to You

Built natively with **Kotlin 2.0**, **Jetpack Compose (Material 3)**, and **Room Database with SQLCipher**:

### 1. Smart T9 Dialpad & Outgoing Calls
* **Fast T9 Predictive Search:** Search your contacts in milliseconds right from the dialpad by spelling names or dialing numbers.
* **Tactile Haptic Engine:** Crisp, responsive vibration feedback and classic DTMF tones.
* **Smart Clipboard Paste:** Long-press to paste phone numbers directly; automatically strips out dashes, spaces, and formatting brackets.
* **Quick Speed Dial (Keys 1-9):** Long-press any digit from 1 to 9 to instantly call your favorite contacts or emergency numbers.
* **Full Dual-SIM Support:** Multi-SIM prompt on outgoing calls with customizable default SIM preference per contact or carrier.

### 2. Deep Privacy & Hardware Security
* **Zero Internet Access:** The app does not declare `android.permission.INTERNET` in its manifest. It is physically impossible for the app to send your data anywhere. No ads, no telemetry, no tracking SDKs.
* **Encrypted Database:** Custom call notes, speed dials, and blacklists are stored locally on-device using strong **AES-256 SQLCipher encryption**.
* **Android KeyStore Protection:** Database encryption keys are sealed inside your device's hardware enclave (TEE / StrongBox).
* **Encrypted Backups:** Export and import your configurations and blocklists with password protection using **PBKDF2 (10,000 rounds)** and **AES-GCM**.

### 3. Practical Everyday Utilities
* **🎭 Fake Call Simulator:** Need to politely excuse yourself from an awkward meeting or situation? Trigger a realistic, customizable incoming call screen with a timer (5s, 10s, 30s) that rings and looks just like a real call.
* **📝 In-Call Notes & Local Recorder:** Take private notes during phone calls and save them encrypted locally.
* **👋 Motion Gestures:** Flip phone face-down to silence incoming rings, or raise-to-ear to answer incoming calls automatically.
* **⏰ Call Back Reminders:** Schedule local alarms to remind you to return missed calls without relying on cloud notification servers.
* **📊 Visual Call Summary:** Review clean, offline statistics and charts of your calling habits directly inside the app.

### 4. Smart Spam Blocker & Clean Contacts
* **Native Call Screening:** Block unwanted callers, robocalls, and hidden numbers offline in under 5ms using Android's native `CallScreeningService`.
* **vCard / VCF Backup & Restore:** Import and export standard `.vcf` contact files completely offline.
* **Duplicate Contact Cleaner:** Find and merge repeating phone numbers and duplicate contacts with one tap.

---

## 📋 Android Permissions & Why We Need Them

To work properly as your **Default Phone App**, Android requires standard telephony permissions. Because Secure Dialer has **zero internet permission**, your data stays 100% on your device:

| Permission | What It Does | Why It Is Safe |
| :--- | :--- | :--- |
| **`READ_CONTACTS`** | Shows your contacts in the dialpad and contacts tab. | Reads locally on your phone; never uploaded anywhere. |
| **`WRITE_CONTACTS`** | Lets you add, edit, or delete contacts directly in the app. | Updates only your local address book. |
| **`CALL_PHONE`** | Places phone calls when you tap a number or speed dial. | Connects directly through your carrier SIM. |
| **`READ_CALL_LOG`** | Shows your recent calls, missed calls, and call history. | Kept strictly on your phone. |
| **`WRITE_CALL_LOG`** | Lets you clear call history entries or delete old logs. | Modifies local logs on your device only. |
| **`MODIFY_AUDIO_SETTINGS`**| Switches audio between earpiece, speakerphone, and Bluetooth headsets. | Standard audio routing for active calls. |
| **`USE_FULL_SCREEN_INTENT`**| Wakes up your screen and shows the incoming call screen when locked. | Needed so you never miss an incoming call. |
| **`POST_NOTIFICATIONS`**| Shows ongoing call controls and missed call badges in your status bar. | Local system notifications only. |
| **`SEND_SMS`** | Sends quick decline text messages (e.g. "I'm in a meeting, call you later"). | Only sends texts when you tap a quick reply button. |
| **`READ_PHONE_STATE`** | Detects active SIM cards and carrier lines for dual-SIM phones. | Needed for SIM slot detection. |
| **`VIBRATE`** | Provides tactile feedback when tapping keypad buttons. | Hardware haptics only. |

---

## 📦 How to Download & Install

### Option 1: Automatic Updates via Obtainium (Recommended)
If you use [Obtainium](https://github.com/ImranR98/Obtainium), you can get automatic update notifications directly from our GitHub Releases:

<p align="center">
  <a href="https://apps.obtainium.imranr.dev/redirect.html?r=obtainium://add/https://github.com/Secure-Phone-apps/Secure-Dialer">
    <img src="https://raw.githubusercontent.com/ImranR98/Obtainium/main/assets/graphics/badge_obtainium.png" height="52" alt="Get it on Obtainium" />
  </a>
</p>

1. Install Obtainium on your Android phone.
2. Tap **Add App** and paste our repository URL: `https://github.com/Secure-Phone-apps/Secure-Dialer`.
3. Tap **Add** and Obtainium will automatically pick the right APK for your device and keep you updated!

### Option 2: Direct APK Download from GitHub Releases
You can download signed release APKs directly from our **[GitHub Releases Page](https://github.com/Secure-Phone-apps/Secure-Dialer/releases)**:

| APK File Name | Which One Should You Download? |
| :--- | :--- |
| **`secure-dialer-v1.3.0-arm64-v8a.apk`** | **Most modern Android phones** (Pixel, Samsung Galaxy, OnePlus, Xiaomi, Motorola from the last 6+ years). **Choose this one if unsure!** |
| **`secure-dialer-v1.3.0-armeabi-v7a.apk`** | **Older 32-bit Android phones** and budget devices. |
| **`secure-dialer-v1.3.0-x86_64.apk`** | **64-bit Emulators**, ChromeOS, or Android-x86 PC setups. |
| **`secure-dialer-v1.3.0-universal.apk`** | **Universal build** that runs on any Android device. |

---

## 🛠️ How to Build from Source (For Developers)

If you want to inspect the code, run tests, or compile your own APK:

```bash
# 1. Clone the repository
git clone https://github.com/Secure-Phone-apps/Secure-Dialer.git
cd Secure-Dialer

# 2. Run unit tests
gradle :app:testDebugUnitTest

# 3. Build the debug APK
gradle :app:assembleDebug
```

Output APK will be generated at: `app/build/outputs/apk/debug/`

---

## 💬 Community, Feedback & Support

I am continuously working to refine this dialer and make it as reliable, beautiful, and secure as possible. 

* 💬 **[GitHub Discussions](https://github.com/Secure-Phone-apps/Secure-Dialer/discussions):** Share your ideas, ask questions, or tell me how it runs on your phone model.
* 🐛 **[GitHub Issues](https://github.com/Secure-Phone-apps/Secure-Dialer/issues):** Found a bug or glitch? Please report it with your phone model and Android version so I can fix it!
* 📚 **[Project Wiki](wiki/Home.md):** Deep-dive technical guides on encryption, permissions, and custom ROM setups (GrapheneOS, CalyxOS, LineageOS).
* 💖 **[Sponsor on GitHub Sponsors](https://github.com/sponsors/Secure-Phone-apps):** If you find this dialer helpful and want to support my work and help me get test devices, your support means the world to me!

---

## 📄 License

Secure Dialer is free software licensed under the **GNU General Public License v3.0 (GPLv3)**. You are free to inspect, audit, modify, and build it from source. See the [LICENSE](LICENSE) file for full details.

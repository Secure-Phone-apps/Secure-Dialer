# Community Launch Pitch Kit

Use this pre-formatted post to share Secure Dialer on privacy and open-source forums like **r/fossdroid**, **r/degoogle**, **r/privacy**, or **XDA Developers**.

---

### **Reddit Post Title Suggestions:**
- `[FOSS] Secure Dialer: A 100% offline, zero-permission Android dialer built with Jetpack Compose & SQLCipher (No ads, no cloud, no trackers)`
- `I built a modern, open-source alternative to Google Dialer & Truecaller with zero internet permissions [FOSS]`

---

### **Reddit Post Body:**

Hi everyone! 👋

I wanted to share **Secure Dialer**, a 100% open-source, offline-first Android dialer and local contacts manager I've been developing and refining over the past several months.

### 🛡️ Why build another dialer?
Most stock and third-party dialers today (Google Phone, Samsung, Truecaller) require internet access, collect diagnostic telemetry, or upload call logs and address books to the cloud. Even in the FOSS space, many dialers have outdated UIs or lack modern spam screening and encrypted notes.

Secure Dialer was built with a strict rule: **Zero internet permissions.**

### 🔑 Key Features:
* **0 KB Network Egress:** `android.permission.INTERNET` is completely omitted. The app cannot connect to the internet even if it wanted to.
* **Modern Material 3 Expressive UI:** Built entirely with Jetpack Compose, supporting dynamic color theming, fluid gestures, and dark/light modes.
* **Smart T9 Dialpad:** Multi-alphabet, phonetic, and partial matching with instant query responses.
* **Offline Spam Screening:** Local regex and prefix-based call blocking without external server lookups.
* **Encrypted Backups & Notes:** AES-256 SQLCipher encryption for your call notes, private contacts, and local backups.
* **Fake Call Simulator:** A discreet escape tool to trigger realistic incoming call rings when you need to step away from a meeting or situation.
* **Wide Compatibility:** Works seamlessly from Android 7.0 (Nougat) all the way up to Android 16.

### 🔗 Links:
* **GitHub Repository:** https://github.com/Secure-Phone-apps/Secure-Dialer
* **Website & Web Showcase:** https://secure-phone-apps.github.io/Secure-Dialer/
* **Direct APK Download:** https://github.com/Secure-Phone-apps/Secure-Dialer/releases

Feedback, feature ideas, and code contributions are always welcome! ⭐

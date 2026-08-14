# Contributing to Secure Dialer 🤝

Hey everyone! Thank you so much for your interest in helping improve **Secure Dialer**!

Whether you are helping translate the app into your language, testing it on different phone models, reporting bugs, or submitting code improvements, every bit of help is deeply appreciated.

---

## 🔒 Core Guidelines & Invariants

To keep Secure Dialer completely private, lightweight, and trustworthy, all contributions must respect these simple rules:

1. **Zero Internet Permission (Non-Negotiable):** Secure Dialer never includes the `android.permission.INTERNET` permission. Please do not add any third-party analytics, ads, cloud sync, or external tracking SDKs.
2. **Native Android APIs First:** We prefer direct native integrations with Android OS telephony services (`TelecomManager`, `InCallService`, `CallScreeningService`, and `ContactsContract`) without unnecessary external wrappers.
3. **Lightweight & Fast:** The app should open instantly and run smoothly on both new flagship phones and older Android devices.
4. **Material 3 Expressive Design:** Follow modern Jetpack Compose Material 3 design patterns with proper edge-to-edge system insets and accessible touch targets (minimum 48dp).

---

## 🛠️ Developer Setup & Testing

### Requirements
* **Android Studio** (Koala or newer recommended)
* **JDK 17**
* **SDK Compatibility:** Android 7.0 (API 24) to Android 15/16 (API 36)

### Quick Build & Test Commands
```bash
# Clone the repository
git clone https://github.com/Secure-Phone-apps/Secure-Dialer.git
cd Secure-Dialer

# Run local unit tests
gradle :app:testDebugUnitTest

# Assemble debug APK
gradle :app:assembleDebug
```

---

## 📥 How to Submit a Pull Request (PR)

1. Check existing **[GitHub Issues](https://github.com/Secure-Phone-apps/Secure-Dialer/issues)** to see if the topic is already being discussed.
2. Fork the repository and create a descriptive feature branch:
   ```bash
   git checkout -b feature/my-improvement
   ```
3. Test your changes locally to ensure the app compiles cleanly and tests pass.
4. Open a Pull Request on GitHub with a clear description of what you changed and which devices you tested on.

---

## 🛡️ Security Reports

If you discover a security flaw or privacy issue, please do not open a public issue. Email me directly at **`movstore.online@gmail.com`** so we can fix it privately before disclosure.

*Thank you for supporting private, open-source communication tools!*

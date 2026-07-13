# Contributing to Secure Dialer 🤝

Thank you for your interest in contributing to **Secure Dialer**! We are building a modern, lightweight, stable, and completely offline-first Android phone dialer to preserve user privacy and security.

As an open-source project, your contributions—whether reporting bugs, improving documentation, refactoring code, or submitting features—are highly appreciated.

---

## 📜 Core Guarantees & Code Principles

To preserve the trust, speed, and privacy of our users, all contributions must adhere strictly to these engineering principles:

1. **🔒 Zero-Internet Guarantee (Hard Rule):** Secure Dialer does not use the `android.permission.INTERNET` permission. Under no circumstances should any network access, third-party analytics, error reporting SDKs, or cloud services be introduced.
2. **🔌 Native Alternatives Only:** Do not add external library wrappers, complex architectures, or speculative features. We favor direct interactions with native Android Framework APIs (e.g., `TelecomManager`, `InCallService`, `CallScreeningService`, and `ContactsContract`).
3. **⚡ Ruthless Code Efficiency:** Keep the codebase lightweight and highly responsive. Avoid boilerplate code, complex DI frameworks, or unnecessary wrapper hierarchies. Use native Kotlin and Jetpack Compose idioms.
4. **🎨 Material Design 3 Styling:** Ensure all UI elements conform to the Material 3 standard, utilizing proper edge-to-edge system insets (`enableEdgeToEdge`), accessible touch targets (minimum 48dp), and consistent padding.

---

## 🚀 Setting Up the Development Environment

### Prerequisites
* **Android Studio** (Koala or newer)
* **JDK 17**
* **Android SDK 24** (Minimum) to **SDK 36** (Target)

### Build Instructions
1. **Clone your fork of the repository:**
   ```bash
   git clone https://github.com/YOUR_USERNAME/Secure-Dialer.git
   ```
2. **Open the project** in Android Studio.
3. Allow Gradle to synchronize.
4. Run or assemble the debug APK:
   ```bash
   gradle assembleDebug
   ```

---

## 🧪 Testing Guidelines

We use Robolectric for fast local JVM tests and Roborazzi for visual regression / screenshot verification without requiring physical emulators.

### Running Local Unit & JVM Tests
To run standard unit tests and local Robolectric lifecycle tests:
```bash
gradle :app:testDebugUnitTest
```

### Verifying UI Screenshots
To verify that UI changes have not introduced visual regressions:
```bash
gradle :app:verifyRoborazziDebug
```

### Recording Reference Screenshots
If you have intentionally modified the UI and need to update the baseline reference screenshots:
```bash
gradle :app:recordRoborazziDebug
```

---

## 📥 Submission Process

1. **Check Existing Issues:** Search the issue tracker to see if there is already an open issue or discussion regarding your change.
2. **Create a Feature Branch:** Branch from the `main` branch with a descriptive name:
   ```bash
   git checkout -b feature/your-feature-name
   ```
3. **Write Clean Code:**
   * Keep your edits focused and minimal.
   * Add comments only where absolutely necessary to explain complex native system logic.
   * Format your code standardly with Kotlin style guidelines.
4. **Run Verification:** Ensure your code builds perfectly and all tests pass:
   ```bash
   gradle assembleDebug :app:testDebugUnitTest :app:verifyRoborazziDebug
   ```
5. **Commit & Push:** Commit your changes with descriptive, concise commit messages, and push to your fork:
   ```bash
   git commit -m "Refactor: Condense recents transaction history logic"
   git push origin feature/your-feature-name
   ```
6. **Submit a Pull Request (PR):** Open a PR against the `main` branch of the official repository. Describe your changes clearly and verify that your PR adheres to our zero-network, privacy-first principles.

---

## 🛡️ Security Vulnerabilities

Please do **NOT** open public issues for security vulnerabilities. Report security issues privately by emailing `movstore.online@gmail.com`.

---

*Thank you for helping us build a faster, safer, and completely private mobile experience!*

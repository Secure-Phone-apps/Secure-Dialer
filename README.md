# Secure Dialer 📞

> **Trust, Clarity, and Uncompromising Privacy.**  
> A lightweight, modern, and secure calling assistant built with Jetpack Compose and native Android components.

---

## 🌟 The Philosophy

In a world where digital communication is increasingly monitored and commoditized, **Secure Dialer** is built with a singular, unyielding promise: **your conversations, contacts, and call histories belong exclusively to you.** 

Rather than wrapping heavy, speculative third-party libraries that bleed data, Secure Dialer acts as a direct, elegant window into native Android framework layers. The result is an incredibly fast, secure, and privacy-first application that respects your device, your battery, and your peace of mind.

---

## ✨ Key Features

### 🔒 1. Uncompromising Privacy & Security
* **On-Device Only:** Your contacts, call logs, and calling analytics never leave your device. There are no background server syncs, no telemetry tracker SDKs, and no analytics engines.
* **On-Demand Permission Consent:** Android runtime permissions (like `CALL_PHONE` and `READ_CONTACTS`) are requested strictly on-demand, with clear, high-contrast visual cues so you remain in absolute control of your data.
* **AppOps Attribution Auditing:** Fully configured with native Android `attribution` tagging, ensuring transparent data access that is easily auditable by the system.

### ⚡ 2. High-Performance Native Integration
* **Telecom Framework Integration:** By using native Android `TelecomManager` and `InCallService` APIs instead of bloated custom engines, Secure Dialer ensures perfect reliability for incoming/outgoing calls, background service handling, and carrier network handshakes.
* **Native Contacts Contract:** Direct synchronization with standard system contacts (`ContactsContract`) ensures memory-efficiency, fluid searches, and zero redundant storage.

### 🎨 3. Elegant Jetpack Compose UI (Material 3)
* **Fluid Layouts:** A beautiful, responsive interface styled following Material Design 3 guidelines.
* **Dynamic Dark/Light Mode:** Seamless adaptive colors that react perfectly to system preferences, reducing eye strain and saving battery.
* **Touch-Target Precision:** Follows strict accessibility standards with large, touch-safe dialer keys (minimum 48dp x 48dp) and readable headings.

### 🧹 4. Extremely Lightweight Codebase
* Streamlined and optimized using clean **MVVM architecture**.
* Redundant logic, useless boilerplate, and legacy wrappers have been ruthlessly eliminated for lightning-fast application launches and minimal APK footprint.

---

## 🛠️ Technical Highlights

* **Language:** Kotlin (100%)
* **UI Framework:** Jetpack Compose (Material Design 3)
* **Default Dialer Role:** Fully integrates with Android's `RoleManager` (`RoleManager.ROLE_DIALER`) for API 29+ devices, and falls back gracefully to `ACTION_CHANGE_DEFAULT_DIALER` on older devices.
* **State Management:** Reactive architecture using `ViewModel` and `MutableStateFlow` to guarantee a completely lag-free user experience.

---

## 🚀 How to Build and Run

### Prerequisites
1. **Android Studio** (Koala or newer recommended).
2. **Android SDK 34** or higher.
3. Gradle Kotlin DSL.

### Steps
1. Clone the repository to your local workspace.
2. Open the project in Android Studio.
3. Wait for the Gradle sync to finish.
4. Run the `:app` configuration on your physical device or emulator.
5. Set **Secure Dialer** as your system's default dialer when prompted to unlock full call-handling functionality!

---

## 🤝 Connected and Trusted

Secure Dialer is more than just another utility—it is a commitment to a clean, stress-free mobile experience. By treating permission prompts, user inputs, and screen layouts with absolute visual transparency, we aim to deliver an experience where technology serves you, and only you.

We believe that great software is defined not by how much code is written, but by how much unnecessary code is **cut**. 

---

## 📄 License

This project is licensed under the **MIT License**. It is fully open-source, permissive, and built to be shared. See the [LICENSE](LICENSE) file for the complete text.

---

*Made with 💙 and 🛡️ for Android.*

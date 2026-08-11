# Welcome to the Secure Dialer Wiki 📚

Welcome to the official technical documentation and wiki for **Secure Dialer**—a modern, zero-internet, privacy-first open-source Android calling application built with Jetpack Compose, Kotlin 2.0, and Room Database.

---

## ⚡ Quick Navigation

* 🛡️ **[Security & Encryption Architecture](Security-and-Encryption-Architecture)**: Deep dive into hardware-backed AES-256 GCM database encryption, Android KeyStore, and offline sandboxing.
* 🔑 **[Permissions & Privacy Explained](Permissions-and-Privacy-Explained)**: Transparent breakdown of every required Android permission and why internet access is strictly blocked.
* 📦 **[Installation & Obtainium Guide](Installation-and-Obtainium-Guide)**: Instructions for installing APKs, verifying SHA-256 checksums, and configuring automatic updates via Obtainium.
* ❓ **[FAQ & Troubleshooting](FAQ-and-Troubleshooting)**: Solutions for setting default phone app status, call screening service, dual-SIM routing, and custom ROM setups (GrapheneOS/LineageOS).

---

## 🌟 Vision & Mission

Commercial Android dialers track calling patterns, harvest address books, and upload private details to remote analytics servers. Secure Dialer was built to restore absolute sovereignty over personal telecommunications:

1. **Zero Internet Sandbox:** Compiled without `android.permission.INTERNET`. The OS physically prevents network transmission.
2. **Hardware Key Isolation:** Sensitive contact and call databases are encrypted on-device via SQLCipher and keys generated inside the device's Secure Enclave (TEE/StrongBox).
3. **Modern UX:** Designed using Jetpack Compose and Material Design 3 for fluid performance, eye-safe dark mode, and single-handed accessibility.

---

## 💬 Community & Support

* **GitHub Repository:** [Secure-Phone-apps/Secure-Dialer](https://github.com/Secure-Phone-apps/Secure-Dialer)
* **Discussions & Q&A:** [Community Discussions](https://github.com/Secure-Phone-apps/Secure-Dialer/discussions)
* **Report an Issue:** [Interactive Issue Tracker](https://github.com/Secure-Phone-apps/Secure-Dialer/issues)
* **Sponsor Development:** [GitHub Sponsors](https://github.com/sponsors/Secure-Phone-apps)

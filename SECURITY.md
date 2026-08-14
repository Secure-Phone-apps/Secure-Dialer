# Security Policy & Architecture (SECURITY.md)

Hey there! As the creator of Secure Dialer, security and user privacy are my number one priority. I built this app with a **zero-trust, local-first architecture** so your phone calls, contacts, and logs never leave your physical device.

---

## 1. How Your Data Is Protected

Even if someone gets physical access to your phone or if another app is trying to inspect files, Secure Dialer keeps your data secure through multiple layers:

### A. On-Device Database Encryption (SQLCipher AES-256)
- **Engine:** `net.sqlcipher.database`
- **Encryption:** Full AES-256 database encryption at rest.
- **Key Generation:** A 256-bit cryptographic key is generated on first launch using `SecureRandom`.
- **Key Protection (Hardware Keystore):** The database passphrase is encrypted using `AES/GCM/NoPadding` and stored in private app preferences, with its master key locked inside your phone's hardware **Android KeyStore** (TEE / StrongBox). This means keys cannot be extracted from device memory or file dumps.

### B. Secure Password-Protected Backups
When you export your settings and data:
- **Key Derivation (PBKDF2):** Your custom password is converted into a 256-bit AES key using **PBKDF2 with HmacSHA256** and **10,000 iterations**.
- **Random Salt:** A unique 16-byte random salt is generated with `SecureRandom` for every backup file.
- **Authenticated Encryption (AES-GCM):** The backup file is encrypted using AES in Galois/Counter Mode (`AES/GCM/NoPadding`) with a fresh 12-byte IV per backup, providing tamper-proof authentication.

---

## 2. Privacy & Permission Invariants

- **100% Offline by Design:** Secure Dialer does not declare or use `android.permission.INTERNET`. There are zero analytics SDKs, zero crash tracking libraries, and zero cloud backends.
- **Minimal Permissions:** Only telephony permissions strictly required by the Android OS to make phone calls, manage audio routing, and display contacts are requested.

---

## 3. Reporting a Vulnerability

If you are a security researcher or developer and find any security vulnerability, please report it privately:

* **Contact Email:** `movstore.online@gmail.com`
* Please include a short description, reproduction steps, and potential impact.
* I will acknowledge your report within 48 hours and work on deploying a verified fix in the next release update.

---

*Thank you for helping keep open-source software secure and private for everyone!*

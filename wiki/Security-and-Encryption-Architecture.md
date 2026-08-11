# 🛡️ Security & Encryption Architecture

Secure Dialer employs a defense-in-depth security model to protect call histories, local contact books, and blocked caller lists from local malware, unauthorized device access, and physical extraction.

---

## 1. Zero-Internet Hardening

The fundamental pillar of Secure Dialer's privacy is **Operating System-Level Isolation**:

* **Manifest Omission:** The `AndroidManifest.xml` file **does not** request `android.permission.INTERNET` or `android.permission.ACCESS_NETWORK_STATE`.
* **OS-Enforced Blocking:** Even if an attacker were to attempt an injection attack, the Android OS kernel physically drops any socket creation or outbound network request attempt.
* **Zero Telemetry:** No Google Analytics, Firebase Crashlytics, Sentry, or third-party tracking SDKs exist in the compiled codebase.

---

## 2. Hardware-Backed AES-256 GCM Encryption

All local app data (call history logs, notes, speed dial configurations, local contact cards, and blocklists) is stored inside an encrypted **SQLCipher SQLite database**.

### Key Generation Lifecycle:
1. **Android KeyStore Initialization:** Upon first launch, Secure Dialer invokes the Android KeyStore System Provider (`AndroidKeyStore`).
2. **Hardware Security Module (HSM / TEE):** A 256-bit AES cryptographic master key is generated inside the device's hardware-isolated **Trusted Execution Environment (TEE)** or **StrongBox Keymaster**.
3. **Master Key Protection:** The master key never enters standard RAM in cleartext and cannot be extracted from the device hardware.
4. **Database Cipher Passphrase:** SQLCipher encrypts database pages on disk using AES-256 in Galois/Counter Mode (GCM).

```
[ Local Database File ] <---> [ SQLCipher Engine (AES-256 GCM) ] <---> [ Android KeyStore (TEE / HSM) ]
```

---

## 3. Contact Isolation: Local vs. System Contacts

Secure Dialer gives users full control over how contacts are stored:

* **System Contacts Integration:** Read and write access to standard Android System Contacts via `ContactsContract` provider.
* **Encrypted Local Contacts Vault:** Users can store sensitive contacts strictly inside Secure Dialer's encrypted database. Local contacts are completely isolated from other installed apps on your device.

---

## 4. Offline Call Screening & Spam Prevention

Instead of uploading incoming phone numbers to cloud servers (like Truecaller), Secure Dialer uses Android's native `CallScreeningService`:

* **Local Spam Blocklist:** Numbers added to your blocklist are stored in your encrypted local database.
* **Zero-Latency Screening:** When an incoming call arrives, `CallScreeningService` queries the encrypted local database in under 5 milliseconds.
* **Automated Call Rejection:** If a match is found, the call is rejected or silenced before your phone rings—without transmitting any data over the internet.

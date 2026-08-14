# 🛡️ Security & Encryption Architecture

Hey everyone! Here is a detailed breakdown of how **Secure Dialer** protects your data, encrypts your database, and keeps your private phone calls isolated on your device.

---

## 1. Zero Network Access (Physical Isolation)

The primary foundation of Secure Dialer is absolute **offline isolation**:

* **No Internet Permission:** `AndroidManifest.xml` does not declare `android.permission.INTERNET` or `android.permission.ACCESS_NETWORK_STATE`.
* **Kernel Enforcement:** The Android operating system kernel physically blocks any network sockets from opening.
* **No Analytics or Trackers:** There are zero third-party tracking or advertising SDKs (No Firebase, No Google Analytics, No Sentry).

---

## 2. On-Device AES-256 SQLCipher Database Encryption

All custom call notes, speed dial shortcuts, local contact cards, and blacklists are stored inside a **SQLCipher-encrypted SQLite database**.

### How Encryption Keys are Managed:
1. **Android KeyStore:** On first launch, Secure Dialer communicates with the hardware-backed **Android KeyStore System**.
2. **Hardware Enclave:** A 256-bit AES master key is generated inside your device's secure hardware enclave (**TEE / StrongBox**).
3. **Encrypted at Rest:** The database passphrase is encrypted using `AES/GCM/NoPadding` and stored in private app storage. Keys cannot be extracted even with file-system dumps.
4. **Full Page Encryption:** SQLCipher encrypts all database pages on disk using **AES-256 in GCM mode**.

```text
┌──────────────────────────────┐        ┌────────────────────────────┐        ┌──────────────────────────────┐
│   Encrypted Local Database   │ ◄────► │   SQLCipher DB Engine      │ ◄────► │  Android KeyStore System     │
│   (AES-256 on device disk)   │        │     (AES-256 GCM)          │        │  (Hardware TEE / StrongBox)  │
└──────────────────────────────┘        └────────────────────────────┘        └──────────────────────────────┘
```

---

## 3. Fast Offline Call Screening

Instead of uploading caller numbers to cloud databases like commercial caller ID apps do, Secure Dialer runs **100% on-device** using Android's native `CallScreeningService`:

```text
[ Incoming Call ] ──► [ CallScreeningService ] ──► [ Local Encrypted DB (<5ms) ] ──► [ Silence / Reject ]
                                                            │
                                                            ▼
                                                (100% Local / Zero Network)
```

1. **Sub-5ms Local Query:** When a call comes in, `CallScreeningService` instantly checks your encrypted local blocklist.
2. **Local Decision:** If the number matches your blocklist or rules (e.g. unknown numbers), the call is silenced or rejected before your phone even rings.
3. **Zero Telemetry:** No caller identity or phone numbers are ever sent over the air.

---

## 4. Threat Model & Security Defenses

| Potential Threat | How Secure Dialer Protects You |
| :--- | :--- |
| **Physical Theft / Device Dump** | Database files on flash storage are encrypted with AES-256 GCM; master keys are locked in hardware KeyStore. |
| **Malicious Apps on Same Device** | Android sandbox UID isolation prevents other apps from reading Secure Dialer's private storage folder. |
| **Network Sniffing / Wi-Fi Snooping** | The app has no internet permission, so 0 bytes are transmitted over Wi-Fi or cellular networks. |
| **Cloud Leaks & Server Hacks** | There is no cloud server. We never host, store, or see your data. |

---

📍 **Quick Links:** [[Home]] | [[Installation and Obtainium Guide]] | [[FAQ and Troubleshooting]] | [[Permissions and Privacy Explained]] | [[Wall of Honor]]

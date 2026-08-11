# 🛡️ Security & Encryption Architecture

Secure Dialer employs a defense-in-depth security architecture designed to protect call logs, contact books, speed dials, and blocked numbers from local device malware, physical extraction, and unauthorized background inspection.

---

## 1. Operating System-Level Network Isolation

The core pillar of Secure Dialer's zero-trust model is **Hardware & OS-Level Sandboxing**:

* **Manifest Omission:** The `AndroidManifest.xml` file completely omits `android.permission.INTERNET` and `android.permission.ACCESS_NETWORK_STATE`.
* **Kernel Enforcement:** The Android Linux kernel strictly drops any attempt to open sockets (`socket()`, `connect()`). Network calls are rejected at the system call layer.
* **Zero Telemetry SDKs:** The application binary contains zero analytics, tracking, or crash reporting SDKs (No Firebase, No Google Play Services, No Sentry).

---

## 2. Hardware-Backed AES-256 GCM Database Encryption

All local app data (call history logs, contact notes, speed dial shortcuts, local encrypted contact cards, and blacklists) is stored inside a **SQLCipher-encrypted SQLite database**.

### Key Generation & Cryptographic Lifecycle:

1. **KeyStore Provider:** Upon first initialization, Secure Dialer calls the Android KeyStore System (`AndroidKeyStore`).
2. **Secure Enclave Generation:** A 256-bit AES cryptographic master key is generated inside the device's hardware-isolated **Trusted Execution Environment (TEE)** or **StrongBox Keymaster (HSM)**.
3. **RAM Protection:** The master passphrase is never stored in unencrypted persistent storage and cannot be extracted via USB debugging or root access.
4. **On-Disk Cipher:** SQLCipher encrypts all page sectors on disk using **AES-256 in Galois/Counter Mode (GCM)**.

```text
┌────────────────────────────────┐      ┌───────────────────────────────┐      ┌─────────────────────────────────┐
│     Encrypted Local Database   │ ◄──► │  SQLCipher Database Engine    │ ◄──► │  Android KeyStore System        │
│    (call_logs.db on disk)      │      │       (AES-256 GCM)           │      │   (Hardware TEE / StrongBox)    │
└────────────────────────────────┘      └───────────────────────────────┘      └─────────────────────────────────┘
```

---

## 3. Isolated Encrypted Local Contacts Vault

Users can choose between two contact storage modes:

* **System Contacts Integration:** Standard Android `ContactsContract` provider integration for seamless device-wide contact management.
* **Encrypted Local Contacts Vault:** An isolated contacts vault stored strictly within Secure Dialer's encrypted database. Local contacts are completely invisible to other applications on your smartphone.

---

## 4. Offline Call Screening & Zero-Latency Rejection

Unlike cloud-dependent caller ID apps that upload incoming caller numbers to remote lookup servers, Secure Dialer relies on Android's native `CallScreeningService`:

```text
[ Incoming Call Event ] ──► [ CallScreeningService ] ──► [ Query SQLCipher Local DB (<5ms) ] ──► [ Reject & Silence / Allow ]
                                                                   │
                                                                   ▼
                                                       (Zero Network Transmission)
```

1. **Instant Local Lookup:** When an incoming call arrives, `CallScreeningService` queries the encrypted local database in under 5 milliseconds.
2. **Autonomous Decision:** If the caller is on your blocklist or matches your call-blocking criteria (e.g. unknown numbers), the call is automatically rejected or silenced before your phone rings.
3. **Zero Leaks:** No phone numbers or timestamp telemetry leave the physical device.

---

## 🛡️ Threat Model & Security Controls Matrix

| Threat Vector | Attack Scenario | Secure Dialer Defense Control |
| :--- | :--- | :--- |
| **Physical Theft / Forensic Dumping** | Attacker extracts flash storage chips or takes device dump. | Database pages are encrypted with AES-256 GCM; master keys are locked in TEE / StrongBox hardware. |
| **Malicious App Inter-Proc Extraction** | Malware attempts to read local app data. | Linux UID sandboxing and file permissions prevent other apps from accessing `/data/data/com.aistudio...`. |
| **Network Traffic Sniffing / MITM** | Attacker monitors Wi-Fi or cellular traffic for voice logs. | Internet permission is omitted; 0 bytes are transmitted over Wi-Fi or cellular networks. |
| **Cloud Leaks & Data Subpoenas** | Third-party database breaches or cloud subpoenas. | No cloud database exists. Secure Dialer developers do not host or store user data anywhere. |


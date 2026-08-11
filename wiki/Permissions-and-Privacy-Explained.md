# 🔑 Permissions & Privacy Explained

Secure Dialer strictly operates under the security principle of **Least Privilege**. Every permission requested in `AndroidManifest.xml` serves a direct, transparent function for cellular phone calling, offline call screening, and contact management.

---

## 📋 Comprehensive Permission Manifest Breakdown

| Permission Name | Grant Type | Technical Purpose & Justification |
| :--- | :--- | :--- |
| `READ_PHONE_STATE` | Runtime | Detects incoming/outgoing call states, cellular radio network status, and SIM card slot availability for dual-SIM routing. |
| `CALL_PHONE` | Runtime | Initiates phone calls directly from the T9 dialpad, speed dial shortcuts, and contact detail cards. |
| `READ_CALL_LOG` | Runtime | Displays incoming, outgoing, missed, and rejected call history entries inside the unified recents log. |
| `WRITE_CALL_LOG` | Runtime | Enables deleting individual call log entries, clearing recents history, and updating call status badges. |
| `READ_CONTACTS` | Runtime | Displays system contacts inside the T9 dialpad auto-suggest and contact manager list. |
| `WRITE_CONTACTS` | Runtime | Allows creating, editing, starring, and removing contacts in the system address book. |
| `MANAGE_OWN_CALLS` | Normal | Integrates with Android's `TelecomManager` for managing active call audio streams and in-call UI overlays. |
| `POST_NOTIFICATIONS` | Runtime | Displays active call heads-up banners, missed call alerts, and incoming call notification cards (Android 13+). |
| `VIBRATE` | Normal | Provides tactile haptic feedback on dialpad key taps and incoming ring vibration. |

---

## 🚫 Explicitly Excluded Dangerous Permissions

For complete user privacy and zero-trust verification, the following high-risk permissions are **completely absent** from Secure Dialer's source code and manifest:

| Excluded Permission | Threat Prevented |
| :--- | :--- |
| ❌ **`android.permission.INTERNET`** | **Zero Network Data Exfiltration:** The app cannot create HTTP/S, TCP/UDP sockets or send data to any remote server. |
| ❌ **`android.permission.ACCESS_FINE_LOCATION`** | **No GPS Tracking:** Your location is never requested, accessed, or stored. |
| ❌ **`android.permission.READ_EXTERNAL_STORAGE`** | **No File Scanning:** Private device storage, photos, and documents remain untouched. |
| ❌ **`android.permission.CAMERA`** | **No Visual Access:** Camera hardware is never activated. |
| ❌ **`android.permission.RECORD_AUDIO`** | **No Background Eavesdropping:** Audio recording APIs are never requested or invoked. |

---

## ⚖️ Privacy Comparison: Secure Dialer vs. Stock Phone Apps

| Feature / Metric | Secure Dialer | Stock Dialers (Google / Samsung) | Cloud Call Blockers (Truecaller) |
| :--- | :---: | :---: | :---: |
| **Internet Access** | ❌ **Blocked** | ✅ Enabled | ✅ Required |
| **Call Log Telemetry** | ❌ **Zero** | ⚠️ Uploaded for Analytics | ⚠️ Uploaded to Cloud DB |
| **Contact Book Harvesting** | ❌ **Local Only** | ⚠️ Synced to Cloud Account | ⚠️ Publicly Searchable |
| **Spam Check Method** | 🛡️ **Offline Local DB** | 🌐 Cloud Query | 🌐 Cloud Lookup |
| **Monetization & Ads** | ❌ **100% Free / No Ads** | ❌ Pre-installed | ⚠️ Ads & Premium Subs |

---

## ⚙️ Dynamic Runtime Permission Request Flow

When launching Secure Dialer for the first time:
1. **Default Dialing Service:** You will be prompted to set **Secure Dialer as your Default Phone App** so Android routes incoming calls to our `InCallService`.
2. **Selective Contacts Access:** You can choose whether to grant contacts access. If denied, Secure Dialer still functions perfectly as a standalone dialer with an isolated, encrypted local contact vault!

---

📍 **Quick Links:** [[Home]] | [[Installation and Obtainium Guide|Installation & Updates]] | [[FAQ and Troubleshooting|FAQ & Troubleshooting]] | [[Security and Encryption Architecture|Security Architecture]]



# 🔑 Permissions & Privacy Explained

Hey everyone! As someone building an offline-first app, I want to be 100% transparent with you about every single permission requested in `AndroidManifest.xml`. 

Every permission requested serves one purpose: **letting you make phone calls, manage your contacts, and screen calls safely on your device.**

---

## 📋 What Each Permission Does & Why It Is Needed

| Permission Name | Why Secure Dialer Needs It | How Your Privacy Is Protected |
| :--- | :--- | :--- |
| `READ_PHONE_STATE` | Detects active call states, network carrier status, and SIM card slots on dual-SIM phones. | Reads only on-device hardware state; never uploaded. |
| `CALL_PHONE` | Allows you to place phone calls when tapping numbers on the dialpad or speed dial. | Connects directly to your carrier through Android telephony. |
| `READ_CALL_LOG` | Shows your recent incoming, outgoing, and missed calls in the Recents tab. | Processed locally on your phone only. |
| `WRITE_CALL_LOG` | Lets you delete call log entries or clear history. | Updates only your local call logs. |
| `READ_CONTACTS` | Displays your contacts inside the T9 dialpad search and Contacts list. | Reads local address book records without cloud syncing. |
| `WRITE_CONTACTS` | Allows you to add, edit, or delete contacts directly in the app. | Modifies your local contacts directly. |
| `MANAGE_OWN_CALLS` | Integrates with Android's `TelecomManager` to show active call screens and control audio. | Standard native calling framework. |
| `POST_NOTIFICATIONS` | Displays active in-call banners and missed call notifications (Android 13+). | Local system notifications only. |
| `VIBRATE` | Provides tactile vibration feedback when tapping keypad digits. | Purely physical haptics. |

---

## 🚫 Permissions We Intentionally NEVER Include

To make sure your data can never be compromised or tracked, these permissions are **completely absent** from Secure Dialer:

| Excluded Permission | Why We Never Include It |
| :--- | :--- |
| ❌ **`android.permission.INTERNET`** | **Zero Network Access:** The app has no internet capability. It is physically impossible to leak or upload your call logs, contacts, or data to any server. |
| ❌ **`android.permission.ACCESS_FINE_LOCATION`** | **Zero GPS Tracking:** Your physical location is never requested or accessed. |
| ❌ **`android.permission.READ_EXTERNAL_STORAGE`** | **No File Inspection:** Your private device storage, photos, and files are never touched. |
| ❌ **`android.permission.CAMERA`** | **No Camera Access:** Camera hardware is never activated. |

---

## ⚖️ How Secure Dialer Compares to Other Apps

| Feature | Secure Dialer | Stock Dialers (Google / Samsung) | Cloud Call Blockers (Truecaller) |
| :--- | :---: | :---: | :---: |
| **Internet Access** | ❌ **Completely Blocked** | ✅ Enabled | ✅ Required |
| **Call Log Tracking** | ❌ **Zero Telemetry** | ⚠️ Synced to Cloud / Analytics | ⚠️ Uploaded to Cloud Servers |
| **Address Book Uploads** | ❌ **Zero Uploads** | ⚠️ Synced to OEM / Google Account | ⚠️ Searchable by Others |
| **Spam Screening** | 🛡️ **100% Offline Local DB** | 🌐 Cloud Lookups | 🌐 Cloud Queries |
| **Ads & Monetization** | ❌ **100% Free & No Ads** | ❌ Pre-installed | ⚠️ Ads & Subscriptions |

---

📍 **Quick Links:** [[Home]] | [[Installation and Obtainium Guide]] | [[FAQ and Troubleshooting]] | [[Security and Encryption Architecture]] | [[Wall of Honor]]

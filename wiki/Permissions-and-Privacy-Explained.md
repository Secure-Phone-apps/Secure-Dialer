# 🔑 Permissions & Privacy Explained

Secure Dialer strictly adheres to the principle of **least privilege**. Every permission requested in `AndroidManifest.xml` serves a direct, necessary function for phone calling, call screening, and contact management.

---

## 📋 Permission Manifest Breakdown

| Permission Name | Type | Purpose & Justification |
| :--- | :--- | :--- |
| `READ_PHONE_STATE` | Runtime | Detects incoming call state, cellular network status, and SIM card availability. |
| `CALL_PHONE` | Runtime | Enables placing phone calls directly from the T9 dialpad and contact list. |
| `READ_CALL_LOG` | Runtime | Displays incoming, outgoing, and missed call history logs. |
| `WRITE_CALL_LOG` | Runtime | Allows clearing call history, removing entries, or marking calls as read. |
| `READ_CONTACTS` | Runtime | Displays system contacts within the app's unified contact list and T9 search. |
| `WRITE_CONTACTS` | Runtime | Allows creating, editing, and deleting system contacts from within the app. |
| `MANAGE_OWN_CALLS` | Normal | Allows integration with TelecomManager for active call state management. |
| `POST_NOTIFICATIONS` | Runtime | Displays active ongoing call status, missed call alerts, and incoming call banners (Android 13+). |
| `VIBRATE` | Normal | Provides haptic vibration feedback on dialpad button presses and incoming rings. |

---

## 🚫 Explicitly Excluded Permissions

For absolute transparency, here are permissions that Secure Dialer **DOES NOT** request:

❌ **`android.permission.INTERNET`**: The app cannot establish any HTTP/HTTPS, TCP, or UDP network connections.  
❌ **`android.permission.ACCESS_FINE_LOCATION`**: The app never tracks or accesses your GPS location.  
❌ **`android.permission.READ_EXTERNAL_STORAGE`**: Media files and external files are not read or scanned.  
❌ **`android.permission.CAMERA`**: No camera access is ever requested.  
❌ **`android.permission.RECORD_AUDIO`**: Background microphone recording is strictly impossible.

---

## ⚙️ How Dynamic Runtime Permissions Work

When you open Secure Dialer for the first time:
1. You will be prompted to set **Secure Dialer as your Default Phone App**.
2. You can selectively grant or revoke contacts access. If you grant contacts access, system contacts appear in T9 search. If you deny contacts access, you can still place manual calls or use encrypted local contacts.

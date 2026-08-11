# ❓ FAQ & Troubleshooting Guide

Welcome to the **Secure Dialer FAQ & Troubleshooting Guide**. Search below for solutions to common configuration questions, default app setups, dual-SIM preferences, and custom ROM integration.

---

## 📱 Section 1: Default Phone App & System Integration

### Q1: Why does Secure Dialer ask to be set as the Default Phone App?
**A:** Android OS architecture strictly requires dialer applications to be registered as the system `Default Dialing App` in order to handle active incoming/outgoing call screens (`InCallService`), access cellular hardware controls (`TelecomManager`), and update call logs (`CallLog`).

### Q2: How do I set Secure Dialer as my default dialer on Samsung / Pixel / Xiaomi / OnePlus?
1. Open your device's **System Settings**.
2. Navigate to **Apps** -> **Default Apps**.
3. Tap **Phone App** (or Default Dialer) and select **Secure Dialer**.
4. *(Optional for Xiaomi / HyperOS / MIUI):* Open **Autostart** permissions and grant Autostart to Secure Dialer to ensure incoming calls wake the screen instantly when locked.

---

## 🚫 Section 2: Call Screening, Spam Blocking & Blacklists

### Q3: How do I activate offline Call Screening & Spam Rejection?
1. Open **System Settings** -> **Apps** -> **Default Apps**.
2. Tap **Caller ID & Spam App** (or Call Screening Service).
3. Select **Secure Dialer**.
4. Inside **Secure Dialer** -> **Settings** -> **Call Screening & Blocklist**:
   * Toggle **Enable Offline Call Screening**.
   * Choose blocking rules: **Block Blocklisted Numbers**, **Block Private/Hidden Numbers**, or **Block Unknown Numbers**.

> [!NOTE]
> **100% Offline Screening:** Unlike cloud-based caller ID services (e.g. Truecaller), Secure Dialer screens calls locally using your encrypted on-device database in under 5 milliseconds with zero internet access.

---

## 📶 Section 3: Dual-SIM Calling & Carrier Preferences

### Q4: Does Secure Dialer support Dual-SIM smartphones?
**A:** Yes! Secure Dialer natively detects multi-SIM hardware slots (`SubscriptionManager`). When placing a call:
* A fluid SIM selector prompt appears allowing you to tap **SIM 1** or **SIM 2**.
* You can configure a default SIM preference per contact or globally in **Settings** -> **Dual-SIM Preferences**.

---

## 🔒 Section 4: Custom ROMs (GrapheneOS / CalyxOS / LineageOS / /e/OS)

### Q5: Is Secure Dialer compatible with GrapheneOS and de-Googled Android ROMs?
**A:** **100% Compatible.** Secure Dialer is engineered specifically for privacy-hardened and de-Googled operating systems. It does not contain or depend on Google Play Services, Firebase Cloud Messaging, or microG components.

---

## 💾 Section 5: Local Backup, Migration & Data Security

### Q6: How do I backup my call history, speed dial settings, and local encrypted contacts?
1. Open **Secure Dialer** -> **Settings** -> **Data & Backup**.
2. Tap **Export Encrypted Backup (.vcf / .json)**.
3. Choose a destination folder on your internal storage or external SD card.
4. To restore on a new smartphone, tap **Import Backup** and select your saved backup file.

> [!SECURITY]
> **Data Ownership:** Your backups remain strictly on your physical storage device. Make sure to keep your exported backup files in a safe location or encrypted container.


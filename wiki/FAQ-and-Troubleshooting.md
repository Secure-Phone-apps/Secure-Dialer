# ❓ FAQ & Troubleshooting Guide

Here are solutions to the most common questions and configuration scenarios when setting up **Secure Dialer**.

---

## 📱 Default Phone App Setup

### Q: Why does Secure Dialer request to be set as the Default Phone App?
**A:** Android requires dialer apps to be registered as the `Default Dialing App` in order to handle active incoming/outgoing call screens (`InCallService`), access hardware cellular controls (`TelecomManager`), and manage system call history (`CallLog`).

### Q: How do I set Secure Dialer as my default dialer on Samsung / Pixel / Xiaomi?
1. Open your phone's **System Settings**.
2. Go to **Apps** -> **Default Apps**.
3. Tap **Phone App** and select **Secure Dialer**.

---

## 🚫 Call Screening & Spam Blocking

### Q: How do I activate offline Call Screening?
1. Open **System Settings** -> **Apps** -> **Default Apps**.
2. Tap **Caller ID & Spam App** (or Call Screening).
3. Select **Secure Dialer**.
4. Inside Secure Dialer settings, enable **Block Unknown Numbers** or **Block Blocklist Numbers**.

---

## 📶 Dual-SIM & Calling Configurations

### Q: Does Secure Dialer support Dual-SIM phones?
**A:** Yes! When placing a call, Secure Dialer automatically displays a dual-SIM selector prompt allowing you to pick SIM 1 or SIM 2. You can also configure a preferred default SIM in Settings -> Dual-SIM Preferences.

---

## 🔒 Custom ROMs (GrapheneOS / CalyxOS / LineageOS)

### Q: Is Secure Dialer compatible with GrapheneOS / CalyxOS / LineageOS?
**A:** Absolutely. Secure Dialer is engineered to run seamlessly on de-Googled Android environments, custom ROMs, and hardened operating systems. Because the app does not rely on Google Play Services or proprietary microG components, it operates natively out of the box.

---

## 💾 Backup & Restore

### Q: How do I backup my call history and local contacts?
1. Open **Secure Dialer** -> **Settings** -> **Data & Backup**.
2. Tap **Export Local Backup (.vcf / .json)**.
3. Save the encrypted file to your internal storage or external SD card.
4. To restore on a new device, tap **Import Backup** and select your file.

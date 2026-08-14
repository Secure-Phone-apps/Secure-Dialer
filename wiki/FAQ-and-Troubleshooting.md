# ❓ FAQ & Troubleshooting Guide

Hey everyone! Here are answers to common questions about setting up **Secure Dialer**, dual-SIM configuration, spam blocking, and custom ROMs.

---

## 📱 Setting Up as Your Default Phone App

### Why does the app ask to be the Default Phone App?
Android requires any app that displays incoming call screens and controls phone audio to be set as the system's **Default Phone App** (`InCallService`). Without this, the phone cannot let Secure Dialer answer or manage your calls.

### How to set Secure Dialer as Default:
1. Open your phone's **Settings**.
2. Go to **Apps** -> **Default Apps**.
3. Tap **Phone App** and select **Secure Dialer**.
4. *(For Xiaomi / HyperOS / MIUI users):* Also enable **Autostart** in app settings so incoming calls can wake up the screen immediately when locked.

---

## 🚫 Offline Spam Blocking & Call Screening

### How do I enable Call Screening?
1. Go to **Settings** -> **Apps** -> **Default Apps**.
2. Tap **Caller ID & Spam App** and select **Secure Dialer**.
3. Inside Secure Dialer, tap **Settings** -> **Call Screening & Blocklist** and toggle on your desired blocking options (block private numbers, block unknown numbers, or custom blocklist).

---

## 📶 Dual-SIM Calling & Carriers

### Does Secure Dialer support Dual-SIM phones?
Yes! Secure Dialer detects both SIM slots automatically. When you tap a number or contact to call:
* A clean SIM dialog appears letting you pick **SIM 1** or **SIM 2**.
* You can also set a default preferred SIM in **Settings** if you always call from one card.

---

## 🔒 Custom ROMs (GrapheneOS, CalyxOS, LineageOS)

### Does it work on de-Googled ROMs?
Yes, **100%**. Secure Dialer was built with privacy and de-Googled systems in mind. It has zero dependencies on Google Play Services, microG, or Firebase.

---

## 💾 Backing Up Your Data

### How do I backup my settings and blocklists?
1. In Secure Dialer, open **Settings** -> **Data & Backup**.
2. Tap **Export Encrypted Backup** and save the file to your device storage.
3. To restore on another phone, tap **Import Backup** and enter your password.

---

📍 **Quick Links:** [[Home]] | [[Installation and Obtainium Guide]] | [[Permissions and Privacy Explained]] | [[Security and Encryption Architecture]] | [[Wall of Honor]]

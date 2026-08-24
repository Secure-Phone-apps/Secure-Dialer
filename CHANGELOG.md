# Changelog

All notable changes, fixes, and release milestones for Secure Dialer are documented here.

## [1.4.0] - 2026-08-24

### Settings, Features & UI Improvements
- **Improved Settings & UI Appearance**: Refined settings navigation, visual hierarchy, and Material 3 design layout for a cleaner, modern look.
- **Contact Source Selection**: Added full support for selecting and managing contact sources (Device Contacts, Custom Secure Contacts, or both).
- **Enhanced Existing Features & Security**: Upgraded core call handling mechanisms, data encryption controls, and local privacy safeguards.
- **Expanded Feature Controls**: Introduced granular configuration toggles and enhanced controls across application preferences and call management features.

---

## [1.3.0] - 2026-08-06

### Architecture & APK Splits
- **Multi-ABI APK Splits**: Added architecture-specific splits (`arm64-v8a`, `armeabi-v7a`, `x86_64`, and universal) to keep download sizes tiny, fast to install, and optimized for different phone processors.

### Navigation & Back-Gesture Polish
- **Predictive System Back Support**: Added Compose `BackHandler` hooks across active calls, call log details, and settings sub-menus so swiping back navigates cleanly step-by-step without accidentally closing the app.

### Button Contrast & Dynamic Theming
- **High-Contrast Call Controls**: Redesigned calling and call-answer buttons in a clean, recognizable green, and hang-up / decline buttons in a crisp red.
- **Adaptive Contrast Engine**: Tuned button luminance so actions remain clear and readable across Light mode, AMOLED Dark mode, and Material You dynamic color themes.

---

## [1.2.0] - 2026-07-25

### Material 3 Expressive & UI Polish
- **Material 3 Expressive Design**: Upgraded typography, rounded container cards, and subtle elevation surfaces for a modern, fluid experience.
- **Clean Avatar Initials**: Replaced heavy image placeholders with lightweight, high-contrast typography initials for fast visual scanning.
- **Larger, Comfortable Dialpad**: Increased touch target sizes and fine-tuned keypad ergonomics for comfortable one-handed dialing.

### Multi-Language Support
- **7 Complete Translations**: Added full localization for English, Spanish, German, Hindi, Arabic, Portuguese, and Japanese with native RTL layout handling.

### Search & Filtering
- **Recents Filter Chips**: Filter call history by **All**, **Missed**, **Dialed**, and **Received** calls with one tap.
- **Contacts & Starred Contacts**: Quick filter tabs for All Contacts and Favorites.
- **In-Dialpad Search**: Real-time T9 name and number search directly as you type numbers on the dialpad.
- **CNAP Carrier Name Delivery**: Display carrier-provided caller names for incoming and active calls.

### Smooth 120Hz Scrolling & Fluid Animations
- **Optimized LazyLists**: Set explicit `contentType` keys across call logs and contacts for stutter-free 120Hz scrolling on high refresh rate screens.
- **Spring Navigation Physics**: Tuned bottom navigation bar tab transitions with natural spring physics.

---

## [1.1.0] - 2026-07-15

### Enhancements
- **Keyboard Auto-Dismissal**: Automatically hide the on-screen soft keyboard the moment an outgoing call is placed to keep the call screen clean and unobstructed.
- **Screen Guard Stability**: Smooth transition between locked screen overlays and active in-call screens.

---

## [1.0.2] - 2026-07-10

### Added
- **Favorites Tab**: Dedicated bottom bar tab for quickly calling starred and frequent contacts.
- **Call Log Sync & Deletion**: Fixed call log deletion so clearing an item deletes it from both internal encrypted storage and the system call log resolver without ghost reappearing entries.
- **Accurate Call Notifications**: Fixed active notification tags so incoming call banners only fire during `STATE_RINGING`.

---

## [1.0.1] - 2026-07-09

### Improvements & Fixes
- **Call Timer Accuracy**: Call duration timer now starts ticking only after the call is actually answered (`STATE_ACTIVE`), not while ringing or dialing.
- **Sticky Date Headers**: Grouped recent calls into clean "Today", "Yesterday", and "Older" date buckets.
- **Alphabet Fast Scroller**: Added vertical alphabet drag scroller with tactile haptics to quickly jump through long contact lists.

---

## [1.0.0] - 2026-07-09

### Initial Release
- Built from scratch using modern Kotlin & Jetpack Compose (Material 3).
- Full InCallService and Telecom integration as default phone handler.
- 100% offline architecture with zero internet permissions.
- SQLCipher AES-256 encrypted local database with Android KeyStore master key.
- Offline spam call blocking using native CallScreeningService.
- Built-in Fake Call simulator for escaping awkward situations.

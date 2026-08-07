# Changelog

All notable changes to this project will be documented in this file.

## [1.3.0] - 2026-08-06

### Performance & Build Architecture
- **ABI Split Support**: Configured multi-architecture APK splits (`arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`) to automatically generate individual, lightweight, highly optimized binaries per CPU architecture. This optimizes run speeds and significantly reduces the installed app footprint on user devices.

### Navigation & Gestures
- **System Back-Swipe Navigation Fixes**: Integrated Compose `BackHandler` hooks across sub-settings screens, call detail logs, and active call views. Swiping back or using the system back button now correctly and gracefully steps backward in the app hierarchy rather than exiting the application prematurely.

### Visual Contrast & Theme Ergonomics
- **Contrast-Enhanced Calling Buttons**: Color-coded call actions with context-aware, highly visible greens and reds. The Call and Answer buttons now stand out in a polished theme-friendly green, while Hang Up and Decline buttons are styled in a crisp, recognizable red. 
- **Dynamic Theme Adaptive System**: Engineered the button colors to dynamically adapt their luminance and contrast based on the background color state, keeping the buttons perfectly readable and aesthetically aligned across light, dark, and custom dynamic theme configurations.

## [1.2.0] - 2026-07-25

### Design & Material 3 Expressive
- **Material 3 Expressive Integration**: Adopted Material 3 Expressive design patterns, enhanced typography, elevated card surfaces, and refined accent color options (including slate/grey accent palette).
- **Streamlined Visual Layouts**: Removed heavy placeholder avatar images in Recents and Contacts in favor of lightweight, high-contrast typography initials for clean visual scanning.
- **Enlarged Dialpad Ergonomics**: Optimized keypad layout with larger, cleaner key targets, compact spacing, and instant tactile touch response.

### Multilingual Support & Localization
- **7 App Translations**: Fully localized the user interface in **Spanish**, **German**, **Hindi**, **Arabic**, **Portuguese**, **Japanese**, and default **English** with RTL/LTR layout support.

### Advanced Filtering & Search
- **Comprehensive Call Log Filters**: Expanded Recents tab with quick filter chips for **All**, **Missed**, **Dialed**, and **Received** call logs.
- **Contacts & Favorites Filtering**: Added **All Contacts** and **Favorites** quick filters along with prominent single-tap contact creation.
- **In-Dialpad Smart Search**: Integrated real-time T9 and number search directly inside the Dialpad view to match saved contacts and call history logs instantly.
- **CNAP (Calling Name Delivery) Support**: Enhanced caller identification display for carrier-supplied CNAP name tags during active and incoming calls.

### Performance & Motion Design
- **Ultra-Fluid Tab Navigation**: Redesigned navigation with spring-physics icon scaling (`StiffnessMediumLow`, `DampingRatioMediumBouncy`) and zero-lag tab transitions across Recents, Contacts, and Dialpad.
- **List Recycling Optimizations**: Applied explicit `contentType` definitions across `LazyColumn` components to maximize view reuse and ensure butter-smooth 120Hz scrolling.

### Security & Release Infrastructure
- **Release Build Optimization**: Fine-tuned R8 rules and ProGuard mappings for Room DB entities, DAOs, ViewModel reflection constructors, and serialization to guarantee release APK stability and keep binary size minimal.


## [1.1.0] - 2026-07-15

### Added
- **Modern Architecture Showcase**: Updated project documentation to clearly highlight our 100% Kotlin & Jetpack Compose declarative design stack, increasing trust and confidence among security auditors and open-source developers.
- **Search-Optimized FOSS Identity**: Fully redesigned and optimized the project README for maximum search visibility across GitHub and web search engines, specifically catering to users looking for safe, secure, and offline-first dialer alternatives.

### Fixed
- **Call-Initiation Keyboard Dismissal**: Implemented smart soft-keyboard auto-dismissal on call placement in `CallManager`, instantly closing any on-screen input boards when a call starts to prevent UI overlapping.
- **Screen Guard Overlay Stability**: Refined the transition between dialpad screen lock states and active calling overlays.

### Security
- **Independent Clean-Slate Identity**: Officially documented our independent code pedigree, certifying that the codebase is built 100% from scratch (no forks, repackaging, or legacy technical debt) for reliable safety audits.

## [1.0.2] - 2026-07-10

### Added
- **Favorites Tab**: Implemented a dedicated "Favorites" tab in the bottom navigation bar to directly view and call your starred contacts.
- **Polished Visuals for Favorites**: Added elegant Material 3 background card containers with proper spacing, color accents, and a streamlined layout that matches the Contacts and Recents tab.

### Fixed
- **Call Type Identification in Notifications**: Fixed an issue where all calls showed up as "Incoming call" in active notifications by correctly filtering active call notification triggers to only fire during `STATE_RINGING`.
- **Missed Call Notification Triggering**: Restored missed call notifications to prompt only for actual missed incoming calls, ignoring local rejections or completed outgoing calls.
- **Persistent Call Log Deletion**: Fixed an issue where deleted recent calls reappeared on app restart by correctly deleting them from both the internal SQLite Room database and the Android system content resolver (`CallLog.Calls`).

## [1.0.1] - 2026-07-09

### Fixed
- **Call History Details**: Implemented expandable call log items in the Recents tab, allowing users to view full history for specific contacts including timestamps and call durations.
- **Call Timer Accuracy**: Corrected the call timer logic to only start incrementing once the call is officially answered (`STATE_ACTIVE`), preventing the timer from starting during dialing or ringing states.
- **Recents Tab Visibility**: Resolved an issue where phone numbers were not appearing in the call history for unknown contacts by implementing robust name-to-number fallbacks.
- **Enhanced Data Display**: Added explicit phone number visibility in the Recents list subtext for better identification of callers.
- **Privacy Audit Compliance**: Standardized attribution context usage across `DialerViewModel`, `CallBlockerService`, and `MyInCallService` to resolve `attributionTag` manifest errors.
- **Manifest Integrity**: Restored missing closing tags in `AndroidManifest.xml` following privacy tag cleanup.

### Changed
- **Smart Call Grouping**: Organized the Recents tab into logical date buckets ("Today", "Yesterday", "Older") with sticky-style headers for faster scanning.
- **Missed Call Filtering**: Added M3 Filter Chips to the Recents screen, allowing users to instantly toggle between full history and missed calls only.
- **A-Z Fast Scroller**: Integrated a high-performance alphabet rail in the Contacts list with haptic-linked vertical drag for rapid navigation.
- **Improved Call Screen Feedback**: Updated the active call header to provide real-time status updates (e.g., "Dialing...", "Incoming call...", "Connecting...") based on the exact Telecom state.
- **Interactive Call Log**: Re-engineered the Recents tab to use an expandable card system, balancing primary call actions with deep dive history access.
- **Refined Dialpad UI**: Modularized dial button components and adjusted layout spacing (300dp width) for improved tap precision.
- **Enhanced Active Call Screen**: Updated call duration typography to `displaySmall` with primary coloring and added a subtle elevation surface tint.
- **Iconography Update**: Replaced text-based backspace with `AutoMirrored.Filled.Backspace` icon.

### Added
- **Automated Release Notes**: Integrated a custom extraction script in the CI pipeline to generate human-readable changelogs from this file during GitHub releases.
- **On-Demand History Fetching**: Optimized data usage by fetching detailed call histories only when requested via UI expansion.

## [1.0.0] - 2026-07-09

### Added
- Initial release of the Secure Dialer application.
- Material 3 design with dynamic color support.
- Comprehensive Dialer with search, speed dial, and call logs.
- Local contact management using Room database.
- Active call management via Telecom framework.
- Call screening and blocking capabilities.
- Privacy-focused architecture with explicit attribution tags.

### Fixed
- Attribution tag declaration errors in AndroidManifest.xml.
- Missing backslash in manifest merger.
- Various UI alignment and spacing issues for better ergonomics.

### Security
- Implemented `createAttributionContext` for fine-grained privacy auditing.
- Restricted permissions to minimum necessary for dialer functionality.

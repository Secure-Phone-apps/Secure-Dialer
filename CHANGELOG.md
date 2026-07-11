# Changelog

All notable changes to this project will be documented in this file.

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

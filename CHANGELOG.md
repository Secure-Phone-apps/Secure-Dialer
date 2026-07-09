# Changelog

All notable changes to this project will be documented in this file.

## [1.0.1] - 2026-07-09

### Changed
- **Refined Dialpad UI**: Modularized dial button components and adjusted layout spacing (300dp width) for improved tap precision.
- **Enhanced Active Call Screen**: Updated call duration typography to `displaySmall` with primary coloring and added a subtle elevation surface tint.
- **Iconography Update**: Replaced text-based backspace with `AutoMirrored.Filled.Backspace` icon.

### Fixed
- **Privacy Audit Compliance**: Standardized attribution context usage across `DialerViewModel`, `CallBlockerService`, and `MyInCallService` to resolve `attributionTag` manifest errors.
- **Manifest Integrity**: Restored missing closing tags in `AndroidManifest.xml` following privacy tag cleanup.

### Added
- **Automated Release Notes**: Integrated a custom extraction script in the CI pipeline to generate human-readable changelogs from this file during GitHub releases.

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

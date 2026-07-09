# Changelog

All notable changes to this project will be documented in this file.

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

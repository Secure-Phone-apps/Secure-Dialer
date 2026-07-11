## Description
Please describe the changes in this Pull Request, what problem they solve, and the motivation behind them.

## Type of Change
- [ ] Bug fix (non-breaking change which fixes an issue)
- [ ] New feature (non-breaking change which adds functionality)
- [ ] Refactoring / Code Condensing / Performance optimization
- [ ] Documentation update

## Core Security & Architecture Verification
As a security-first repository under **secure phone apps**, please confirm the following:
- [ ] **No Internet Permission**: This PR does not introduce any internet connectivity, API calls, or remote tracker dependency.
- [ ] **Native Components Only**: This PR leverages native Android framework utilities (`TelecomManager`, `ContactsContract`, standard Room / Jetpack Compose) without pulling in high-overhead or untrusted third-party libraries.
- [ ] **No Telemetry**: No third-party analytics or tracker SDKs have been added.

## Testing & Verification
- [ ] Compiled successfully with `./gradlew compileDebugSources` or `gradle :app:assembleDebug`.
- [ ] Tested on a physical device / emulator (please specify Android version: ______).
- [ ] Verified that permission handling is dynamic and doesn't crash the app.

## Screenshots (if applicable)
Please provide before/after screenshots for any user-interface changes.

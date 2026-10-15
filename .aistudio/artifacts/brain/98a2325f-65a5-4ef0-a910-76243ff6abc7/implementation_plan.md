# Implementation Plan: Silence Incoming Call & Call-Waiting via Volume and Lock Buttons

## Overview
Currently, when an incoming call arrives (either as a standalone call or as a second call while already active), pressing the volume buttons or the power/lock button does not silence the ringtone, vibration, flashlight, or call-waiting beeps.

This plan integrates the standard behavior from Google Phone and Samsung One UI: pressing any Volume key (Up or Down) or the Lock/Power button immediately silences the ringer, cancels vibration, stops camera flash strobing, and silences call-waiting tones without rejecting the call.

---

## Architecture & Proposed Changes

### 1. `InCallService` Framework Integration (`MyInCallService.kt`)
- **Override `onSilenceRinger()`**: Implement AOSP's official callback invoked by `TelecomManager` whenever hardware keys signal ringer silence.
- **Expose Public `silence()` Function**:
  - Stop the `Ringtone` playback immediately.
  - Cancel hardware `Vibrator` waveform.
  - Call `stopCallWaitingTone()` to mute secondary call beeps.
  - Call `FlashLightManager.stopFlashing(this)` to halt camera LED strobing.
- **Dynamic Lock/Screen-Off Receiver**:
  - Register an `ACTION_SCREEN_OFF` broadcast receiver inside `MyInCallService` whenever an incoming or waiting call is ringing.
  - When the user presses the Power/Lock button, `ACTION_SCREEN_OFF` fires -> invoke `silence()`.
  - Unregister cleanly when call state leaves `STATE_RINGING` or call disconnects to prevent memory leaks.

### 2. Activity Hardware Key Dispatch (`MainActivity.kt`)
- **Override `dispatchKeyEvent(event: KeyEvent)`**:
  - Detect `KEYCODE_VOLUME_DOWN` and `KEYCODE_VOLUME_UP` when `event.action == KeyEvent.ACTION_DOWN`.
  - Check if any call is in `Call.STATE_RINGING` or `CallManager.waitingCall.value != null`.
  - If ringing:
    - Invoke `CallManager.silenceRinger(this)` and `MyInCallService.silence()`.
    - Return `true` to consume the key event so system volume slider dialog is not awkwardly displayed over the incoming call screen.
  - If not ringing: delegate normally to `super.dispatchKeyEvent(event)`.

### 3. Centralized Call Manager Integration (`CallManager.kt`)
- Enhance `CallManager.silenceRinger(context)`:
  - Trigger `(telecomManager).silenceRinger()`.
  - Directly notify `inCallService?.silence()`.

---

## Verification & Testing Plan
1. **Compilation**: Run `compile_applet` to ensure clean Kotlin compilation.
2. **Automated Unit Tests**: Execute `gradle :app:testDebugUnitTest` to guarantee zero regressions.
3. **Scenario Testing**:
   - **Incoming Call + Volume Down/Up**: Ringer stops, vibration stops, flashlight stops; screen remains visible with Answer/Decline buttons.
   - **Incoming Call + Power/Lock**: Screen-off silences ringer and vibration; call remains incoming.
   - **Call-Waiting (2nd Call)**: While on Call 1, Call 2 arrives; pressing volume or lock silences the call-waiting tone immediately while Call 1 conversation continues uninterrupted.

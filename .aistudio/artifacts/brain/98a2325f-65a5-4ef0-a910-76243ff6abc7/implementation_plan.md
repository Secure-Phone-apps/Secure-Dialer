# Implementation Plan: Advanced Fake Call System Upgrades

## Overview
This plan addresses all items raised in the GitHub issue regarding the fake call feature:
1. **Critical Privacy Fix (Notification Disguise)**: Remove any mention of "Fake Call Simulation" or giveaway text from notification channels and banners so incoming fake calls mimic standard, authentic phone calls down to the notification metadata.
2. **Clock-Based Date & Time Scheduling**: Replace manual second/minute/hour delta calculations with a native Android Date & Time picker enabling precise target scheduling.
3. **Multiple Scheduled Fake Calls**: Enable managing multiple scheduled fake calls at different times/days with custom names and numbers persisted via Room.
4. **Custom Answered Audio Playback**: Support selecting an audio file or voice recording to play automatically when the fake call is answered.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> - **Notification Privacy**: The notification channel will be disguised as a standard high-priority incoming call channel (`incoming_call_channel_v4`) displaying standard incoming caller info without any "fake call" markers.
> - **Scheduling**: Users can pick a specific Date and Time on the clock rather than calculating offsets.

---

## 1. Overview & Core Concept
- **What It Does**: Upgrades the fake call feature into a stealthy, professional scheduling tool with zero giveaway metadata and support for multiple alarms and answered audio simulation.
- **Key Value**: Preserves complete discretion during sensitive situations while offering precise scheduling and realistic audio interaction.

---

## 2. User Experience & Visual Design
- **Stealth Notifications**: Notifications match genuine system incoming call notifications (Category `CATEGORY_CALL`, high priority, standard phone ringing style).
- **Scheduled Alarms List**: A clean card view in Advanced Settings showing all active scheduled fake calls with time remaining, caller name, and quick-cancel options.
- **Date/Time & Audio Pickers**: Native Material 3 pickers integrated seamlessly into `FakeCallSettings.kt`.

---

## 3. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────┐
│                   FAKE CALL UPGRADE                    │
├────────────────────────────────────────────────────────┤
│                                                        │
│   [FakeCallEntity (Room DB)]                           │
│   - id, timestamp, callerName, callerNumber, audioPath │
│                                                        │
│   [FakeCallReceiver / AlarmManager]                    │
│   - Exact Alarm scheduling                             │
│   - Stealth Notification (Disguised as standard call)  │
│                                                        │
│   [MediaPlayer Integration]                            │
│   - Plays custom audio uri upon fake call answer       │
│                                                        │
└────────────────────────────────────────────────────────┘
```

- **Persistence**: Store scheduled fake calls in Room DB (`FakeCallEntity`) so alarms persist across reboots via `BOOT_COMPLETED`.
- **Audio Playback**: Use `MediaPlayer` bound to `STREAM_VOICE_CALL` when `onFakeAnswer` is triggered.

---

## 4. Proposed Changes

### 1. Notification Privacy Disguise (`FakeCallReceiver.kt`)
- Change notification channel ID to use the standard incoming call channel with a discreet label.
- Remove any occurrence of "Fake Call Simulation" or giveaway text from notification titles/text.

### 2. Room Persistence & Multiple Alarms (`AppDatabase.kt`, `FakeCallDao.kt`)
- Create `FakeCallEntity` and DAO for scheduled fake calls.
- Update `FakeCallReceiver` to handle alarm persistence and exact scheduling.

### 3. UI Enhancements (`FakeCallSettings.kt`)
- Add date/time picker dialogs.
- Add scheduled alarms list and custom audio file picker using `ActivityResultContracts.OpenDocument()`.

---

## 5. Verification Plan
1. **Compilation**: Run `compile_applet`.
2. **Unit Tests**: Run `gradle :app:testDebugUnitTest`.
3. **Scenario Testing**: Verify stealth notifications, date/time scheduling, and answered audio playback.

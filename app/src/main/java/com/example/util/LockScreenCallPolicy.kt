/*
 * Copyright (C) 2026 MovStore
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.example.util

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.view.WindowManager
import androidx.compose.runtime.Immutable

enum class IncomingCallDisplayMode(val key: String) {
    NOTIFICATION("NOTIFICATION"),
    FULL_SCREEN("FULL_SCREEN");

    companion object {
        fun fromScreenOnKey(key: String?): IncomingCallDisplayMode {
            return entries.firstOrNull { it.key == key } ?: NOTIFICATION
        }

        fun fromScreenOffKey(key: String?): IncomingCallDisplayMode {
            return entries.firstOrNull { it.key == key } ?: FULL_SCREEN
        }
    }
}

@Immutable
data class IncomingCallPresentationDecision(
    val launchFullScreenUi: Boolean,
    val wakeScreen: Boolean,
    val attachFullScreenIntent: Boolean,
    val useSilentShadeNotification: Boolean
)

/**
 * Coordinates Android 9 (API 28) through Android 17+ (API 37) lockscreen presentation,
 * incoming call display modes, and optional unlock gating across AOSP and OEM ROMs.
 */
object LockScreenCallPolicy {

    const val PREFS_NAME = "dialer_prefs"
    const val PREF_REQUIRE_UNLOCK_TO_ANSWER = "require_unlock_for_lockscreen_calls"
    const val PREF_INCOMING_DISPLAY_SCREEN_ON = "incoming_call_display_screen_on"
    const val PREF_INCOMING_DISPLAY_SCREEN_OFF = "incoming_call_display_screen_off"

    fun getIncomingDisplayModeScreenOn(context: Context): IncomingCallDisplayMode {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(PREF_INCOMING_DISPLAY_SCREEN_ON, IncomingCallDisplayMode.NOTIFICATION.key)
        return IncomingCallDisplayMode.fromScreenOnKey(raw)
    }

    fun setIncomingDisplayModeScreenOn(context: Context, mode: IncomingCallDisplayMode) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(PREF_INCOMING_DISPLAY_SCREEN_ON, mode.key)
            .apply()
    }

    fun getIncomingDisplayModeScreenOff(context: Context): IncomingCallDisplayMode {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(PREF_INCOMING_DISPLAY_SCREEN_OFF, IncomingCallDisplayMode.FULL_SCREEN.key)
        return IncomingCallDisplayMode.fromScreenOffKey(raw)
    }

    fun setIncomingDisplayModeScreenOff(context: Context, mode: IncomingCallDisplayMode) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(PREF_INCOMING_DISPLAY_SCREEN_OFF, mode.key)
            .apply()
    }

    fun resolveIncomingCallPresentation(
        context: Context,
        isScreenInteractive: Boolean,
        isKeyguardLocked: Boolean
    ): IncomingCallPresentationDecision {
        return resolveIncomingCallPresentation(
            isScreenInteractive = isScreenInteractive,
            isKeyguardLocked = isKeyguardLocked,
            screenOnMode = getIncomingDisplayModeScreenOn(context),
            screenOffMode = getIncomingDisplayModeScreenOff(context)
        )
    }

    fun resolveIncomingCallPresentation(
        isScreenInteractive: Boolean,
        isKeyguardLocked: Boolean,
        screenOnMode: IncomingCallDisplayMode,
        screenOffMode: IncomingCallDisplayMode
    ): IncomingCallPresentationDecision {
        val isScreenOffOrLocked = !isScreenInteractive || isKeyguardLocked
        val selectedMode = if (isScreenOffOrLocked) screenOffMode else screenOnMode
        val wantsFullScreen = selectedMode == IncomingCallDisplayMode.FULL_SCREEN
        val shouldAttachFullScreenIntent = isScreenOffOrLocked && wantsFullScreen

        return IncomingCallPresentationDecision(
            launchFullScreenUi = wantsFullScreen,
            wakeScreen = isScreenOffOrLocked && wantsFullScreen && !isScreenInteractive,
            attachFullScreenIntent = shouldAttachFullScreenIntent,
            useSilentShadeNotification = wantsFullScreen && !shouldAttachFullScreenIntent
        )
    }

    fun isRequireUnlockToAnswerEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(PREF_REQUIRE_UNLOCK_TO_ANSWER, false)
    }

    fun setRequireUnlockToAnswerEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(PREF_REQUIRE_UNLOCK_TO_ANSWER, enabled)
            .apply()
    }

    fun isDeviceLocked(context: Context): Boolean {
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager ?: return false
        return km.isKeyguardLocked
    }

    /**
     * Never dismiss keyguard automatically on automatic call presentation.
     * Automatic dismissal forces the OS lockscreen challenge above the incoming call UI.
     */
    fun shouldPromptUnlockBeforeAnswering(context: Context): Boolean {
        return isRequireUnlockToAnswerEnabled(context) && isDeviceLocked(context)
    }

    /**
     * Applies window lockscreen flags without triggering KeyguardManager.requestDismissKeyguard().
     */
    fun applyLockScreenWindowFlags(
        activity: Activity,
        showWhenLocked: Boolean,
        keepScreenOn: Boolean = showWhenLocked,
        turnScreenOn: Boolean = false
    ) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                activity.setShowWhenLocked(showWhenLocked)
                activity.setTurnScreenOn(turnScreenOn)
            }
            @Suppress("DEPRECATION")
            if (showWhenLocked) {
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
            } else {
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
            }

            @Suppress("DEPRECATION")
            if (turnScreenOn) {
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
            } else {
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
            }

            if (keepScreenOn) {
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        } catch (_: Exception) {}
    }

    /**
     * Explicitly asks Android to dismiss the keyguard only for a sensitive user-initiated action
     * (e.g., Add Call, Call Notes, or Answer when "Require Unlock to Answer Calls" is enabled).
     */
    fun requestUnlockThen(activity: Activity, onUnlocked: () -> Unit) {
        val km = activity.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        if (km == null || !km.isKeyguardLocked) {
            onUnlocked()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                km.requestDismissKeyguard(
                    activity,
                    object : KeyguardManager.KeyguardDismissCallback() {
                        override fun onDismissSucceeded() {
                            if (!activity.isFinishing && !activity.isDestroyed) {
                                activity.runOnUiThread(onUnlocked)
                            }
                        }

                        override fun onDismissError() {
                            // If OEM keyguard was actually already cleared, proceed safely
                            if (!km.isKeyguardLocked && !activity.isFinishing && !activity.isDestroyed) {
                                activity.runOnUiThread(onUnlocked)
                            }
                        }

                        override fun onDismissCancelled() {
                            // User cancelled unlock; keep sensitive action gated
                        }
                    }
                )
            } catch (_: Exception) {
                if (!km.isKeyguardLocked) onUnlocked()
            }
        } else {
            onUnlocked()
        }
    }
}

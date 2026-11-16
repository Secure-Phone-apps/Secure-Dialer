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

/**
 * Coordinates Android 9 (API 28) through Android 17+ (API 37) lockscreen presentation
 * and optional unlock gating across AOSP and OEM ROMs (Samsung One UI, Motorola, Xiaomi, Pixel).
 */
object LockScreenCallPolicy {

    const val PREFS_NAME = "dialer_prefs"
    const val PREF_REQUIRE_UNLOCK_TO_ANSWER = "require_unlock_for_lockscreen_calls"

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
    fun applyLockScreenWindowFlags(activity: Activity, showWhenLocked: Boolean, keepScreenOn: Boolean = showWhenLocked) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                activity.setShowWhenLocked(showWhenLocked)
                activity.setTurnScreenOn(showWhenLocked)
            }
            @Suppress("DEPRECATION")
            val lockFlags = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON

            if (showWhenLocked) {
                activity.window.addFlags(lockFlags)
            } else {
                activity.window.clearFlags(lockFlags)
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

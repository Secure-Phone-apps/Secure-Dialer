/*
 * Copyright (C) 2026 MovStore
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.example.ui.viewmodel

import android.content.SharedPreferences
import com.example.*
import com.example.model.AppSetting
import com.example.util.CallAudioRecorder
import com.example.util.RecordingCompressionProfile
import com.example.util.RichHapticEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class DialerSettingsCoordinator(
    private val repository: DialerRepository,
    private val prefs: SharedPreferences,
    private val scope: CoroutineScope
) {

    fun updateSettingString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
        scope.launch {
            try {
                repository.dao.insertSetting(AppSetting(key, value))
            } catch (_: Exception) {}
        }
    }

    fun updateSettingBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
        scope.launch {
            try {
                repository.dao.insertSetting(AppSetting(key, value.toString()))
            } catch (_: Exception) {}
        }
    }

    fun updateDarkTheme(dark: Boolean) {
        prefs.edit().putBoolean("is_dark_theme", dark).apply()
    }

    fun updateAmoledMode(amoled: Boolean, isDarkTheme: Boolean, onDarkThemeForceTrue: () -> Unit) {
        val editor = prefs.edit().putBoolean("is_amoled_mode", amoled)
        if (amoled && !isDarkTheme) {
            onDarkThemeForceTrue()
            editor.putBoolean("is_dark_theme", true)
        }
        editor.apply()
    }

    fun updateCustomColorHex(hex: String) {
        prefs.edit().putString("custom_color_hex", hex).apply()
    }

    fun updateM3Expressive(expressive: Boolean) {
        prefs.edit().putBoolean("is_m3_expressive", expressive).apply()
    }

    fun updateAvatarShapeType(shapeType: String) {
        prefs.edit().putString("avatar_shape_type", shapeType).apply()
    }

    fun updateUseDynamicColor(dynamic: Boolean) {
        prefs.edit().putBoolean("use_dynamic_color", dynamic).apply()
    }

    fun updateThemeColor(color: String) {
        prefs.edit().putString("theme_color", color).apply()
    }

    fun updateRowSwipeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("is_row_swipe_enabled", enabled).apply()
    }

    fun updateVibrateOnClickEnabled(enabled: Boolean) {
        RichHapticEngine.setHapticsEnabled(enabled)
        updateSettingBoolean("vibrate_on_click_enabled", enabled)
    }

    fun updateRecordingCompressionProfile(profile: RecordingCompressionProfile) {
        CallAudioRecorder.setCompressionProfile(repository.context, profile)
        updateSettingString("recording_compression_profile", profile.key)
    }

    fun updatePreferredSim(sim: String) {
        prefs.edit().putString("preferred_sim", sim).apply()
        scope.launch { repository.savePreferredSim(sim) }
    }

    fun updateVoicemailNumber(num: String) {
        scope.launch { repository.saveVoicemailNumber(num) }
    }
}

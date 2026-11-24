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

    fun updateSettingInt(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
        scope.launch {
            try {
                repository.dao.insertSetting(AppSetting(key, value.toString()))
            } catch (_: Exception) {}
        }
    }

    fun updateDarkTheme(dark: Boolean) {
        updateSettingBoolean("is_dark_theme", dark)
    }

    fun updateAmoledMode(amoled: Boolean, isDarkTheme: Boolean, onDarkThemeForceTrue: () -> Unit) {
        updateSettingBoolean("is_amoled_mode", amoled)
        if (amoled && !isDarkTheme) {
            onDarkThemeForceTrue()
            updateSettingBoolean("is_dark_theme", true)
        }
    }

    fun updateCustomColorHex(hex: String) {
        updateSettingString("custom_color_hex", hex)
    }

    fun updateM3Expressive(expressive: Boolean) {
        updateSettingBoolean("is_m3_expressive", expressive)
    }

    fun updateAvatarShapeType(shapeType: String) {
        updateSettingString("avatar_shape_type", shapeType)
    }

    fun updateUseDynamicColor(dynamic: Boolean) {
        updateSettingBoolean("use_dynamic_color", dynamic)
    }

    fun updateThemeColor(color: String) {
        updateSettingString("theme_color", color)
    }

    fun updateRowSwipeEnabled(enabled: Boolean) {
        updateSettingBoolean("is_row_swipe_enabled", enabled)
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

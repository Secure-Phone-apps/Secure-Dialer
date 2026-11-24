/*
 * Copyright (C) 2026 MovStore
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.example.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.annotation.StringRes
import com.example.R
import java.util.Locale

data class SupportedLanguage(
    val tag: String,
    val nativeName: String,
    val englishName: String
)

object LocaleHelper {
    private const val PREFS_NAME = "secure_dialer_locale"
    private const val KEY_LANG = "app_language_tag"

    val SUPPORTED_LANGUAGES = listOf(
        SupportedLanguage("", "System Default", "System Default"),
        SupportedLanguage("en", "English", "English"),
        SupportedLanguage("ar", "العربية", "Arabic"),
        SupportedLanguage("de", "Deutsch", "German"),
        SupportedLanguage("es", "Español", "Spanish"),
        SupportedLanguage("fr", "Français", "French"),
        SupportedLanguage("hi", "हिन्दी", "Hindi"),
        SupportedLanguage("ja", "日本語", "Japanese"),
        SupportedLanguage("pl", "Polski", "Polish"),
        SupportedLanguage("pt", "Português", "Portuguese"),
        SupportedLanguage("tr", "Türkçe", "Turkish"),
        SupportedLanguage("zh", "中文", "Chinese")
    )

    fun getPersistedLanguageTag(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANG, "") ?: ""
    }

    fun setPersistedLanguageTag(context: Context, languageTag: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANG, languageTag)
            .apply()
    }

    fun getActiveLocale(context: Context): Locale {
        val tag = getPersistedLanguageTag(context)
        return if (tag.isBlank()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context.resources.configuration.locales.get(0) ?: Locale.getDefault()
            } else {
                @Suppress("DEPRECATION")
                context.resources.configuration.locale ?: Locale.getDefault()
            }
        } else {
            Locale.forLanguageTag(tag)
        }
    }

    fun applyLocale(context: Context): Context {
        val tag = getPersistedLanguageTag(context)
        if (tag.isBlank()) return context

        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(locale))
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }
        config.setLayoutDirection(locale)

        return context.createConfigurationContext(config)
    }
}

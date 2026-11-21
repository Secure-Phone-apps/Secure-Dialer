/*
 * Copyright (C) 2026 MovStore
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.example

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.LocaleList
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.w3c.dom.Element
import java.io.File
import java.util.regex.Pattern
import javax.xml.parsers.DocumentBuilderFactory

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Batch4LocalizationAndClirTest {

    private val supportedLocales = listOf("ar", "de", "es", "fr", "hi", "ja", "pl", "pt", "tr", "zh")

    private fun parseStrings(file: File): Map<String, String> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val nodes = doc.getElementsByTagName("string")
        val map = LinkedHashMap<String, String>(nodes.length)
        for (i in 0 until nodes.length) {
            val el = nodes.item(i) as Element
            map[el.getAttribute("name")] = el.textContent ?: ""
        }
        return map
    }

    @Test
    fun `all 11 string files maintain exact key parity and exact format specifier type match`() {
        val baseMap = parseStrings(File("src/main/res/values/strings.xml"))
        assertTrue(baseMap.size >= 700)

        val specifierRegex = Pattern.compile("%(?:(\\d+)\\$)?([a-zA-Z])")
        fun extractSpecifiers(text: String): List<String> {
            val m = specifierRegex.matcher(text)
            val list = mutableListOf<String>()
            while (m.find()) list.add(m.group())
            return list
        }

        for (loc in supportedLocales) {
            val locMap = parseStrings(File("src/main/res/values-$loc/strings.xml"))
            assertEquals("Key count parity mismatch for $loc", baseMap.size, locMap.size)
            assertEquals("Key order/set mismatch for $loc", baseMap.keys.toList(), locMap.keys.toList())

            for ((key, enVal) in baseMap) {
                val locVal = locMap[key] ?: ""
                assertEquals(
                    "Exact specifier type/index mismatch in $loc/$key",
                    extractSpecifiers(enVal),
                    extractSpecifiers(locVal)
                )
            }
        }
    }

    @Test
    fun `task 4_1 and 4_2 targeted fixes and all 6 SIM rename translations are verified`() {
        val baseMap = parseStrings(File("src/main/res/values/strings.xml"))
        assertTrue("Base Japan CLIR must use 184", baseMap["settings_clir_prefix_japan"]?.contains("184") == true)
        assertFalse("Base Japan CLIR must not use 1831", baseMap["settings_clir_prefix_japan"]?.contains("1831") == true)

        val simKeys = listOf(
            "settings_rename_sims",
            "settings_rename_sims_sub",
            "dialog_rename_sim_title",
            "dialog_rename_sim_prompt",
            "sim_label_placeholder",
            "toast_sim_renamed"
        )

        for (loc in supportedLocales) {
            val locMap = parseStrings(File("src/main/res/values-$loc/strings.xml"))
            assertTrue("$loc Japan CLIR must contain 184", locMap["settings_clir_prefix_japan"]?.contains("184") == true)
            assertFalse("$loc Japan CLIR must not contain 1831", locMap["settings_clir_prefix_japan"]?.contains("1831") == true)
            for (k in simKeys) {
                assertFalse(
                    "Key '$k' must be localized (not verbatim English) in $loc",
                    locMap[k] == baseMap[k]
                )
            }
        }

        // German specific fixes
        val deMap = parseStrings(File("src/main/res/values-de/strings.xml"))
        assertEquals("Anruf planen (%1\$d Sek.)", deMap["schedule_escape_call"])
        assertEquals("NAVIGATION & ANSICHT", deMap["header_navigation_layout"])
        assertTrue(deMap["spam_csv_placeholder"]?.contains("\\n") == true)

        // French specific fixes
        val frMap = parseStrings(File("src/main/res/values-fr/strings.xml"))
        assertEquals("Actif", frMap["battery_schedule_show_now"])
        assertEquals("Contacter %1\$s sur WhatsApp", frMap["dialog_whatsapp_contact"])

        // Arabic specific fixes
        val arMap = parseStrings(File("src/main/res/values-ar/strings.xml"))
        assertEquals("تمت التصفية بواسطة %1\$s", arMap["screened_by_app"])
        assertFalse(arMap["spam_csv_placeholder"] == baseMap["spam_csv_placeholder"])

        // Turkish & Portuguese stray fixes
        val trMap = parseStrings(File("src/main/res/values-tr/strings.xml"))
        assertFalse(trMap["spam_csv_placeholder"] == baseMap["spam_csv_placeholder"])
        val ptMap = parseStrings(File("src/main/res/values-pt/strings.xml"))
        assertEquals("WhatsApp para %1\$s", ptMap["dialog_whatsapp_contact"])

        // Verify Kotlin Japan CLIR preset uses 184
        val clirOut = CallManager.formatOutgoingNumberWithClir("09012345678", isHideCallerId = true, clirPrefix = "184")
        assertEquals("18409012345678", clirOut)
    }

    @Test
    fun `locales_config xml registers all 11 supported BCP-47 locales and manifest declares it`() {
        val localesXml = File("src/main/res/xml/locales_config.xml")
        assertTrue("res/xml/locales_config.xml must exist", localesXml.exists())

        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(localesXml)
        val localeNodes = doc.getElementsByTagName("locale")
        val declared = mutableSetOf<String>()
        for (i in 0 until localeNodes.length) {
            val el = localeNodes.item(i) as Element
            declared.add(el.getAttribute("android:name"))
        }

        val expected = (listOf("en") + supportedLocales).toSet()
        assertEquals(expected, declared)

        val manifestText = File("src/main/AndroidManifest.xml").readText()
        assertTrue(
            "AndroidManifest.xml must declare android:localeConfig=\"@xml/locales_config\"",
            manifestText.contains("android:localeConfig=\"@xml/locales_config\"")
        )
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun `runtime android 13 context resolves locales_config cleanly`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val resId = app.resources.getIdentifier("locales_config", "xml", app.packageName)
        assertTrue("locales_config XML resource must be packaged into APK", resId != 0)
    }
}

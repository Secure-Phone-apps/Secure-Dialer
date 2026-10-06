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

package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import java.util.regex.Pattern
import javax.xml.parsers.DocumentBuilderFactory

class LocalizationIntegrityTest {

    private fun loadStrings(file: File): Map<String, String> {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(file)
        val nodeList = doc.getElementsByTagName("string")
        val map = mutableMapOf<String, String>()

        for (i in 0 until nodeList.length) {
            val node = nodeList.item(i) as Element
            val name = node.getAttribute("name")
            val text = node.textContent ?: ""
            assertTrue("Duplicate key detected: $name in ${file.name}", !map.containsKey(name))
            map[name] = text
        }
        return map
    }

    private val supportedLocales = listOf("ar", "de", "es", "fr", "hi", "ja", "pl", "pt", "zh")

    private fun loadOrderedKeys(file: File): List<String> {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(file)
        val nodeList = doc.getElementsByTagName("string")
        val keys = mutableListOf<String>()
        for (i in 0 until nodeList.length) {
            val node = nodeList.item(i) as Element
            keys.add(node.getAttribute("name"))
        }
        return keys
    }

    @Test
    fun testAllLocalesCompletenessParityAndFormatIntegrity() {
        val baseFile = File("src/main/res/values/strings.xml")
        assertTrue("English strings.xml must exist", baseFile.exists())
        val enStrings = loadStrings(baseFile)
        val enOrderedKeys = loadOrderedKeys(baseFile)

        assertTrue("Canonical strings must not be empty", enStrings.isNotEmpty())

        val formatPattern = Pattern.compile("%(?:(\\d+)\\$)?([a-zA-Z])")

        for (locale in supportedLocales) {
            val localeFile = File("src/main/res/values-$locale/strings.xml")
            assertTrue("Locale file for '$locale' must exist at values-$locale/strings.xml", localeFile.exists())

            val locStrings = loadStrings(localeFile)
            val locOrderedKeys = loadOrderedKeys(localeFile)

            // 1. Completeness: Ensure 100% key parity (0 missing, 0 extra)
            val missingKeys = enStrings.keys - locStrings.keys
            assertEquals("Missing keys in locale '$locale': $missingKeys", emptySet<String>(), missingKeys)

            val extraKeys = locStrings.keys - enStrings.keys
            assertEquals("Orphaned/extra keys in locale '$locale': $extraKeys", emptySet<String>(), extraKeys)

            // 2. Structural Order: Ensure exact identical key sequence matching canonical English sections
            assertEquals("Key order mismatch in locale '$locale'", enOrderedKeys, locOrderedKeys)

            // 3. Format Specifier Parity: %1$s, %1$d must match to prevent runtime formatting crashes
            for ((key, enText) in enStrings) {
                val locText = locStrings[key] ?: continue

                val enMatcher = formatPattern.matcher(enText)
                val enSpecifiers = mutableListOf<String>()
                while (enMatcher.find()) {
                    enSpecifiers.add(enMatcher.group())
                }

                val locMatcher = formatPattern.matcher(locText)
                val locSpecifiers = mutableListOf<String>()
                while (locMatcher.find()) {
                    locSpecifiers.add(locMatcher.group())
                }

                assertEquals(
                    "Format specifier mismatch in key '$key' for locale '$locale': EN has $enSpecifiers, $locale has $locSpecifiers",
                    enSpecifiers.size,
                    locSpecifiers.size
                )

                // 4. Content sanity: Strings must not be empty or blank
                assertTrue("Key '$key' in locale '$locale' must not be blank", locText.isNotBlank())
            }
        }
    }

    @Test
    fun testXmlEntitySafety() {
        // Ensure no raw unescaped & exists inside string tags in any strings.xml file
        val allFiles = listOf(File("src/main/res/values/strings.xml")) +
                supportedLocales.map { File("src/main/res/values-$it/strings.xml") }

        val bareAmpRegex = Regex("&(?!(amp|lt|gt|quot|apos|#\\d+|#x[0-9a-fA-F]+);)")
        val commentRegex = Regex("<!--[\\s\\S]*?-->")
        for (file in allFiles) {
            val contentWithoutComments = commentRegex.replace(file.readText(), "")
            val match = bareAmpRegex.find(contentWithoutComments)
            assertTrue("File ${file.name} contains unescaped '&' at match: ${match?.value}", match == null)
        }
    }
}

/*
 * Copyright (C) 2026 MovStore
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.example

import android.Manifest
import android.app.Application
import android.content.ComponentName
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.example.ui.buildOnboardingPermissionsList
import com.example.util.CallScreenPermissionHelper
import com.example.util.OemFamily
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Batch2ManifestAndPermissionsTest {

    private lateinit var context: Application

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `manifest queries declare every OEM package referenced by CallScreenPermissionHelper and keep INTERNET removed`() {
        val manifestFile = File("src/main/AndroidManifest.xml")
        assertTrue("AndroidManifest.xml must exist", manifestFile.exists())

        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(manifestFile)
        val packageNodes = doc.getElementsByTagName("package")
        val declaredQueryPackages = mutableSetOf<String>()
        for (i in 0 until packageNodes.length) {
            val nameAttr = packageNodes.item(i).attributes?.getNamedItem("android:name")?.nodeValue
            if (!nameAttr.isNullOrBlank()) {
                declaredQueryPackages.add(nameAttr)
            }
        }

        val expectedOemPackages = OemFamily.entries
            .flatMap { family -> CallScreenPermissionHelper.getOemBackgroundIntentCandidates(context, family) }
            .mapNotNull { intent -> intent.component?.packageName }
            .toSet()

        assertTrue("Expected non-empty OEM candidate packages", expectedOemPackages.size >= 13)
        val missingPackages = expectedOemPackages - declaredQueryPackages
        assertTrue(
            "Manifest <queries> missing OEM packages: $missingPackages",
            missingPackages.isEmpty()
        )

        val pkgInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS
        )
        val requestedPerms = pkgInfo.requestedPermissions?.toSet().orEmpty()
        assertFalse(
            "INTERNET permission must remain strictly removed",
            requestedPerms.contains(Manifest.permission.INTERNET)
        )
    }

    @Test
    fun `main and inCall activities declare hardened configChanges for foldables DPI scaling and dark mode`() {
        val pm = context.packageManager
        val requiredFlags = ActivityInfo.CONFIG_ORIENTATION or
            ActivityInfo.CONFIG_SCREEN_SIZE or
            ActivityInfo.CONFIG_SMALLEST_SCREEN_SIZE or
            ActivityInfo.CONFIG_SCREEN_LAYOUT or
            ActivityInfo.CONFIG_DENSITY or
            ActivityInfo.CONFIG_FONT_SCALE or
            ActivityInfo.CONFIG_LOCALE or
            ActivityInfo.CONFIG_LAYOUT_DIRECTION or
            ActivityInfo.CONFIG_UI_MODE or
            ActivityInfo.CONFIG_KEYBOARD_HIDDEN

        listOf(MainActivity::class.java, InCallActivity::class.java).forEach { activityClass ->
            val info = pm.getActivityInfo(ComponentName(context, activityClass), 0)
            assertTrue(
                "${activityClass.simpleName} missing required configChanges flags (actual=0x${info.configChanges.toString(16)})",
                (info.configChanges and requiredFlags) == requiredFlags
            )
        }
    }

    @Test
    fun `onboarding permissions include READ_PHONE_STATE on all APIs and BLUETOOTH_CONNECT on API 31+`() {
        val api28 = buildOnboardingPermissionsList(Build.VERSION_CODES.P).toSet()
        assertTrue(api28.contains(Manifest.permission.READ_PHONE_STATE))
        assertTrue(api28.contains(Manifest.permission.READ_CONTACTS))
        assertTrue(api28.contains(Manifest.permission.READ_CALL_LOG))
        assertTrue(api28.contains(Manifest.permission.CALL_PHONE))
        assertFalse(api28.contains(Manifest.permission.BLUETOOTH_CONNECT))
        assertFalse(api28.contains(Manifest.permission.POST_NOTIFICATIONS))

        val api31 = buildOnboardingPermissionsList(Build.VERSION_CODES.S).toSet()
        assertTrue(api31.contains(Manifest.permission.READ_PHONE_STATE))
        assertTrue(api31.contains(Manifest.permission.BLUETOOTH_CONNECT))
        assertFalse(api31.contains(Manifest.permission.POST_NOTIFICATIONS))

        val api36 = buildOnboardingPermissionsList(36).toSet()
        assertTrue(api36.contains(Manifest.permission.READ_PHONE_STATE))
        assertTrue(api36.contains(Manifest.permission.BLUETOOTH_CONNECT))
        assertTrue(api36.contains(Manifest.permission.POST_NOTIFICATIONS))
    }
}

/*
 * Copyright (C) 2026 MovStore
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.example

import android.app.Activity
import android.app.Application
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.UserManager
import androidx.activity.ComponentActivity
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.DatabaseIntegrityStatus
import com.example.util.VaultBiometricAuthHelper
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowUserManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Batch1P0CrashesAndDbSafetyTest {

    private lateinit var context: Application

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        AppDatabase.resetForTesting()
    }

    @After
    fun tearDown() {
        AppDatabase.resetForTesting()
    }

    @Test
    fun `selectAuthPath isolates setDeviceCredentialAllowed strictly to API 29 and routes API 24-28 to Keyguard confirm`() {
        assertEquals(
            VaultBiometricAuthHelper.AuthPath.KEYGUARD_CREDENTIAL_CONFIRM,
            VaultBiometricAuthHelper.selectAuthPath(Build.VERSION_CODES.N)
        )
        assertEquals(
            VaultBiometricAuthHelper.AuthPath.KEYGUARD_CREDENTIAL_CONFIRM,
            VaultBiometricAuthHelper.selectAuthPath(Build.VERSION_CODES.O)
        )
        assertEquals(
            VaultBiometricAuthHelper.AuthPath.KEYGUARD_CREDENTIAL_CONFIRM,
            VaultBiometricAuthHelper.selectAuthPath(Build.VERSION_CODES.P)
        )
        assertEquals(
            VaultBiometricAuthHelper.AuthPath.BIOMETRIC_PROMPT_Q,
            VaultBiometricAuthHelper.selectAuthPath(Build.VERSION_CODES.Q)
        )
        assertEquals(
            VaultBiometricAuthHelper.AuthPath.BIOMETRIC_PROMPT_R_PLUS,
            VaultBiometricAuthHelper.selectAuthPath(Build.VERSION_CODES.R)
        )
        assertEquals(
            VaultBiometricAuthHelper.AuthPath.BIOMETRIC_PROMPT_R_PLUS,
            VaultBiometricAuthHelper.selectAuthPath(36)
        )
    }

    @Implements(KeyguardManager::class)
    class ShadowConfirmKeyguardManager : org.robolectric.shadows.ShadowKeyguardManager() {
        companion object {
            var confirmIntentToReturn: Intent? = Intent("android.app.action.CONFIRM_DEVICE_CREDENTIAL")
        }

        @Implementation
        @Suppress("DEPRECATION")
        fun createConfirmDeviceCredentialIntent(title: CharSequence?, description: CharSequence?): Intent? {
            return confirmIntentToReturn
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P], shadows = [ShadowConfirmKeyguardManager::class])
    fun `android 9 api 28 requires real Keyguard ActivityResult OK before invoking onSuccess`() {
        ShadowConfirmKeyguardManager.confirmIntentToReturn = Intent("android.app.action.CONFIRM_DEVICE_CREDENTIAL")
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val km = activity.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        Shadows.shadowOf(km).setIsDeviceSecure(true)

        var successCount = 0
        var errorCount = 0

        VaultBiometricAuthHelper.authenticate(
            activity = activity,
            onSuccess = { successCount++ },
            onError = { errorCount++ }
        )

        // CRITICAL: Must NOT bypass lock and call onSuccess synchronously before credential confirmation!
        assertEquals(0, successCount)
        assertEquals(0, errorCount)

        val launched = Shadows.shadowOf(activity).nextStartedActivityForResult
        assertNotNull("Expected confirm credential intent to launch for result", launched)

        // Simulate user cancelling PIN/pattern screen
        activity.activityResultRegistry.dispatchResult(launched.requestCode, Activity.RESULT_CANCELED, null)
        assertEquals(0, successCount)
        assertEquals(1, errorCount)

        // Now test user entering valid PIN/pattern (RESULT_OK)
        var okSuccessCount = 0
        var okErrorCount = 0
        VaultBiometricAuthHelper.authenticate(
            activity = activity,
            onSuccess = { okSuccessCount++ },
            onError = { okErrorCount++ }
        )
        assertEquals(0, okSuccessCount)
        val secondLaunch = Shadows.shadowOf(activity).nextStartedActivityForResult
        assertNotNull(secondLaunch)
        activity.activityResultRegistry.dispatchResult(secondLaunch.requestCode, Activity.RESULT_OK, null)
        assertEquals(1, okSuccessCount)
        assertEquals(0, okErrorCount)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P], shadows = [ShadowConfirmKeyguardManager::class])
    fun `android 9 api 28 rejects access cleanly when Keyguard confirm intent is unavailable on secured device`() {
        ShadowConfirmKeyguardManager.confirmIntentToReturn = null
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val km = activity.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        Shadows.shadowOf(km).setIsDeviceSecure(true)

        var successCalled = false
        var errorMessage: String? = null

        VaultBiometricAuthHelper.authenticate(
            activity = activity,
            onSuccess = { successCalled = true },
            onError = { errorMessage = it }
        )

        assertFalse("Must never silently unlock when device is secure and confirm intent is null", successCalled)
        assertNotNull(errorMessage)
    }

    @Test
    fun `direct boot transient keystore error stays locked before unlock and auto-recovers when unlocked`() {
        val um = context.getSystemService(UserManager::class.java)
        val shadowUm: ShadowUserManager = Shadows.shadowOf(um)

        // Prime an initial in-memory instance created during locked Direct Boot state
        AppDatabase.getDatabase(context)
        AppDatabase.resetForTesting(DatabaseIntegrityStatus.TransientKeystoreError("DirectBoot locked"))
        // Rebuild once so INSTANCE is cached alongside a simulated TransientKeystoreError
        val initialDb = AppDatabase.getDatabase(context)
        assertNotNull(initialDb)

        // Force TransientKeystoreError state while keeping INSTANCE alive (simulating background boot open)
        val statusField = AppDatabase::class.java.getDeclaredField("_integrityStatus").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val stateFlow = statusField.get(AppDatabase.Companion) as kotlinx.coroutines.flow.MutableStateFlow<DatabaseIntegrityStatus>
        stateFlow.value = DatabaseIntegrityStatus.TransientKeystoreError("Keystore locked during Direct Boot")

        shadowUm.setUserUnlocked(false)
        assertFalse(AppDatabase.isDeviceUserUnlocked(context))
        assertFalse(AppDatabase.retryOpenOnTransientRecoveryIfUnlocked(context))
        assertTrue(AppDatabase.integrityStatus.value is DatabaseIntegrityStatus.TransientKeystoreError)

        // Device unlocks -> auto recovery succeeds via getDatabase() or explicit retryOpenOnTransientRecoveryIfUnlocked()
        shadowUm.setUserUnlocked(true)
        assertTrue(AppDatabase.isDeviceUserUnlocked(context))
        AppDatabase.getDatabase(context)
        assertFalse(
            "Database must recover out of TransientKeystoreError automatically once user unlocks",
            AppDatabase.integrityStatus.value is DatabaseIntegrityStatus.TransientKeystoreError
        )
    }

    @Test
    fun `NativeCryptoLoadError is distinct from RobolectricTestFallback and eligible for retry`() {
        val statusField = AppDatabase::class.java.getDeclaredField("_integrityStatus").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val stateFlow = statusField.get(AppDatabase.Companion) as kotlinx.coroutines.flow.MutableStateFlow<DatabaseIntegrityStatus>

        val nativeErr = DatabaseIntegrityStatus.NativeCryptoLoadError("dlopen libsqlcipher.so failed")
        stateFlow.value = nativeErr

        assertTrue(AppDatabase.integrityStatus.value is DatabaseIntegrityStatus.NativeCryptoLoadError)
        assertFalse(AppDatabase.integrityStatus.value is DatabaseIntegrityStatus.RobolectricTestFallback)

        // Explicit retry transitions cleanly (in Robolectric test runtime -> RobolectricTestFallback)
        assertTrue(AppDatabase.retryOpenOnTransientRecoveryIfUnlocked(context, force = true))
        assertTrue(AppDatabase.integrityStatus.value is DatabaseIntegrityStatus.RobolectricTestFallback)
    }
}

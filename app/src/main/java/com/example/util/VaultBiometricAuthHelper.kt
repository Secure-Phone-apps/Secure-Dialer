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

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Native Biometric & Device Credential helper for the Recording Vault, Private Contacts Vault,
 * and App Lock.
 * - API 30+ (Android 11-17+): BiometricPrompt with BIOMETRIC_WEAK or DEVICE_CREDENTIAL.
 * - API 29 (Android 10): BiometricPrompt with setDeviceCredentialAllowed(true).
 * - API 24-28 (Android 7-9): KeyguardManager.createConfirmDeviceCredentialIntent with real
 *   ActivityResult confirmation (prevents API 28 NoSuchMethodError and API 24-28 instant bypass).
 */
object VaultBiometricAuthHelper {

    private val requestCounter = AtomicInteger(0)

    internal enum class AuthPath {
        BIOMETRIC_PROMPT_R_PLUS,
        BIOMETRIC_PROMPT_Q,
        KEYGUARD_CREDENTIAL_CONFIRM
    }

    internal fun selectAuthPath(sdkInt: Int = Build.VERSION.SDK_INT): AuthPath {
        return when {
            sdkInt >= Build.VERSION_CODES.R -> AuthPath.BIOMETRIC_PROMPT_R_PLUS
            sdkInt == Build.VERSION_CODES.Q -> AuthPath.BIOMETRIC_PROMPT_Q
            else -> AuthPath.KEYGUARD_CREDENTIAL_CONFIRM
        }
    }

    fun authenticate(
        activity: Activity,
        title: String = "Unlock Recording Vault",
        subtitle: String = "Authenticate to access confidential call recordings",
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        val keyguardManager = activity.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val isDeviceSecure = keyguardManager?.isDeviceSecure == true

        if (!isDeviceSecure) {
            // No lockscreen PIN/pattern/fingerprint configured on the device
            onSuccess()
            return
        }

        val completed = AtomicBoolean(false)
        val finishSuccess = {
            if (completed.compareAndSet(false, true)) {
                onSuccess()
            }
        }
        val finishError: (String) -> Unit = { message ->
            if (completed.compareAndSet(false, true)) {
                onError(message)
            }
        }

        when (selectAuthPath()) {
            AuthPath.BIOMETRIC_PROMPT_R_PLUS -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    try {
                        val executor = ContextCompat.getMainExecutor(activity)
                        val cancellationSignal = CancellationSignal()
                        val authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK or
                            BiometricManager.Authenticators.DEVICE_CREDENTIAL
                        val prompt = BiometricPrompt.Builder(activity)
                            .setTitle(title)
                            .setSubtitle(subtitle)
                            .setAllowedAuthenticators(authenticators)
                            .build()

                        prompt.authenticate(
                            cancellationSignal,
                            executor,
                            createBiometricCallback(finishSuccess, finishError)
                        )
                        return
                    } catch (_: Throwable) {
                        // Fall through to real Keyguard confirmation on stripped OEM ROMs
                    }
                }
                confirmDeviceCredentialViaKeyguard(
                    activity = activity,
                    keyguardManager = keyguardManager,
                    title = title,
                    subtitle = subtitle,
                    onSuccess = finishSuccess,
                    onError = finishError
                )
            }

            AuthPath.BIOMETRIC_PROMPT_Q -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        val executor = ContextCompat.getMainExecutor(activity)
                        val cancellationSignal = CancellationSignal()
                        @Suppress("DEPRECATION")
                        val prompt = BiometricPrompt.Builder(activity)
                            .setTitle(title)
                            .setSubtitle(subtitle)
                            .setDeviceCredentialAllowed(true)
                            .build()

                        prompt.authenticate(
                            cancellationSignal,
                            executor,
                            createBiometricCallback(finishSuccess, finishError)
                        )
                        return
                    } catch (_: Throwable) {
                        // Fall through to real Keyguard confirmation on buggy Android 10 OEM ROMs
                    }
                }
                confirmDeviceCredentialViaKeyguard(
                    activity = activity,
                    keyguardManager = keyguardManager,
                    title = title,
                    subtitle = subtitle,
                    onSuccess = finishSuccess,
                    onError = finishError
                )
            }

            AuthPath.KEYGUARD_CREDENTIAL_CONFIRM -> {
                // API 24-28 (Android 7.0-9.0): BiometricPrompt.setDeviceCredentialAllowed does not exist on API 28
                confirmDeviceCredentialViaKeyguard(
                    activity = activity,
                    keyguardManager = keyguardManager,
                    title = title,
                    subtitle = subtitle,
                    onSuccess = finishSuccess,
                    onError = finishError
                )
            }
        }
    }

    private fun createBiometricCallback(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ): BiometricPrompt.AuthenticationCallback {
        return object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                super.onAuthenticationError(errorCode, errString)
                onError(errString?.toString() ?: "Authentication cancelled")
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun confirmDeviceCredentialViaKeyguard(
        activity: Activity,
        keyguardManager: KeyguardManager?,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val intent = try {
            keyguardManager?.createConfirmDeviceCredentialIntent(title, subtitle)
        } catch (t: Throwable) {
            onError(t.localizedMessage ?: "Device credential confirmation unavailable")
            return
        }

        if (intent == null) {
            onError("Device credential confirmation is unavailable on this device")
            return
        }

        if (activity !is ComponentActivity) {
            onError("Device credential confirmation requires an active screen")
            return
        }

        val registryKey = "vault_device_credential_${requestCounter.incrementAndGet()}"
        var launcher: ActivityResultLauncher<Intent>? = null
        try {
            launcher = activity.activityResultRegistry.register(
                registryKey,
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                try {
                    launcher?.unregister()
                } catch (_: Throwable) {}
                if (result.resultCode == Activity.RESULT_OK) {
                    onSuccess()
                } else {
                    onError("Authentication cancelled")
                }
            }
            launcher.launch(intent)
        } catch (t: Throwable) {
            try {
                launcher?.unregister()
            } catch (_: Throwable) {}
            onError(t.localizedMessage ?: "Unable to verify device credentials")
        }
    }
}
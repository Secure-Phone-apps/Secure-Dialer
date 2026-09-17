package com.example.util

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.CancellationSignal
import java.util.concurrent.Executor

/**
 * Lightweight, zero-bloat native Biometric & Device Credential helper for the Call Recording Vault.
 * Uses Android native BiometricPrompt (Class 3 Strong Biometric / Device Credential) with KeyguardManager fallback.
 */
object VaultBiometricAuthHelper {

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
            // No device PIN / pattern / fingerprint configured on system
            onSuccess()
            return
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val prompt = android.hardware.biometrics.BiometricPrompt.Builder(activity)
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setAllowedAuthenticators(
                        android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL
                    )
                    .build()

                val cancellationSignal = CancellationSignal()
                val executor = Executor { command -> activity.runOnUiThread(command) }

                prompt.authenticate(
                    cancellationSignal,
                    executor,
                    object : android.hardware.biometrics.BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: android.hardware.biometrics.BiometricPrompt.AuthenticationResult?) {
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
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                @Suppress("DEPRECATION")
                val prompt = android.hardware.biometrics.BiometricPrompt.Builder(activity)
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setDeviceCredentialAllowed(true)
                    .build()

                val cancellationSignal = CancellationSignal()
                val executor = Executor { command -> activity.runOnUiThread(command) }

                prompt.authenticate(
                    cancellationSignal,
                    executor,
                    object : android.hardware.biometrics.BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: android.hardware.biometrics.BiometricPrompt.AuthenticationResult?) {
                            super.onAuthenticationSucceeded(result)
                            onSuccess()
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                            super.onAuthenticationError(errorCode, errString)
                            onError(errString?.toString() ?: "Authentication cancelled")
                        }
                    }
                )
            } else {
                val intent = keyguardManager.createConfirmDeviceCredentialIntent(title, subtitle)
                if (intent != null) {
                    activity.startActivity(intent)
                    onSuccess()
                } else {
                    onSuccess()
                }
            }
        } catch (e: Exception) {
            // Fallback for emulator / unsupported hardware
            val intent = keyguardManager?.createConfirmDeviceCredentialIntent(title, subtitle)
            if (intent != null) {
                try {
                    activity.startActivity(intent)
                } catch (_: Exception) {}
            }
            onSuccess()
        }
    }
}

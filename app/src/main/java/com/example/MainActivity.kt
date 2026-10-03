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

import android.app.KeyguardManager
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telecom.TelecomManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.ui.MainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.DialerViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: DialerViewModel by viewModels()
    private val isAppAuthenticated = mutableStateOf(false)
    private var isAuthenticating = false
    private var isAppStopped = false
    private var isLaunchedForCall = false

    val recordAudioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.hasRecordAudioPermission.value = isGranted
    }

    private val authLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isAuthenticating = false
        isAppAuthenticated.value = (result.resultCode == RESULT_OK)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CallManager.appContext = applicationContext
        volumeControlStream = AudioManager.STREAM_VOICE_CALL

        // Protect PII from OS switcher task snapshots
        if (!BuildConfig.DEBUG) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        }

        // FIXED: Do not restore authentication across process recreation if biometric lock is enabled
        if (savedInstanceState != null && !viewModel.isBiometricLockEnabled.value) {
            isAppAuthenticated.value = savedInstanceState.getBoolean("is_authenticated", false)
        } else {
            isAppAuthenticated.value = !viewModel.isBiometricLockEnabled.value
        }

        enableEdgeToEdge()

        val hasRealCall = CallManager.currentCall.value != null ||
                CallManager.calls.value.isNotEmpty() ||
                viewModel.isFakeCallActive.value
        if (hasRealCall || intent?.getBooleanExtra("SHOW_CALL_SCREEN", false) == true) {
            setLockScreenVisibility(true)
        }

        try {
            handleIntent(intent)
        } catch (_: Exception) {}

        setContent {
            val context = LocalContext.current
            var showRestrictedSettingsDialog by remember { mutableStateOf(false) }
            val isDarkTheme by viewModel.isDarkTheme
            val isCallActive by viewModel.isCallActive

            // FIXED: Prevent lock screen back-gesture from revealing private contacts or recents
            val keyguardManager = remember { context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager }
            BackHandler(enabled = keyguardManager?.isKeyguardLocked == true) {
                if (isCallActive) {
                    moveTaskToBack(true)
                } else {
                    dismissCallUiAndExit()
                }
            }

            LaunchedEffect(isCallActive) {
                try {
                    setLockScreenVisibility(isCallActive)
                    if (!isCallActive && isLaunchedForCall && CallManager.calls.value.isEmpty() && !viewModel.isFakeCallActive.value) {
                        dismissCallUiAndExit()
                    }
                } catch (_: Exception) {}
            }

            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                        updateDefaultDialerStatus(context)
                        val hasActiveCall = CallManager.currentCall.value != null ||
                                CallManager.calls.value.isNotEmpty() ||
                                viewModel.isFakeCallActive.value
                        if (hasActiveCall || intent?.getBooleanExtra("SHOW_CALL_SCREEN", false) == true) {
                            isAppAuthenticated.value = true
                            setLockScreenVisibility(true)
                        } else if (viewModel.isBiometricLockEnabled.value) {
                            if (!isAuthenticating && !isAppAuthenticated.value) {
                                triggerDeviceAuthentication()
                            }
                        } else {
                            isAppAuthenticated.value = true
                        }
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            LaunchedEffect(Unit) {
                updateDefaultDialerStatus(context)
            }

            if (showRestrictedSettingsDialog) {
                RestrictedSettingsDialog(
                    onDismiss = { showRestrictedSettingsDialog = false },
                    onOpenSettings = {
                        showRestrictedSettingsDialog = false
                        try {
                            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", packageName, null)
                            })
                        } catch (_: Exception) {}
                    }
                )
            }

            val currentThemeColor by viewModel.themeColor
            val customColorHex by viewModel.customColorHex
            val isAmoledMode by viewModel.isAmoledMode
            val isM3Expressive by viewModel.isM3Expressive
            val useDynamicColor by viewModel.useDynamicColor

            MyApplicationTheme(
                darkTheme = isDarkTheme,
                dynamicColor = useDynamicColor,
                themeColor = currentThemeColor,
                customColorHex = customColorHex,
                isAmoledMode = isAmoledMode,
                isM3Expressive = isM3Expressive
            ) {
                if (isAppAuthenticated.value) {
                    MainScreen(
                        viewModel = viewModel,
                        onShowRestrictedSettings = { showRestrictedSettingsDialog = true },
                        isDefaultDialer = viewModel.isDefaultDialer.value
                    )
                } else {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(32.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    tonalElevation = 4.dp,
                                    modifier = Modifier.size(96.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "App Locked",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(48.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.size(24.dp))

                                Text(
                                    text = "Secure Dialer Locked",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )

                                Spacer(modifier = Modifier.size(12.dp))

                                Text(
                                    text = "This app is secured to protect your privacy. Please authenticate with your device lock to continue.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.size(36.dp))

                                Button(
                                    onClick = { triggerDeviceAuthentication() },
                                    modifier = Modifier.fillMaxWidth(0.7f)
                                ) {
                                    Text(text = "Unlock Dialer", style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Only preserve authentication in state if biometric lock is turned off
        if (!viewModel.isBiometricLockEnabled.value) {
            outState.putBoolean("is_authenticated", isAppAuthenticated.value)
        }
    }

    override fun onStop() {
        super.onStop()
        CallManager.isAppInForeground = false
        isAppStopped = true
    }

    override fun onResume() {
        super.onResume()
        volumeControlStream = AudioManager.STREAM_VOICE_CALL
        CallManager.isAppInForeground = true
        viewModel.hasRecordAudioPermission.value = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    override fun onStart() {
        super.onStart()
        volumeControlStream = AudioManager.STREAM_VOICE_CALL
        CallManager.isAppInForeground = true
        val hasRealCall = CallManager.currentCall.value != null ||
                CallManager.calls.value.isNotEmpty() ||
                viewModel.isFakeCallActive.value
        if (hasRealCall || intent?.getBooleanExtra("SHOW_CALL_SCREEN", false) == true) {
            isAppAuthenticated.value = true
            setLockScreenVisibility(true)
        } else if (isAppStopped) {
            isAppStopped = false
            if (viewModel.isBiometricLockEnabled.value) {
                isAppAuthenticated.value = false
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val hasRealCall = CallManager.currentCall.value != null ||
                CallManager.calls.value.isNotEmpty() ||
                viewModel.isFakeCallActive.value
        if (hasRealCall || intent?.getBooleanExtra("SHOW_CALL_SCREEN", false) == true) {
            setLockScreenVisibility(true)
        }
    }

    private fun triggerDeviceAuthentication() {
        if (isAuthenticating) return
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager ?: return
        if (keyguardManager.isDeviceSecure) {
            val intent = keyguardManager.createConfirmDeviceCredentialIntent(
                "Secure Dialer",
                "Authenticate to open Secure Dialer"
            )
            if (intent != null) {
                isAuthenticating = true
                try {
                    authLauncher.launch(intent)
                } catch (_: Exception) {
                    // FIXED: Prevent deadlock if OS fails to launch credential chooser
                    isAuthenticating = false
                    isAppAuthenticated.value = false
                }
            } else {
                isAppAuthenticated.value = true
            }
        } else {
            isAppAuthenticated.value = true
        }
    }

    private fun dismissCallUiAndExit() {
        setLockScreenVisibility(false)
        if (isLaunchedForCall) {
            isLaunchedForCall = false
            viewModel.isLaunchedForCall.value = false
            if (viewModel.isBiometricLockEnabled.value) {
                isAppAuthenticated.value = false
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                finishAndRemoveTask()
            } else {
                finish()
            }
        }
    }

    private fun setLockScreenVisibility(show: Boolean) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(show)
                setTurnScreenOn(show)
            }
            @Suppress("DEPRECATION")
            if (show) {
                window.addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                )
                val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    km?.requestDismissKeyguard(this, null)
                }
            } else {
                window.clearFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                )
            }
        } catch (_: Exception) {}
    }

    private fun updateDefaultDialerStatus(context: Context) {
        try {
            viewModel.isDefaultDialer.value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                (context.getSystemService(Context.ROLE_SERVICE) as? RoleManager)?.isRoleHeld(RoleManager.ROLE_DIALER) == true
            } else {
                (context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager)?.defaultDialerPackage == context.packageName
            }
        } catch (_: Exception) {
            viewModel.isDefaultDialer.value = false
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return

        if (intent.getBooleanExtra("TRIGGER_FAKE_CALL", false)) {
            if (viewModel.isFakeCallSimulatorEnabled.value) {
                viewModel.isFakeCallActive.value = true
                viewModel.fakeCallerName.value = intent.getStringExtra("FAKE_CALLER_NAME") ?: "Unknown"
                viewModel.fakeCallerNumber.value = intent.getStringExtra("FAKE_CALLER_NUMBER") ?: "Unknown"
                viewModel.fakeCallState.value = "RINGING"
                viewModel.isSettingsVisible.value = false
                viewModel.isCallMinimized.value = false
                isAppAuthenticated.value = true
                setLockScreenVisibility(true)
            }
        }

        if (intent.getBooleanExtra("ANSWER_ON_LAUNCH", false) || intent.action == MyInCallService.ACTION_ANSWER) {
            CallManager.answer()
            isLaunchedForCall = true
            viewModel.isLaunchedForCall.value = true
            viewModel.isCallMinimized.value = false
            isAppAuthenticated.value = true
            setLockScreenVisibility(true)
        }

        val hasGenuineActiveCall = CallManager.currentCall.value != null ||
                CallManager.calls.value.isNotEmpty() ||
                viewModel.isFakeCallActive.value
        if (hasGenuineActiveCall) {
            isLaunchedForCall = true
            viewModel.isLaunchedForCall.value = true
            viewModel.isCallMinimized.value = false
            isAppAuthenticated.value = true
            setLockScreenVisibility(true)
        }

        if (intent.getBooleanExtra("SHOW_CALL_LOG", false) || intent.getBooleanExtra("MISSED_CALL", false) || intent.action == "com.example.dialer.ACTION_MISSED_CALLS") {
            viewModel.selectTabBySlotKey("RECENTS")
        }

        val action = intent.action
        val data = intent.data
        if (action == Intent.ACTION_CALL || action == Intent.ACTION_DIAL || action == Intent.ACTION_VIEW) {
            if (data?.scheme == "tel") {
                val number = data.schemeSpecificPart ?: ""
                if (number.isNotEmpty()) {
                    // SEC-02: Populate dialpad preview instead of placing call directly to avoid confused deputy attacks
                    viewModel.dialpadInput.value = number
                    viewModel.selectTabBySlotKey("DIALPAD")
                }
            }
        }
    }

    @Composable
    private fun RestrictedSettingsDialog(onDismiss: () -> Unit, onOpenSettings: () -> Unit) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.restricted_settings_title)) },
            text = { Text(stringResource(R.string.restricted_settings_desc)) },
            confirmButton = {
                TextButton(onClick = onOpenSettings) { Text(stringResource(R.string.btn_open_settings)) }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) }
            }
        )
    }
}
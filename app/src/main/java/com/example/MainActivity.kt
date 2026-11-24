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
import android.telecom.Call
import android.telecom.TelecomManager
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.data.AppDatabase
import com.example.data.DatabaseIntegrityStatus
import com.example.ui.MainScreen
import com.example.ui.theme.AppShapes
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.DialerViewModel
import com.example.util.VaultBiometricAuthHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: DialerViewModel by viewModels()
    private val isAppAuthenticated = mutableStateOf(false)
    private var isAuthenticating = false
    private var suppressAutoAuthUntilStop = false
    private var isAppStopped = false
    private var isLaunchedForCall = false

    val recordAudioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.hasRecordAudioPermission.value = isGranted
    }


    override fun attachBaseContext(newBase: Context) {
        val wrapped = com.example.util.LocaleHelper.applyLocale(newBase)
        super.attachBaseContext(wrapped)
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

        if (viewModel.isFakeCallActive.value || intent?.getBooleanExtra("TRIGGER_FAKE_CALL", false) == true) {
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

            LaunchedEffect(viewModel.isFakeCallActive.value, isCallActive) {
                try {
                    val isFake = viewModel.isFakeCallActive.value
                    setLockScreenVisibility(isFake)
                    if (!isCallActive && isLaunchedForCall && CallManager.calls.value.isEmpty() && !isFake) {
                        dismissCallUiAndExit()
                    }
                } catch (_: Exception) {}
            }

            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                        updateDefaultDialerStatus(context)
                        val isFake = viewModel.isFakeCallActive.value
                        if (isFake) {
                            isAppAuthenticated.value = true
                            setLockScreenVisibility(true)
                        } else if (viewModel.isBiometricLockEnabled.value) {
                            if (!isAuthenticating && !isAppAuthenticated.value && !suppressAutoAuthUntilStop) {
                                window.decorView.post {
                                    if (!isFinishing && !isDestroyed && viewModel.isBiometricLockEnabled.value && !isAppAuthenticated.value && !suppressAutoAuthUntilStop) {
                                        triggerDeviceAuthentication()
                                    }
                                }
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

            LaunchedEffect(isAppAuthenticated.value) {
                if (isAppAuthenticated.value) {
                    viewModel.flushPendingExternalNumber()
                }
            }

            val dbIntegrityStatus by AppDatabase.integrityStatus.collectAsStateWithLifecycle()

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
                if (showRestrictedSettingsDialog) {
                    RestrictedSettingsDialog(
                        onDismiss = { showRestrictedSettingsDialog = false },
                        onOpenSettings = {
                            showRestrictedSettingsDialog = false
                            com.example.util.CallScreenPermissionHelper.openOemPermissionSettings(this@MainActivity)
                        }
                    )
                }

                when (val status = dbIntegrityStatus) {
                    is DatabaseIntegrityStatus.TransientKeystoreError -> {
                        AlertDialog(
                            onDismissRequest = {},
                            title = { Text(stringResource(R.string.db_keystore_locked_title)) },
                            text = { Text(stringResource(R.string.db_keystore_locked_desc)) },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        lifecycleScope.launch(Dispatchers.IO) {
                                            AppDatabase.retryOpenOnTransientRecovery(applicationContext, force = true)
                                        }
                                    },
                                    modifier = Modifier.testTag("db_keystore_retry_button")
                                ) {
                                    Text(stringResource(R.string.btn_retry))
                                }
                            }
                        )
                    }
                    is DatabaseIntegrityStatus.NativeCryptoLoadError -> {
                        AlertDialog(
                            onDismissRequest = {},
                            title = { Text(stringResource(R.string.db_native_crypto_error_title)) },
                            text = { Text(stringResource(R.string.db_native_crypto_error_desc)) },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        lifecycleScope.launch(Dispatchers.IO) {
                                            AppDatabase.retryOpenOnTransientRecovery(applicationContext, force = true)
                                        }
                                    },
                                    modifier = Modifier.testTag("db_native_crypto_retry_button")
                                ) {
                                    Text(stringResource(R.string.btn_retry))
                                }
                            }
                        )
                    }
                    is DatabaseIntegrityStatus.CorruptedQuarantined -> {
                        AlertDialog(
                            onDismissRequest = { AppDatabase.dismissIntegrityWarning() },
                            title = { Text(stringResource(R.string.db_quarantined_title)) },
                            text = {
                                Text(
                                    stringResource(
                                        R.string.db_quarantined_desc,
                                        status.backupFileName
                                    )
                                )
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = { AppDatabase.dismissIntegrityWarning() },
                                    modifier = Modifier.testTag("db_quarantine_close_button")
                                ) {
                                    Text(stringResource(R.string.close))
                                }
                            }
                        )
                    }
                    DatabaseIntegrityStatus.Healthy,
                    is DatabaseIntegrityStatus.RobolectricTestFallback -> Unit
                }

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
                                    shape = AppShapes.Avatar,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    tonalElevation = 4.dp,
                                    modifier = Modifier.size(96.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = stringResource(R.string.app_locked_title),
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(48.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.size(24.dp))

                                Text(
                                    text = stringResource(R.string.app_locked_title),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )

                                Spacer(modifier = Modifier.size(12.dp))

                                Text(
                                    text = stringResource(R.string.app_locked_desc),
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.size(36.dp))

                                Button(
                                    onClick = {
                                        suppressAutoAuthUntilStop = false
                                        isAuthenticating = false
                                        triggerDeviceAuthentication()
                                    },
                                    modifier = Modifier.fillMaxWidth(0.7f)
                                ) {
                                    Text(text = stringResource(R.string.btn_unlock_dialer), style = MaterialTheme.typography.titleMedium)
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

    override fun onPause() {
        super.onPause()
        CallManager.setAppInForeground(false)
    }

    override fun onStop() {
        super.onStop()
        CallManager.setAppInForeground(false)
        isAppStopped = true
        suppressAutoAuthUntilStop = false
    }

    override fun onResume() {
        super.onResume()
        volumeControlStream = AudioManager.STREAM_VOICE_CALL
        CallManager.setAppInForeground(true)
        updateDefaultDialerStatus(this)
        viewModel.hasRecordAudioPermission.value = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CALL_LOG) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            viewModel.syncData(force = false)
        }
        if (AppDatabase.integrityStatus.value is DatabaseIntegrityStatus.TransientKeystoreError ||
            AppDatabase.integrityStatus.value is DatabaseIntegrityStatus.NativeCryptoLoadError
        ) {
            lifecycleScope.launch(Dispatchers.IO) {
                AppDatabase.retryOpenOnTransientRecoveryIfUnlocked(applicationContext, force = true)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        volumeControlStream = AudioManager.STREAM_VOICE_CALL
        CallManager.setMainActivityInForeground(true)
        if (viewModel.isFakeCallActive.value) {
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
        if (viewModel.isFakeCallActive.value) {
            setLockScreenVisibility(true)
        }
    }

    private fun triggerDeviceAuthentication() {
        if (isAuthenticating) return
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        if (keyguardManager?.isDeviceSecure != true) {
            isAppAuthenticated.value = true
            return
        }

        isAuthenticating = true
        VaultBiometricAuthHelper.authenticate(
            activity = this,
            title = getString(R.string.device_auth_title),
            subtitle = getString(R.string.device_auth_subtitle),
            onSuccess = {
                isAuthenticating = false
                suppressAutoAuthUntilStop = false
                isAppAuthenticated.value = true
            },
            onError = { _ ->
                isAuthenticating = false
                suppressAutoAuthUntilStop = true
                isAppAuthenticated.value = false
            }
        )
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
        val turnScreenOn = if (show) {
            val pm = getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
            com.example.util.LockScreenCallPolicy.resolveIncomingCallPresentation(
                context = this,
                isScreenInteractive = pm?.isInteractive ?: true,
                isKeyguardLocked = com.example.util.LockScreenCallPolicy.isDeviceLocked(this)
            ).wakeScreen
        } else {
            false
        }
        com.example.util.LockScreenCallPolicy.applyLockScreenWindowFlags(
            activity = this,
            showWhenLocked = show,
            keepScreenOn = show,
            turnScreenOn = turnScreenOn
        )
    }

    private fun updateDefaultDialerStatus(context: Context) {
        viewModel.isDefaultDialer.value = com.example.util.CallScreenPermissionHelper.isDefaultDialer(context)
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
                viewModel.fakeCallAudioUri.value = intent.getStringExtra("FAKE_CALL_AUDIO_URI")
                viewModel.fakeCallVoiceType.value = intent.getStringExtra("FAKE_CALL_VOICE_TYPE") ?: "NONE"
                viewModel.fakeCallState.value = "RINGING"
                viewModel.isSettingsVisible.value = false
                viewModel.isCallMinimized.value = false
                isAppAuthenticated.value = true
                setLockScreenVisibility(true)
            }
        }

        if (intent.getBooleanExtra("ANSWER_ON_LAUNCH", false) || intent.action == MyInCallService.ACTION_ANSWER) {
            if (com.example.util.LockScreenCallPolicy.shouldPromptUnlockBeforeAnswering(this)) {
                com.example.util.LockScreenCallPolicy.requestUnlockThen(this) { CallManager.answer() }
            } else {
                CallManager.answer()
            }
            isLaunchedForCall = true
            viewModel.isLaunchedForCall.value = true
            viewModel.isCallMinimized.value = false
        }

        val hasGenuineActiveCall = CallManager.currentCall.value != null ||
                CallManager.calls.value.isNotEmpty() ||
                viewModel.isFakeCallActive.value
        if (hasGenuineActiveCall) {
            isLaunchedForCall = true
            viewModel.isLaunchedForCall.value = true
            viewModel.isCallMinimized.value = false
            if (viewModel.isFakeCallActive.value) {
                isAppAuthenticated.value = true
                setLockScreenVisibility(true)
            }
        }

        val action = intent.action
        val data = intent.data
        val isCallLogViewIntent = action == Intent.ACTION_VIEW && (
                intent.type == android.provider.CallLog.Calls.CONTENT_TYPE ||
                        intent.type == android.provider.CallLog.Calls.CONTENT_ITEM_TYPE ||
                        data?.toString()?.startsWith(android.provider.CallLog.Calls.CONTENT_URI.toString()) == true
                )
        val isMissedOrCallLogIntent = intent.getBooleanExtra("SHOW_CALL_LOG", false) ||
                intent.getBooleanExtra("MISSED_CALL", false) ||
                action == "com.example.dialer.ACTION_MISSED_CALLS" ||
                action == TelecomManager.ACTION_SHOW_MISSED_CALLS_NOTIFICATION ||
                isCallLogViewIntent

        if (isMissedOrCallLogIntent) {
            viewModel.selectTabBySlotKey("RECENTS")
            viewModel.syncData(force = true)
            com.example.util.MissedCallNotificationReceiver.clearMissedCallNotifications(this)
        }

        if (action == Intent.ACTION_CALL || action == Intent.ACTION_DIAL || action == Intent.ACTION_VIEW) {
            if (data?.scheme == "tel") {
                val number = data.schemeSpecificPart ?: ""
                if (number.isNotEmpty()) {
                    // SEC-02: Populate dialpad preview and queue pending number via ViewModel
                    viewModel.handleExternalNumber(number)
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

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            val isVolumeKey = event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
                    event.keyCode == KeyEvent.KEYCODE_VOLUME_UP
            val isRinging = CallManager.currentCall.value?.state == Call.STATE_RINGING ||
                    CallManager.calls.value.any { it.state == Call.STATE_RINGING } ||
                    CallManager.waitingCall.value?.state == Call.STATE_RINGING ||
                    (viewModel.isFakeCallActive.value && viewModel.fakeCallState.value == "RINGING")

            if (isVolumeKey && isRinging) {
                CallManager.silenceRinger(this)
                if (viewModel.isFakeCallActive.value) {
                    viewModel.fakeCallState.value = "SILENCED"
                }
                return true
            }

            val isActiveCall = CallManager.currentCall.value?.state == Call.STATE_ACTIVE ||
                    CallManager.calls.value.any { it.state == Call.STATE_ACTIVE } ||
                    (viewModel.isFakeCallActive.value && viewModel.fakeCallState.value == "ACTIVE")

            if (isVolumeKey && isActiveCall) {
                val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                val direction = if (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                    AudioManager.ADJUST_RAISE
                } else {
                    AudioManager.ADJUST_LOWER
                }
                audioManager?.adjustStreamVolume(AudioManager.STREAM_VOICE_CALL, direction, AudioManager.FLAG_SHOW_UI)
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }
}
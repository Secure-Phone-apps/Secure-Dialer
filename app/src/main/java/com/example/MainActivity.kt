package com.example

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telecom.TelecomManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.ui.MainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.DialerViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: DialerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkDefaultDialerRole()
        handleIntent(intent)

        setContent {
            val context = LocalContext.current
            var showRestrictedSettingsDialog by remember { mutableStateOf(false) }
            val isDarkTheme by viewModel.isDarkTheme
            val isCallActive by viewModel.isCallActive

            LaunchedEffect(isCallActive) {
                setLockScreenVisibility(isCallActive)
            }

            // Observe lifecycle to refresh default dialer status
            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                        updateDefaultDialerStatus(context)
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
                        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", packageName, null)
                        })
                    }
                )
            }

            val currentThemeColor by viewModel.themeColor
            val isM3Expressive by viewModel.isM3Expressive

            MyApplicationTheme(
                darkTheme = isDarkTheme,
                dynamicColor = false,
                themeColor = currentThemeColor,
                isM3Expressive = isM3Expressive
            ) {
                MainScreen(
                    viewModel = viewModel,
                    onShowRestrictedSettings = { showRestrictedSettingsDialog = true },
                    isDefaultDialer = viewModel.isDefaultDialer.value
                )
            }
        }
    }

    private fun setLockScreenVisibility(show: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(show)
            setTurnScreenOn(show)
        } else {
            @Suppress("DEPRECATION")
            if (show) {
                window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
            } else {
                window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
            }
        }
    }

    private fun checkDefaultDialerRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(Context.ROLE_SERVICE) as RoleManager
            if (!roleManager.isRoleHeld(RoleManager.ROLE_DIALER)) {
                startActivity(roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER))
            }
        } else {
            val telecomManager = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            if (telecomManager.defaultDialerPackage != packageName) {
                startActivity(Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                    putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName)
                })
            }
        }
    }

    private fun updateDefaultDialerStatus(context: Context) {
        viewModel.isDefaultDialer.value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            (context.getSystemService(Context.ROLE_SERVICE) as RoleManager).isRoleHeld(RoleManager.ROLE_DIALER)
        } else {
            (context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager).defaultDialerPackage == context.packageName
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return

        if (intent.getBooleanExtra("SHOW_CALL_LOG", false)) {
            viewModel.selectedTab.value = 1
        }

        val action = intent.action
        val data = intent.data
        if (action == Intent.ACTION_CALL || action == Intent.ACTION_DIAL || action == Intent.ACTION_VIEW) {
            val scheme = data?.scheme
            if (scheme == "tel") {
                val number = data?.schemeSpecificPart ?: ""
                if (number.isNotEmpty()) {
                    if (action == Intent.ACTION_CALL) {
                        CallManager.placeCall(this, number)
                    } else {
                        viewModel.dialpadInput.value = number
                        viewModel.selectedTab.value = 3
                        viewModel.isDialpadVisible.value = true
                    }
                }
            }
        }
    }

    @Composable
    private fun RestrictedSettingsDialog(onDismiss: () -> Unit, onOpenSettings: () -> Unit) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Restricted Settings") },
            text = { Text("To use this app as your default dialer, you may need to manually enable it. Go to System Settings > Apps > [Our App Name] > Advanced > Allow Restricted Settings, then try again.") },
            confirmButton = {
                TextButton(onClick = onOpenSettings) { Text("Open Settings") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        )
    }
}

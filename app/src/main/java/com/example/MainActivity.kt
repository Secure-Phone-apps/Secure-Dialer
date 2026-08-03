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
import android.app.KeyguardManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {

    private val viewModel: DialerViewModel by viewModels()
    private val isAppAuthenticated = mutableStateOf(false)
    private var isAuthenticating = false
    private var isAppStopped = false

    companion object {
        private const val REQUEST_CODE_CONFIRM_DEVICE_CREDENTIAL = 4224
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("is_authenticated", isAppAuthenticated.value)
    }

    override fun onStop() {
        super.onStop()
        isAppStopped = true
    }

    override fun onStart() {
        super.onStart()
        if (isAppStopped) {
            isAppStopped = false
            if (viewModel.isBiometricLockEnabled.value) {
                isAppAuthenticated.value = false
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_CONFIRM_DEVICE_CREDENTIAL) {
            isAuthenticating = false
            if (resultCode == RESULT_OK) {
                isAppAuthenticated.value = true
            } else {
                isAppAuthenticated.value = false
            }
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
                startActivityForResult(intent, REQUEST_CODE_CONFIRM_DEVICE_CREDENTIAL)
            } else {
                isAppAuthenticated.value = true
            }
        } else {
            isAppAuthenticated.value = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            isAppAuthenticated.value = savedInstanceState.getBoolean("is_authenticated", false)
        } else {
            isAppAuthenticated.value = !viewModel.isBiometricLockEnabled.value
        }

        enableEdgeToEdge()
        try {
            checkDefaultDialerRole()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            handleIntent(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setContent {
            val context = LocalContext.current
            var showRestrictedSettingsDialog by remember { mutableStateOf(false) }
            val isDarkTheme by viewModel.isDarkTheme
            val isCallActive by viewModel.isCallActive

            LaunchedEffect(isCallActive) {
                try {
                    setLockScreenVisibility(isCallActive)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Observe lifecycle to refresh default dialer status and handle authentication
            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                        updateDefaultDialerStatus(context)
                        if (viewModel.isBiometricLockEnabled.value) {
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
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                )
            }

            val currentThemeColor by viewModel.themeColor
            val isM3Expressive by viewModel.isM3Expressive
            val useDynamicColor by viewModel.useDynamicColor

            MyApplicationTheme(
                darkTheme = isDarkTheme,
                dynamicColor = useDynamicColor,
                themeColor = currentThemeColor,
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
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
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
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
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
                                    Text(
                                        text = "Unlock Dialer",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setLockScreenVisibility(show: Boolean) {
        try {
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkDefaultDialerRole() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = getSystemService(Context.ROLE_SERVICE) as? RoleManager
                if (roleManager != null && !roleManager.isRoleHeld(RoleManager.ROLE_DIALER)) {
                    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
                    startActivity(intent)
                }
            } else {
                val telecomManager = getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                if (telecomManager != null && telecomManager.defaultDialerPackage != packageName) {
                    val intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                        putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName)
                    }
                    startActivity(intent)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateDefaultDialerStatus(context: Context) {
        try {
            viewModel.isDefaultDialer.value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                (context.getSystemService(Context.ROLE_SERVICE) as? RoleManager)?.isRoleHeld(RoleManager.ROLE_DIALER) == true
            } else {
                (context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager)?.defaultDialerPackage == context.packageName
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
                        viewModel.selectedTab.value = 2
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

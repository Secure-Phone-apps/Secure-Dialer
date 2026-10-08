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

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.telecom.CallAudioState
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.CallManager
import com.example.MainActivity
import com.example.ui.components.DynamicIslandPill
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlin.math.abs
import kotlin.math.hypot

object DynamicIslandOverlayManager {
    private var overlayComposeView: ComposeView? = null
    private var overlayLifecycleOwner: OverlayLifecycleOwner? = null
    private var monitorJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private data class MonitorState(
        val call: android.telecom.Call?,
        val callState: Int,
        val inForeground: Boolean,
        val audioRoute: Int?
    )

    fun canDrawOverlay(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    fun startCallMonitoring(context: Context) {
        monitorJob?.cancel()
        monitorJob = scope.launch {
            combine(
                CallManager.currentCall,
                CallManager.callState,
                CallManager.isAppInForeground,
                CallManager.audioState
            ) { call, state, inForeground, audio ->
                MonitorState(call, state, inForeground, audio?.route)
            }.collectLatest { state ->
                val call = state.call
                val callState = state.callState
                val inForeground = state.inForeground

                val isCallPlacedOrAnswered = call != null && (
                    callState == android.telecom.Call.STATE_ACTIVE ||
                    callState == android.telecom.Call.STATE_DIALING ||
                    callState == android.telecom.Call.STATE_CONNECTING ||
                    callState == android.telecom.Call.STATE_HOLDING
                )

                if (isCallPlacedOrAnswered) {
                    val appContext = context.applicationContext
                    val prefs = appContext.getSharedPreferences("dialer_prefs", Context.MODE_PRIVATE)
                    val isEnabled = prefs.getBoolean("is_dynamic_island_enabled", true)
                    val speakerOnly = prefs.getBoolean("is_dynamic_island_speaker_only", false)

                    // PRIVACY FIX: Do not float caller PII over a locked device keyguard
                    val km = appContext.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                    val isLocked = km?.isKeyguardLocked == true

                    if (isEnabled && canDrawOverlay(appContext) && !inForeground && !isLocked) {
                        val isSpeaker = state.audioRoute == CallAudioState.ROUTE_SPEAKER
                        if (!speakerOnly || isSpeaker) {
                            showOverlay(appContext)
                        } else {
                            hideOverlay(appContext)
                        }
                    } else {
                        hideOverlay(appContext)
                    }
                } else {
                    hideOverlay(context.applicationContext)
                }
            }
        }
    }

    fun stopCallMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
        hideOverlay()
    }

    fun showOverlay(context: Context) {
        if (overlayComposeView != null || !canDrawOverlay(context)) return

        try {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return

            val density = context.resources.displayMetrics.density
            val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
            var statusBarHeight = if (resourceId > 0) {
                context.resources.getDimensionPixelSize(resourceId)
            } else {
                (36 * density).toInt()
            }

            // On modern Android (API 30+), ensure we measure the display cutout (camera hole)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    val insets = wm.currentWindowMetrics.windowInsets.getInsets(
                        android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.displayCutout()
                    )
                    if (insets.top > statusBarHeight) {
                        statusBarHeight = insets.top
                    }
                } catch (_: Exception) {}
            }

            // Position cleanly BELOW status bar, notification bar, and camera punch hole
            val marginBelowCutout = (8 * density).toInt()
            val compactY = statusBarHeight + marginBelowCutout
            val expandedY = statusBarHeight + marginBelowCutout

            val layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = compactY
            }

            val lifecycleOwner = OverlayLifecycleOwner().apply {
                onCreate()
                onResume()
            }
            overlayLifecycleOwner = lifecycleOwner

            val composeView = ComposeView(context).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                setViewTreeLifecycleOwner(lifecycleOwner)
                setViewTreeSavedStateRegistryOwner(lifecycleOwner)
                setViewTreeViewModelStoreOwner(lifecycleOwner)

                setContent {
                    MyApplicationTheme(darkTheme = true) {
                        val callerName by CallManager.callerName.collectAsStateWithLifecycle()
                        val callerNumber by CallManager.callerNumber.collectAsStateWithLifecycle()
                        val callerPhotoUri by CallManager.callerPhotoUri.collectAsStateWithLifecycle()
                        val callerLabel by CallManager.callerLabel.collectAsStateWithLifecycle()
                        val callState by CallManager.callState.collectAsStateWithLifecycle()
                        val audioState by CallManager.audioState.collectAsStateWithLifecycle()
                        val currentSimSlot by CallManager.currentSimSlot.collectAsStateWithLifecycle()

                        DynamicIslandPill(
                            callerName = callerName,
                            callerNumber = callerNumber,
                            callState = callState,
                            audioState = audioState,
                            photoUri = callerPhotoUri,
                            callerLabel = callerLabel,
                            simSlot = currentSimSlot,
                            onExpandedChange = { expanded ->
                                val currentWm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                                if (currentWm != null && overlayComposeView != null) {
                                    if (expanded) {
                                        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
                                        layoutParams.x = 0
                                        layoutParams.y = expandedY
                                    } else {
                                        layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT
                                        layoutParams.x = 0
                                        layoutParams.y = compactY
                                    }
                                    try {
                                        currentWm.updateViewLayout(this@apply, layoutParams)
                                    } catch (_: Exception) {}
                                }
                            },
                            onExpandToFullScreen = {
                                // FIXED: Align intent extras with MainActivity contracts
                                val intent = Intent(context, MainActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                    putExtra("SHOW_CALL_SCREEN", true)
                                    putExtra("EXTRA_NAVIGATE_TO_INCALL", true)
                                }
                                context.startActivity(intent)
                                hideOverlay(context)
                            },
                            onHangUp = {
                                CallManager.disconnect()
                                hideOverlay(context)
                            }
                        )
                    }
                }
            }

            overlayComposeView = composeView
            wm.addView(composeView, layoutParams)
        } catch (_: Exception) {
            overlayComposeView = null
            overlayLifecycleOwner = null
        }
    }

    fun hideOverlay(context: Context? = null) {
        try {
            overlayComposeView?.let { view ->
                val wm = context?.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                    ?: view.context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                wm?.removeView(view)
            }
        } catch (_: Exception) {
        } finally {
            overlayLifecycleOwner?.onDestroy()
            overlayLifecycleOwner = null
            overlayComposeView = null
        }
    }
}

class OverlayLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = store

    fun onCreate() {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    fun onResume() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }
}
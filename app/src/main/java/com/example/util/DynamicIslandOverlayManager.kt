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

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telecom.Call
import android.telecom.CallAudioState
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.CallManager
import com.example.MainActivity
import com.example.ui.components.DynamicIslandPill
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

object DynamicIslandOverlayManager {
    private var windowManager: WindowManager? = null
    private var overlayComposeView: ComposeView? = null
    private var overlayLifecycleOwner: OverlayLifecycleOwner? = null
    private var monitorJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

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
            CallManager.currentCall.collectLatest { call ->
                if (call != null) {
                    val prefs = context.getSharedPreferences("dialer_prefs", Context.MODE_PRIVATE)
                    val isEnabled = prefs.getBoolean("is_dynamic_island_enabled", true)
                    val speakerOnly = prefs.getBoolean("is_dynamic_island_speaker_only", false)
                    
                    if (isEnabled && canDrawOverlay(context) && !CallManager.isAppInForeground) {
                        val isSpeaker = CallManager.audioState.value?.route == CallAudioState.ROUTE_SPEAKER
                        if (!speakerOnly || isSpeaker) {
                            showOverlay(context)
                        } else {
                            hideOverlay()
                        }
                    } else {
                        hideOverlay()
                    }
                } else {
                    hideOverlay()
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
        if (overlayComposeView != null) return
        if (!canDrawOverlay(context)) return

        try {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
            windowManager = wm

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
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = 12
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

                var initialX = 0
                var initialY = 0
                var initialTouchX = 0f
                var initialTouchY = 0f
                var isDraggingWindow = false

                setOnTouchListener { _, event ->
                    when (event.action) {
                        android.view.MotionEvent.ACTION_DOWN -> {
                            initialX = layoutParams.x
                            initialY = layoutParams.y
                            initialTouchX = event.rawX
                            initialTouchY = event.rawY
                            isDraggingWindow = false
                            false
                        }
                        android.view.MotionEvent.ACTION_MOVE -> {
                            val dx = event.rawX - initialTouchX
                            val dy = event.rawY - initialTouchY
                            if (kotlin.math.hypot(dx.toDouble(), dy.toDouble()) > 20) {
                                isDraggingWindow = true
                                layoutParams.x = initialX + dx.toInt()
                                layoutParams.y = (initialY + dy.toInt()).coerceAtLeast(10)
                                try {
                                    wm.updateViewLayout(this, layoutParams)
                                } catch (_: Exception) {}
                            }
                            false
                        }
                        android.view.MotionEvent.ACTION_UP -> {
                            if (isDraggingWindow) {
                                val metrics = context.resources.displayMetrics
                                val halfScreen = metrics.widthPixels / 2
                                val targetX = when {
                                    layoutParams.x < -halfScreen / 3 -> -halfScreen + 160
                                    layoutParams.x > halfScreen / 3 -> halfScreen - 160
                                    else -> 0
                                }
                                layoutParams.x = targetX
                                try {
                                    wm.updateViewLayout(this, layoutParams)
                                } catch (_: Exception) {}
                            }
                            false
                        }
                        else -> false
                    }
                }

                setContent {
                    MyApplicationTheme(darkTheme = true) {
                        val callerName by CallManager.callerName.collectAsStateWithLifecycle()
                        val callerNumber by CallManager.callerNumber.collectAsStateWithLifecycle()
                        val callState by CallManager.callState.collectAsStateWithLifecycle()
                        val audioState by CallManager.audioState.collectAsStateWithLifecycle()

                        DynamicIslandPill(
                            callerName = callerName,
                            callerNumber = callerNumber,
                            callState = callState,
                            audioState = audioState,
                            onExpandToFullScreen = {
                                val intent = Intent(context, MainActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                    putExtra("EXTRA_NAVIGATE_TO_INCALL", true)
                                }
                                context.startActivity(intent)
                                hideOverlay()
                            },
                            onHangUp = {
                                CallManager.disconnect()
                                hideOverlay()
                            }
                        )
                    }
                }
            }

            overlayComposeView = composeView
            wm.addView(composeView, layoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
            overlayComposeView = null
            overlayLifecycleOwner = null
        }
    }

    fun hideOverlay() {
        try {
            overlayComposeView?.let { view ->
                windowManager?.removeViewImmediate(view)
            }
        } catch (e: Exception) {
            e.printStackTrace()
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

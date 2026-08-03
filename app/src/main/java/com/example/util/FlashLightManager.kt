package com.example.util

import android.content.Context
import android.hardware.camera2.CameraManager
import kotlinx.coroutines.*

object FlashLightManager {
    private var flashJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    fun startFlashing(context: Context) {
        val prefs = context.getSharedPreferences("dialer_prefs", Context.MODE_PRIVATE)
        val flashEnabled = prefs.getBoolean("flash_alerts_enabled", false)
        if (!flashEnabled) return

        if (flashJob != null) return // Already flashing

        flashJob = scope.launch {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return@launch
            var currentCameraId: String? = null
            try {
                currentCameraId = cameraManager.cameraIdList.firstOrNull { id ->
                    val characteristics = cameraManager.getCameraCharacteristics(id)
                    characteristics.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                } ?: return@launch

                var state = false
                while (isActive) {
                    state = !state
                    cameraManager.setTorchMode(currentCameraId, state)
                    delay(350) // Flash frequency: ~350ms interval for optimal alerts
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                // Guaranteed safety cleanup: ensure flashlight is completely turned off
                if (currentCameraId != null) {
                    try {
                        cameraManager.setTorchMode(currentCameraId, false)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    fun stopFlashing(context: Context) {
        flashJob?.cancel()
        flashJob = null
    }
}

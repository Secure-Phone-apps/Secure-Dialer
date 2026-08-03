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

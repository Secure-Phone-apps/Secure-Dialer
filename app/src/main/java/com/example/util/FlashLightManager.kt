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
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import kotlinx.coroutines.*

object FlashLightManager {
    private var flashJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun startFlashing(context: Context) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences("dialer_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("flash_alerts_enabled", false)) return

        // FIXED: Check isActive instead of != null to prevent permanent lockout
        if (flashJob?.isActive == true) return

        flashJob = scope.launch {
            val cameraManager = appContext.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return@launch
            var targetCameraId: String? = null

            try {
                // FIXED: Explicitly target REAR camera flash to avoid blinding user with front-facing selfie flash
                targetCameraId = cameraManager.cameraIdList.firstOrNull { id ->
                    val chars = cameraManager.getCameraCharacteristics(id)
                    val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                    val isBack = chars.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
                    hasFlash && isBack
                } ?: cameraManager.cameraIdList.firstOrNull { id ->
                    cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                } ?: return@launch

                var state = false
                val startTime = System.currentTimeMillis()

                // Safety timeout: max 45s strobe prevents hardware overheating if call disconnect event is missed
                while (isActive && (System.currentTimeMillis() - startTime < 45000L)) {
                    state = !state
                    try {
                        cameraManager.setTorchMode(targetCameraId, state)
                    } catch (_: Exception) {}
                    delay(350)
                }
            } catch (_: Exception) {
            } finally {
                // FIXED: Guaranteed non-cancellable hardware cleanup ensures LED is completely turned off
                withContext(NonCancellable) {
                    targetCameraId?.let { id ->
                        try {
                            cameraManager.setTorchMode(id, false)
                        } catch (_: Exception) {}
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
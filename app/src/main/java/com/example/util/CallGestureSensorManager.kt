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
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * World-class Hardware Sensor Manager for Call Gestures:
 * - Proximity detection (Ear close screen dimming)
 * - Flip-Face-Down to Mute incoming ringer
 * - Shake gesture detection
 */
class CallGestureSensorManager(
    private val context: Context,
    private val onProximityChanged: (Boolean) -> Unit,
    private val onFlipFaceDown: () -> Unit = {},
    private val onShake: () -> Unit = {}
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val proximitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var isListening = false
    private var lastShakeTime = 0L
    private val prefs = context.getSharedPreferences("dialer_prefs", Context.MODE_PRIVATE)
    private var wasFaceUp = false
    private var faceDownStartTime = 0L
    private var hasFlippedThisCall = false

    fun startListening() {
        if (isListening || sensorManager == null) return
        wasFaceUp = false
        faceDownStartTime = 0L
        hasFlippedThisCall = false

        proximitySensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        isListening = true
    }

    fun stopListening() {
        if (!isListening || sensorManager == null) return
        sensorManager.unregisterListener(this)
        isListening = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_PROXIMITY -> {
                val distance = event.values.getOrNull(0) ?: return
                val maxRange = proximitySensor?.maximumRange ?: 5f
                val isNear = distance < maxRange && distance < 5f
                onProximityChanged(isNear)
            }
            Sensor.TYPE_ACCELEROMETER -> {
                val x = event.values.getOrNull(0) ?: 0f
                val y = event.values.getOrNull(1) ?: 0f
                val z = event.values.getOrNull(2) ?: 0f

                // 1. Check Flip Face Down (requires phone was face-up first, then turned face-down and held for 500ms)
                val isFlipEnabled = prefs.getBoolean("flip_to_silence_enabled", false)
                if (isFlipEnabled && !hasFlippedThisCall) {
                    if (z > 3.0f) {
                        // Phone is upright or face-up
                        wasFaceUp = true
                        faceDownStartTime = 0L
                    } else if (wasFaceUp && z < -7.5f && kotlin.math.abs(x) < 4.0f && kotlin.math.abs(y) < 4.0f) {
                        val now = System.currentTimeMillis()
                        if (faceDownStartTime == 0L) {
                            faceDownStartTime = now
                        } else if (now - faceDownStartTime >= 500L) {
                            hasFlippedThisCall = true
                            faceDownStartTime = 0L
                            onFlipFaceDown()
                        }
                    } else {
                        faceDownStartTime = 0L
                    }
                }

                // 2. Check Shake
                val gForce = sqrt((x * x + y * y + z * z).toDouble()).toFloat() / SensorManager.GRAVITY_EARTH
                if (gForce > 2.7f) {
                    val now = System.currentTimeMillis()
                    if (now - lastShakeTime > 1500) {
                        lastShakeTime = now
                        onShake()
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

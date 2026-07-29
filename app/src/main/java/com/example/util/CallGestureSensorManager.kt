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

    fun startListening() {
        if (isListening || sensorManager == null) return

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

                // 1. Check Flip Face Down (Z axis strongly negative, X & Y near zero)
                if (z < -8.5f && kotlin.math.abs(x) < 3.0f && kotlin.math.abs(y) < 3.0f) {
                    onFlipFaceDown()
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

package com.byr0n.glyphstopwatch

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * Streams accelerometer samples, plus whether the proximity sensor reads near, on the main
 * thread. Without a proximity sensor, near is always true.
 */
class MotionSensors(
    context: Context,
    private val onSample: (x: Float, y: Float, z: Float, proximityNear: Boolean) -> Unit,
) : SensorEventListener {
    private val sensors = context.getSystemService(SensorManager::class.java)
    private val accelerometer = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val proximity = sensors.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    private var proximityNear = proximity == null

    var isRunning = false
        private set

    fun start() {
        if (isRunning) return
        isRunning = true
        sensors.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        proximity?.let { sensors.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
    }

    fun stop() {
        if (!isRunning) return
        isRunning = false
        sensors.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_PROXIMITY -> proximityNear = event.values[0] < event.sensor.maximumRange
            Sensor.TYPE_ACCELEROMETER -> onSample(event.values[0], event.values[1], event.values[2], proximityNear)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
}

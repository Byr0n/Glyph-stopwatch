package com.byr0n.glyphstopwatch

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock

/**
 * Watches the accelerometer, with the proximity sensor as an extra check, and reports
 * debounced face down and face up changes on the main thread.
 */
class FaceDownDetector(context: Context, private val onFlip: (Flip) -> Unit) : SensorEventListener {
    private val sensors = context.getSystemService(SensorManager::class.java)
    private val accelerometer = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val proximity = sensors.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    private val filter = FlipFilter()

    // Without a proximity sensor, rely on the accelerometer alone.
    private var proximityNear = proximity == null

    fun start() {
        filter.reset()
        sensors.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        proximity?.let { sensors.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
    }

    fun stop() {
        sensors.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_PROXIMITY -> proximityNear = event.values[0] < event.sensor.maximumRange
            Sensor.TYPE_ACCELEROMETER ->
                filter.onSample(event.values[2], proximityNear, SystemClock.elapsedRealtime())?.let(onFlip)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
}

package com.byr0n.glyphstopwatch

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock

/** Reports shakes of the face down phone on the main thread, via [ShakeFilter]. */
class ShakeDetector(context: Context, private val onShake: () -> Unit) : SensorEventListener {
    private val sensors = context.getSystemService(SensorManager::class.java)
    private val accelerometer = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val filter = ShakeFilter()
    private var listening = false

    fun start() {
        if (listening) return
        listening = true
        filter.reset(SystemClock.elapsedRealtime())
        sensors.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
    }

    fun stop() {
        if (!listening) return
        listening = false
        sensors.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val (x, y, z) = event.values
        if (filter.onSample(x, y, z, SystemClock.elapsedRealtime())) onShake()
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
}

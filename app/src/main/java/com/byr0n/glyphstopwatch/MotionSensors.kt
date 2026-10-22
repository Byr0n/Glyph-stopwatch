package com.byr0n.glyphstopwatch

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * The phone's motion and proximity sensors, in two modes that trade battery for detail.
 *
 * [Mode.WAIT_FOR_COVER] listens to the proximity sensor alone. When it is a wake-up sensor
 * (most are), the processor can sleep until something covers the phone, such as the table
 * when it is placed face down. Phones without one fall back to a slow wake-up accelerometer,
 * or failing that a slow ordinary one, which [needsWakeLockToWait] reports.
 *
 * [Mode.WATCH] adds the accelerometer at 25 Hz, enough to tell face down from face up and
 * to catch shakes. Events arrive on the main thread.
 */
class MotionSensors(
    context: Context,
    private val onAcceleration: (x: Float, y: Float, z: Float) -> Unit,
    private val onProximity: (near: Boolean) -> Unit,
) : SensorEventListener {
    enum class Mode { OFF, WAIT_FOR_COVER, WATCH }

    private val sensors = context.getSystemService(SensorManager::class.java)
    private val accelerometer = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val proximity: Sensor? = sensors.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    private val wakingProximity = proximity?.takeIf { it.isWakeUpSensor }
        ?: sensors.getDefaultSensor(Sensor.TYPE_PROXIMITY, true)
    private val wakingAccelerometer: Sensor? = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER, true)

    /** False when the phone has no proximity sensor, so "covered" can never be known. */
    val hasProximity = proximity != null

    /** True when waiting for cover still needs the processor kept awake. */
    val needsWakeLockToWait = wakingProximity == null && wakingAccelerometer == null

    var mode = Mode.OFF
        private set

    fun setMode(newMode: Mode) {
        if (newMode == mode) return
        sensors.unregisterListener(this)
        mode = newMode
        when (newMode) {
            Mode.OFF -> Unit
            Mode.WAIT_FOR_COVER -> when {
                wakingProximity != null -> sensors.registerListener(this, wakingProximity, SensorManager.SENSOR_DELAY_NORMAL)
                wakingAccelerometer != null ->
                    sensors.registerListener(this, wakingAccelerometer, SensorManager.SENSOR_DELAY_NORMAL)
                else -> sensors.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
            }
            Mode.WATCH -> {
                sensors.registerListener(this, accelerometer, WATCH_PERIOD_US)
                proximity?.let { sensors.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_PROXIMITY -> onProximity(event.values[0] < event.sensor.maximumRange)
            Sensor.TYPE_ACCELEROMETER -> onAcceleration(event.values[0], event.values[1], event.values[2])
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit

    private companion object {
        // 25 samples a second.
        const val WATCH_PERIOD_US = 40_000
    }
}

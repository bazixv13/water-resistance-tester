package com.raywdude.waterresistance

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Streams raw barometric pressure readings (hPa) from the device barometer.
 *
 * The water resistance test relies on the fact that a phone with intact seals traps a small
 * cushion of air: squeezing the chassis makes the internal barometer read higher. Phones with
 * no barometer cannot run the test at all, which is reported through [isAvailable].
 */
class PressureSensor(context: Context) {

    private val sensorManager: SensorManager? =
        context.applicationContext.getSystemService(SensorManager::class.java)

    private val barometer: Sensor? =
        sensorManager?.getDefaultSensor(Sensor.TYPE_PRESSURE)

    val isAvailable: Boolean = barometer != null

    fun readings(): Flow<Float> = callbackFlow {
        val manager = sensorManager
        val sensor = barometer
        if (manager == null || sensor == null) {
            close()
            return@callbackFlow
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val pressure = event.values.firstOrNull() ?: return
                trySend(pressure)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        // Ask for the fastest the hardware will give: on most flagships that is 50-100 Hz, which
        // is what makes the live trace look continuous instead of stepped.
        manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_FASTEST)
        awaitClose { manager.unregisterListener(listener) }
    }
}

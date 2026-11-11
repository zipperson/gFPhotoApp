package com.frame.app.platform.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.frame.app.domain.model.TiltState
import com.frame.app.domain.service.LevelSensor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.sample
import kotlin.math.abs

/**
 * [LevelSensor] backed by the device's gravity sensor (falling back to the raw
 * accelerometer when no gravity sensor exists). Converts samples to [TiltState]
 * via [TiltMath] and throttles emissions to a UI-friendly cadence.
 *
 * Throttling strategy: register at UI sensor rate, then [sample] the flow at a
 * fixed interval so the overlay redraws ~20x/sec at most regardless of the
 * sensor's native rate — matching the project's "throttled StateFlow" decision
 * and keeping the Compose Canvas light.
 */
class DeviceLevelSensor(
    context: Context,
    private val samplePeriodMs: Long = DEFAULT_SAMPLE_PERIOD_MS,
) : LevelSensor {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    // Prefer the (gravity-isolated) GRAVITY sensor; fall back to ACCELEROMETER.
    private val sensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    override val tilt: Flow<TiltState> = callbackFlow {
        val activeSensor = sensor
        if (activeSensor == null) {
            trySend(TiltState.UNAVAILABLE)
            close()
            return@callbackFlow
        }

        // Low-pass filter when using the raw accelerometer to isolate gravity;
        // the GRAVITY sensor is already filtered.
        val usingAccelerometer = activeSensor.type == Sensor.TYPE_ACCELEROMETER
        val filtered = FloatArray(3)
        var hasSample = false

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val (x, y, z) = if (usingAccelerometer) {
                    if (!hasSample) {
                        filtered[0] = event.values[0]
                        filtered[1] = event.values[1]
                        filtered[2] = event.values[2]
                        hasSample = true
                    } else {
                        filtered[0] = ALPHA * filtered[0] + (1 - ALPHA) * event.values[0]
                        filtered[1] = ALPHA * filtered[1] + (1 - ALPHA) * event.values[1]
                        filtered[2] = ALPHA * filtered[2] + (1 - ALPHA) * event.values[2]
                    }
                    Triple(filtered[0], filtered[1], filtered[2])
                } else {
                    Triple(event.values[0], event.values[1], event.values[2])
                }
                trySend(TiltMath.toTiltState(x, y, z))
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        sensorManager.registerListener(
            listener,
            activeSensor,
            SensorManager.SENSOR_DELAY_UI,
        )

        awaitClose { sensorManager.unregisterListener(listener) }
    }
        .sample(samplePeriodMs)
        // Avoid redundant redraws when the device is still.
        .distinctUntilChanged { a, b ->
            a.isAvailable == b.isAvailable &&
                abs(a.rollDegrees - b.rollDegrees) < MIN_ANGLE_DELTA &&
                abs(a.pitchDegrees - b.pitchDegrees) < MIN_ANGLE_DELTA
        }
        .conflate()

    private companion object {
        const val DEFAULT_SAMPLE_PERIOD_MS = 50L // ~20 Hz
        const val ALPHA = 0.8f                   // accelerometer low-pass factor
        const val MIN_ANGLE_DELTA = 0.1f         // degrees; ignore sub-threshold jitter
    }
}

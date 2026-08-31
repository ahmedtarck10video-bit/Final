package com.example.engine3d

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.PI

class SensorsTracker(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ORIENTATION)

    private val _headOrientation = MutableStateFlow(HeadOrientation(0f, 0f, 0f))
    val headOrientation: StateFlow<HeadOrientation> = _headOrientation.asStateFlow()

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    fun start() {
        rotationSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)

            val yaw = (orientationAngles[0] * 180f / PI.toFloat()) // Azimuth (Z)
            val pitch = (orientationAngles[1] * 180f / PI.toFloat()) // Pitch (X)
            val roll = (orientationAngles[2] * 180f / PI.toFloat()) // Roll (Y)

            _headOrientation.value = HeadOrientation(pitch = pitch, yaw = yaw, roll = roll)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

data class HeadOrientation(
    val pitch: Float = 0f,
    val yaw: Float = 0f,
    val roll: Float = 0f
)

package com.example.bouranizdi

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs
import kotlin.math.sign

/**
 * Čte akcelerometr (osa x) a převádí náklon telefonu na hodnotu -1..1.
 * Záporná hodnota = náklon doleva, kladná = doprava, 0 = telefon rovně (nebo v mrtvé zóně).
 * Registraci řídí aktivita: start() v onResume, stop() v onPause.
 */
class TiltController(context: Context) : SensorEventListener {

    companion object {
        /** Síla low-pass filtru (0..1): menší = hladší, ale pomalejší reakce. */
        const val FILTER_ALPHA = 0.2f

        /** Mrtvá zóna v m/s²: menší náklon se ignoruje, aby pálka v klidu necukala. */
        const val DEAD_ZONE = 0.6f

        /** Zrychlení v m/s², při kterém je náklon „naplno“ (≈ 30° náklonu). */
        const val MAX_TILT_ACCEL = 5f
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    /** Vyhlazená hodnota osy x akcelerometru v m/s². */
    private var filteredX = 0f

    /** Aktuální náklon -1..1; čte ho vlákno herní smyčky. */
    @Volatile
    var tilt = 0f
        private set

    /** Začne poslouchat akcelerometr. Volat z Activity.onResume. */
    fun start() {
        val sensor = accelerometer ?: return
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
    }

    /** Přestane poslouchat akcelerometr a vynuluje náklon. Volat z Activity.onPause. */
    fun stop() {
        sensorManager.unregisterListener(this)
        filteredX = 0f
        tilt = 0f
    }

    override fun onSensorChanged(event: SensorEvent) {
        filteredX = lowPass(filteredX, event.values[0], FILTER_ALPHA)
        tilt = accelToTilt(filteredX, DEAD_ZONE, MAX_TILT_ACCEL)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Přesnost senzoru nepotřebujeme.
    }
}

/** Low-pass filtr: posune předchozí hodnotu o část (alpha) směrem k nové hodnotě. */
fun lowPass(previous: Float, raw: Float, alpha: Float): Float =
    previous + alpha * (raw - previous)

/**
 * Převede zrychlení osy x na náklon -1..1.
 * Akcelerometr dává kladné x, když je telefon nakloněný doleva, proto se znaménko otáčí.
 * Uvnitř mrtvé zóny vrací 0; za ní hodnota plynule roste, takže pálka neposkočí.
 */
fun accelToTilt(accelX: Float, deadZone: Float, maxAccel: Float): Float {
    val magnitude = abs(accelX)
    if (magnitude <= deadZone) return 0f
    val scaled = (magnitude - deadZone) / (maxAccel - deadZone)
    return -sign(accelX) * scaled.coerceAtMost(1f)
}

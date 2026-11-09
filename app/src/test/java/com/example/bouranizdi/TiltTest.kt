package com.example.bouranizdi

import org.junit.Assert.assertEquals
import org.junit.Test

/** Testy převodu akcelerometru na náklon (filtr, mrtvá zóna, směr). */
class TiltTest {

    private val dead = 0.6f
    private val max = 5f

    @Test
    fun lowPass_movesPartwayTowardRaw() {
        assertEquals(2f, lowPass(0f, 10f, 0.2f), 1e-6f)
    }

    @Test
    fun lowPass_constantInput_converges() {
        var value = 0f
        repeat(100) { value = lowPass(value, 3f, 0.2f) }
        assertEquals(3f, value, 1e-3f)
    }

    @Test
    fun insideDeadZone_returnsZero() {
        assertEquals(0f, accelToTilt(0.5f, dead, max), 0f)
        assertEquals(0f, accelToTilt(-0.5f, dead, max), 0f)
        assertEquals(0f, accelToTilt(dead, dead, max), 0f)
    }

    @Test
    fun justOutsideDeadZone_isSmallNotJump() {
        val tilt = accelToTilt(dead + 0.01f, dead, max)
        assert(kotlin.math.abs(tilt) < 0.01f)
    }

    @Test
    fun phoneTiltedLeft_positiveX_givesNegativeTilt() {
        assertEquals(-1f, accelToTilt(max, dead, max), 1e-6f)
    }

    @Test
    fun phoneTiltedRight_negativeX_givesPositiveTilt() {
        assertEquals(1f, accelToTilt(-max, dead, max), 1e-6f)
    }

    @Test
    fun beyondMax_isClampedToOne() {
        assertEquals(1f, accelToTilt(-9.81f, dead, max), 1e-6f)
        assertEquals(-1f, accelToTilt(9.81f, dead, max), 1e-6f)
    }
}

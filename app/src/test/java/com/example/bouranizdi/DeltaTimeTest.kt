package com.example.bouranizdi

import org.junit.Assert.assertEquals
import org.junit.Test

/** Testy výpočtu delta time pro herní smyčku. */
class DeltaTimeTest {

    private val max = 0.05f

    @Test
    fun normalFrame_returnsSecondsBetweenTimes() {
        // 16 ms ≈ 60 FPS
        assertEquals(0.016f, computeDeltaSeconds(0L, 16_000_000L, max), 1e-6f)
    }

    @Test
    fun longPause_isClampedToMax() {
        // 2 sekundy (např. po návratu z pozadí) se omezí na max
        assertEquals(max, computeDeltaSeconds(0L, 2_000_000_000L, max), 1e-6f)
    }

    @Test
    fun sameTime_returnsZero() {
        assertEquals(0f, computeDeltaSeconds(5L, 5L, max), 0f)
    }

    @Test
    fun timeGoingBackwards_returnsZero() {
        assertEquals(0f, computeDeltaSeconds(10_000_000L, 0L, max), 0f)
    }
}

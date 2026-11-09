package com.example.bouranizdi

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

/**
 * Pálka ve spodní části obrazovky. Rozměry i rychlost jsou relativní k velikosti obrazovky.
 * Pozice [x] je levý okraj pálky v pixelech.
 */
class Paddle(private val screenWidth: Float, screenHeight: Float) {

    companion object {
        /** Šířka pálky jako podíl šířky obrazovky. */
        const val WIDTH_RATIO = 0.22f

        /** Výška pálky jako podíl výšky obrazovky. */
        const val HEIGHT_RATIO = 0.018f

        /** Vzdálenost spodní hrany pálky od spodního okraje jako podíl výšky obrazovky. */
        const val BOTTOM_MARGIN_RATIO = 0.08f

        /** Rychlost při plném náklonu v šířkách obrazovky za sekundu. */
        const val MAX_SPEED_RATIO = 1.5f

        /** Barva pálky. */
        const val COLOR = Color.WHITE
    }

    val width = screenWidth * WIDTH_RATIO
    val height = screenHeight * HEIGHT_RATIO
    val y = screenHeight * (1f - BOTTOM_MARGIN_RATIO) - height

    /** Levý okraj pálky; na začátku je pálka uprostřed. */
    var x = (screenWidth - width) / 2f
        private set

    /** Paint se vytvoří až při prvním kreslení (unit testy kreslení nevolají). */
    private val paint by lazy { Paint().apply { color = COLOR; isAntiAlias = true } }

    /** Posune pálku podle náklonu (-1..1) o dt sekund a udrží ji na obrazovce. */
    fun move(tilt: Float, dt: Float) {
        val maxSpeed = screenWidth * MAX_SPEED_RATIO
        x = movePaddleX(x, tilt, maxSpeed, dt, width, screenWidth)
    }

    /** Vykreslí pálku jako zaoblený obdélník. */
    fun draw(canvas: Canvas) {
        val radius = height / 2f
        canvas.drawRoundRect(x, y, x + width, y + height, radius, radius, paint)
    }
}

/**
 * Spočítá novou x-pozici pálky: posun = náklon × max. rychlost × dt,
 * výsledek omezený tak, aby pálka nevyjela z obrazovky.
 */
fun movePaddleX(x: Float, tilt: Float, maxSpeed: Float, dt: Float, paddleWidth: Float, screenWidth: Float): Float {
    val newX = x + tilt * maxSpeed * dt
    return newX.coerceIn(0f, screenWidth - paddleWidth)
}

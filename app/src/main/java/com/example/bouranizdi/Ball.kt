package com.example.bouranizdi

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin

/**
 * Míček. Poloměr a rychlost jsou relativní k velikosti obrazovky.
 * [x], [y] je střed míčku v pixelech, [vx], [vy] rychlost v pixelech za sekundu.
 */
class Ball(screenWidth: Float, screenHeight: Float) {

    companion object {
        /** Poloměr míčku jako podíl šířky obrazovky. */
        const val RADIUS_RATIO = 0.018f

        /** Rychlost míčku ve výškách obrazovky za sekundu. */
        const val SPEED_RATIO = 0.55f

        /** Úhel odpalu od svislice ve stupních (kladný = doprava). */
        const val LAUNCH_ANGLE_DEG = 20f

        /** Barva míčku. */
        const val COLOR = Color.WHITE
    }

    val radius = screenWidth * RADIUS_RATIO
    val speed = screenHeight * SPEED_RATIO

    var x = 0f
    var y = 0f
    var vx = 0f
    var vy = 0f

    /** Paint se vytvoří až při prvním kreslení (unit testy kreslení nevolají). */
    private val paint by lazy { Paint().apply { color = COLOR; isAntiAlias = true } }

    /** Posune míček o dt sekund podle jeho rychlosti. */
    fun move(dt: Float) {
        x += vx * dt
        y += vy * dt
    }

    /** Odpálí míček šikmo nahoru pod úhlem [LAUNCH_ANGLE_DEG]. */
    fun launch() {
        val angle = Math.toRadians(LAUNCH_ANGLE_DEG.toDouble()).toFloat()
        vx = speed * sin(angle)
        vy = -speed * cos(angle)
    }

    /** Zastaví míček. */
    fun stop() {
        vx = 0f
        vy = 0f
    }

    /** Položí míček doprostřed na horní hranu pálky. */
    fun placeOnPaddle(paddle: Paddle) {
        x = paddle.x + paddle.width / 2f
        y = paddle.y - radius
    }

    /**
     * Odrazí míček od levé, pravé a horní stěny. Míček se vrátí dovnitř plochy
     * a rychlost se nastaví směrem od stěny (ne jen otočí), takže se nemůže „zaseknout“ ve stěně.
     * Každá osa se kontroluje zvlášť, takže v rohu se odrazí v obou osách.
     */
    fun checkWallCollision(screenWidth: Float) {
        if (x - radius < 0f) {
            x = radius
            vx = abs(vx)
        } else if (x + radius > screenWidth) {
            x = screenWidth - radius
            vx = -abs(vx)
        }
        if (y - radius < 0f) {
            y = radius
            vy = abs(vy)
        }
    }

    /** True, když míček celý propadl pod spodní okraj obrazovky. */
    fun isBelowBottom(screenHeight: Float): Boolean = y - radius > screenHeight

    /** Vykreslí míček jako kruh. */
    fun draw(canvas: Canvas) {
        canvas.drawCircle(x, y, radius, paint)
    }
}

/**
 * Na kolik menších kroků rozdělit pohyb o vzdálenost [distance], aby žádný krok nebyl delší než [maxStep].
 * Brání tomu, aby rychlý míček při jednom dlouhém snímku „proskočil“ skrz pálku nebo blok.
 */
fun substepCount(distance: Float, maxStep: Float): Int {
    if (maxStep <= 0f) return 1
    return ceil(distance / maxStep).toInt().coerceAtLeast(1)
}

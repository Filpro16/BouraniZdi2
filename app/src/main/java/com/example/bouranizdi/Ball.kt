package com.example.bouranizdi

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

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

        /** Největší úhel odrazu od svislice ve stupních (při dopadu na úplný kraj pálky). */
        const val MAX_BOUNCE_ANGLE_DEG = 60f

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

    /**
     * Odrazí míček od pálky, pokud do ní narazil shora (míček letí dolů a jeho střed je nad spodní hranou pálky).
     * Úhel odrazu závisí na místě dopadu: střed pálky = kolmo nahoru, kraj = až [MAX_BOUNCE_ANGLE_DEG].
     * Rychlost míčku se nemění. Vrací true, když k odrazu došlo.
     */
    fun checkPaddleCollision(paddle: Paddle): Boolean {
        if (vy <= 0f || y > paddle.y + paddle.height) return false
        val hit = circleIntersectsRect(x, y, radius, paddle.x, paddle.y, paddle.x + paddle.width, paddle.y + paddle.height)
        if (!hit) return false
        val offset = paddleHitOffset(x, paddle.x, paddle.width)
        val maxAngle = Math.toRadians(MAX_BOUNCE_ANGLE_DEG.toDouble()).toFloat()
        val angle = offset * maxAngle
        vx = speed * sin(angle)
        vy = -speed * cos(angle)
        y = paddle.y - radius
        return true
    }

    /**
     * Odrazí míček od živého bloku, pokud se ho dotýká. Vrací true, když k zásahu došlo
     * (blok samotný nezničí – to řeší volající).
     * Strana odrazu se pozná podle toho, ve které ose míček do bloku zajel méně:
     * menší překryv v ose x = náraz z boku, v ose y = náraz shora / zespodu, stejný = roh (odraz v obou osách).
     */
    fun checkBlockCollision(block: Block): Boolean {
        if (!block.isAlive) return false
        if (!circleIntersectsRect(x, y, radius, block.left, block.top, block.right, block.bottom)) return false
        val overlapX = min(x + radius - block.left, block.right - (x - radius))
        val overlapY = min(y + radius - block.top, block.bottom - (y - radius))
        if (overlapX <= overlapY) vx = if (x < block.centerX) -abs(vx) else abs(vx)
        if (overlapY <= overlapX) vy = if (y < block.centerY) -abs(vy) else abs(vy)
        return true
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

/**
 * Kam míček dopadl na pálku: -1 = levý kraj, 0 = střed, 1 = pravý kraj.
 * Dopad těsně za krajem (míček přesahuje pálku) se omezí na -1 / 1.
 */
fun paddleHitOffset(ballX: Float, paddleX: Float, paddleWidth: Float): Float {
    val halfWidth = paddleWidth / 2f
    val center = paddleX + halfWidth
    return ((ballX - center) / halfWidth).coerceIn(-1f, 1f)
}

/** True, když se kruh (střed cx, cy, poloměr r) překrývá s obdélníkem. */
fun circleIntersectsRect(cx: Float, cy: Float, r: Float, left: Float, top: Float, right: Float, bottom: Float): Boolean {
    // Nejbližší bod obdélníku ke středu kruhu
    val nearestX = cx.coerceIn(left, right)
    val nearestY = cy.coerceIn(top, bottom)
    val dx = cx - nearestX
    val dy = cy - nearestY
    return sqrt(dx * dx + dy * dy) <= r
}

package com.example.bouranizdi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Testy pohybu míčku a odrazů od stěn. */
class BallTest {

    private val screenW = 1000f
    private val screenH = 2000f

    private fun ballAt(x: Float, y: Float, vx: Float, vy: Float) = Ball(screenW, screenH).also {
        it.x = x; it.y = y; it.vx = vx; it.vy = vy
    }

    @Test
    fun move_isVelocityTimesDt() {
        val ball = ballAt(100f, 100f, 200f, -300f)
        ball.move(0.1f)
        assertEquals(120f, ball.x, 1e-4f)
        assertEquals(70f, ball.y, 1e-4f)
    }

    @Test
    fun move_isFrameRateIndependent() {
        val a = ballAt(500f, 500f, 300f, -400f)
        val b = ballAt(500f, 500f, 300f, -400f)
        repeat(10) { a.move(0.01f) }
        b.move(0.1f)
        assertEquals(b.x, a.x, 1e-3f)
        assertEquals(b.y, a.y, 1e-3f)
    }

    @Test
    fun launch_goesUpWithConfiguredSpeed() {
        val ball = Ball(screenW, screenH)
        ball.launch()
        assertTrue(ball.vy < 0f)
        assertTrue(ball.vx > 0f)
        val speed = kotlin.math.sqrt(ball.vx * ball.vx + ball.vy * ball.vy)
        assertEquals(ball.speed, speed, 1e-2f)
    }

    @Test
    fun leftWall_bouncesRightAndStaysInside() {
        val ball = ballAt(5f, 500f, -300f, 100f)
        ball.checkWallCollision(screenW)
        assertEquals(ball.radius, ball.x, 1e-4f)
        assertEquals(300f, ball.vx, 1e-4f)
        assertEquals(100f, ball.vy, 1e-4f)
    }

    @Test
    fun rightWall_bouncesLeft() {
        val ball = ballAt(screenW - 2f, 500f, 300f, 100f)
        ball.checkWallCollision(screenW)
        assertEquals(screenW - ball.radius, ball.x, 1e-4f)
        assertEquals(-300f, ball.vx, 1e-4f)
    }

    @Test
    fun topWall_bouncesDown() {
        val ball = ballAt(500f, 3f, 100f, -300f)
        ball.checkWallCollision(screenW)
        assertEquals(ball.radius, ball.y, 1e-4f)
        assertEquals(300f, ball.vy, 1e-4f)
    }

    @Test
    fun corner_bouncesInBothAxes() {
        val ball = ballAt(1f, 1f, -300f, -300f)
        ball.checkWallCollision(screenW)
        assertEquals(300f, ball.vx, 1e-4f)
        assertEquals(300f, ball.vy, 1e-4f)
        assertEquals(ball.radius, ball.x, 1e-4f)
        assertEquals(ball.radius, ball.y, 1e-4f)
    }

    @Test
    fun alreadyBouncedAwayFromWall_doesNotFlipBack() {
        // Míček je ještě ve stěně, ale už letí pryč – nesmí se otočit zpátky.
        val ball = ballAt(2f, 500f, 300f, 100f)
        ball.checkWallCollision(screenW)
        assertEquals(300f, ball.vx, 1e-4f)
    }

    @Test
    fun bottom_doesNotBounce() {
        val ball = ballAt(500f, screenH + 100f, 0f, 300f)
        ball.checkWallCollision(screenW)
        assertEquals(300f, ball.vy, 1e-4f)
    }

    @Test
    fun isBelowBottom_onlyWhenFullyBelow() {
        val ball = Ball(screenW, screenH)
        ball.y = screenH
        assertFalse(ball.isBelowBottom(screenH))
        ball.y = screenH + ball.radius + 1f
        assertTrue(ball.isBelowBottom(screenH))
    }

    @Test
    fun placeOnPaddle_sitsOnTopCenter() {
        val paddle = Paddle(screenW, screenH)
        val ball = Ball(screenW, screenH)
        ball.placeOnPaddle(paddle)
        assertEquals(paddle.x + paddle.width / 2f, ball.x, 1e-4f)
        assertEquals(paddle.y - ball.radius, ball.y, 1e-4f)
    }

    @Test
    fun substepCount_splitsLongMoves() {
        assertEquals(1, substepCount(5f, 10f))
        assertEquals(1, substepCount(10f, 10f))
        assertEquals(3, substepCount(25f, 10f))
        assertEquals(1, substepCount(0f, 10f))
        assertEquals(1, substepCount(25f, 0f))
    }
}

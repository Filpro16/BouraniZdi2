package com.example.bouranizdi

import org.junit.Assert.assertEquals
import org.junit.Test

/** Testy pohybu pálky a jejího udržení na obrazovce. */
class PaddleTest {

    private val screenW = 1000f
    private val screenH = 2000f
    private val paddleW = 200f
    private val speed = 1500f

    @Test
    fun noTilt_doesNotMove() {
        assertEquals(400f, movePaddleX(400f, 0f, speed, 0.016f, paddleW, screenW), 1e-4f)
    }

    @Test
    fun tiltRight_movesRightByTiltTimesSpeedTimesDt() {
        // 0.5 × 1500 px/s × 0.1 s = 75 px
        assertEquals(475f, movePaddleX(400f, 0.5f, speed, 0.1f, paddleW, screenW), 1e-4f)
    }

    @Test
    fun tiltLeft_movesLeft() {
        assertEquals(250f, movePaddleX(400f, -1f, speed, 0.1f, paddleW, screenW), 1e-4f)
    }

    @Test
    fun atLeftEdge_staysOnScreen() {
        assertEquals(0f, movePaddleX(10f, -1f, speed, 0.05f, paddleW, screenW), 0f)
        assertEquals(0f, movePaddleX(0f, -1f, speed, 0.05f, paddleW, screenW), 0f)
    }

    @Test
    fun atRightEdge_staysOnScreen() {
        val maxX = screenW - paddleW
        assertEquals(maxX, movePaddleX(maxX - 5f, 1f, speed, 0.05f, paddleW, screenW), 0f)
        assertEquals(maxX, movePaddleX(maxX, 1f, speed, 0.05f, paddleW, screenW), 0f)
    }

    @Test
    fun atEdge_canMoveBackAway() {
        assertEquals(75f, movePaddleX(0f, 0.5f, speed, 0.1f, paddleW, screenW), 1e-4f)
    }

    @Test
    fun movementIsFrameRateIndependent() {
        // 10 kroků po 0,01 s = 1 krok po 0,1 s
        var x = 400f
        repeat(10) { x = movePaddleX(x, 0.3f, speed, 0.01f, paddleW, screenW) }
        assertEquals(movePaddleX(400f, 0.3f, speed, 0.1f, paddleW, screenW), x, 1e-3f)
    }

    @Test
    fun newPaddle_isCenteredAndRelativeToScreen() {
        val paddle = Paddle(screenW, screenH)
        assertEquals(screenW * Paddle.WIDTH_RATIO, paddle.width, 1e-4f)
        assertEquals((screenW - paddle.width) / 2f, paddle.x, 1e-4f)
        assert(paddle.y + paddle.height < screenH)
    }
}

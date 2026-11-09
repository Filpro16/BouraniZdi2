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

/** Testy odrazu míčku od pálky. */
class BallPaddleTest {

    private val screenW = 1000f
    private val screenH = 2000f
    private val paddle = Paddle(screenW, screenH)
    private val centerX = paddle.x + paddle.width / 2f

    /** Míček letící dolů, který se právě dotýká horní hrany pálky v bodě x. */
    private fun fallingBallTouchingPaddleAt(x: Float) = Ball(screenW, screenH).also {
        it.x = x; it.y = paddle.y - it.radius + 1f; it.vx = 0f; it.vy = it.speed
    }

    private fun speedOf(ball: Ball) = kotlin.math.sqrt(ball.vx * ball.vx + ball.vy * ball.vy)

    @Test
    fun hitCenter_bouncesStraightUp() {
        val ball = fallingBallTouchingPaddleAt(centerX)
        assertTrue(ball.checkPaddleCollision(paddle))
        assertEquals(0f, ball.vx, 1e-3f)
        assertEquals(-ball.speed, ball.vy, 1e-3f)
        assertEquals(paddle.y - ball.radius, ball.y, 1e-4f)
    }

    @Test
    fun hitRightSide_bouncesRight_hitLeftSide_bouncesLeft() {
        val right = fallingBallTouchingPaddleAt(centerX + paddle.width / 4f)
        val left = fallingBallTouchingPaddleAt(centerX - paddle.width / 4f)
        right.checkPaddleCollision(paddle)
        left.checkPaddleCollision(paddle)
        assertTrue(right.vx > 0f && right.vy < 0f)
        assertTrue(left.vx < 0f && left.vy < 0f)
        assertEquals(right.vx, -left.vx, 1e-3f)
    }

    @Test
    fun hitEdge_bouncesAtMaxAngle_andKeepsSpeed() {
        val ball = fallingBallTouchingPaddleAt(paddle.x + paddle.width)
        ball.checkPaddleCollision(paddle)
        val angle = Math.toDegrees(kotlin.math.atan2(ball.vx, -ball.vy).toDouble())
        assertEquals(Ball.MAX_BOUNCE_ANGLE_DEG.toDouble(), angle, 1e-2)
        assertEquals(ball.speed, speedOf(ball), 1e-2f)
    }

    @Test
    fun hitJustPastEdge_stillBounces_angleClamped() {
        // Střed míčku je mimo pálku, ale okraj míčku se jí dotkne.
        val ball = fallingBallTouchingPaddleAt(paddle.x - 5f)
        assertTrue(ball.checkPaddleCollision(paddle))
        val angle = Math.toDegrees(kotlin.math.atan2(ball.vx, -ball.vy).toDouble())
        assertEquals(-Ball.MAX_BOUNCE_ANGLE_DEG.toDouble(), angle, 1e-2)
    }

    @Test
    fun ballMovingUp_isIgnored() {
        val ball = fallingBallTouchingPaddleAt(centerX).also { it.vy = -it.speed }
        assertFalse(ball.checkPaddleCollision(paddle))
        assertEquals(-ball.speed, ball.vy, 0f)
    }

    @Test
    fun ballMissingPaddle_isIgnored() {
        val ball = fallingBallTouchingPaddleAt(paddle.x - 100f)
        assertFalse(ball.checkPaddleCollision(paddle))
    }

    @Test
    fun ballAlreadyBelowPaddle_isNotRescued() {
        val ball = fallingBallTouchingPaddleAt(centerX).also { it.y = paddle.y + paddle.height + 2f }
        assertFalse(ball.checkPaddleCollision(paddle))
    }

    @Test
    fun paddleHitOffset_mapsPositionToMinusOneOne() {
        assertEquals(0f, paddleHitOffset(150f, 100f, 100f), 1e-4f)
        assertEquals(-1f, paddleHitOffset(100f, 100f, 100f), 1e-4f)
        assertEquals(1f, paddleHitOffset(200f, 100f, 100f), 1e-4f)
        assertEquals(0.5f, paddleHitOffset(175f, 100f, 100f), 1e-4f)
        assertEquals(1f, paddleHitOffset(260f, 100f, 100f), 1e-4f)
    }

    @Test
    fun circleIntersectsRect_cases() {
        // uvnitř, dotyk hranou, mimo, mimo u rohu (diagonálně), dotyk rohem
        assertTrue(circleIntersectsRect(50f, 50f, 5f, 0f, 0f, 100f, 100f))
        assertTrue(circleIntersectsRect(105f, 50f, 5f, 0f, 0f, 100f, 100f))
        assertFalse(circleIntersectsRect(106f, 50f, 5f, 0f, 0f, 100f, 100f))
        assertFalse(circleIntersectsRect(104f, 104f, 5f, 0f, 0f, 100f, 100f))
        assertTrue(circleIntersectsRect(103f, 103f, 5f, 0f, 0f, 100f, 100f))
    }
}

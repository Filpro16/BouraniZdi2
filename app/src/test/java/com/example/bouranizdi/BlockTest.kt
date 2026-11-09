package com.example.bouranizdi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Testy zdi z bloků a odrazů míčku od bloků. */
class BlockTest {

    private val screenW = 1000f
    private val screenH = 2000f

    /** Blok 100 × 40 px s levým horním rohem v (400, 400). */
    private fun block() = Block(400f, 400f, 100f, 40f)

    private fun ballAt(x: Float, y: Float, vx: Float, vy: Float) = Ball(screenW, screenH).also {
        it.x = x; it.y = y; it.vx = vx; it.vy = vy
    }

    @Test
    fun grid_hasAllBlocks_insideScreen_withoutOverlap() {
        val blocks = createBlockGrid(screenW, screenH)
        assertEquals(Block.ROWS * Block.COLUMNS, blocks.size)
        for (b in blocks) {
            assertTrue(b.left >= 0f && b.right <= screenW && b.top >= 0f && b.bottom < screenH / 2f)
        }
        for (i in blocks.indices) for (j in i + 1 until blocks.size) {
            val a = blocks[i]; val b = blocks[j]
            val overlap = a.left < b.right && b.left < a.right && a.top < b.bottom && b.top < a.bottom
            assertFalse("bloky $i a $j se překrývají", overlap)
        }
    }

    @Test
    fun grid_isRelativeToScreenSize() {
        val small = createBlockGrid(500f, 1000f)
        val big = createBlockGrid(1000f, 2000f)
        assertEquals(small.last().right * 2f, big.last().right, 1e-3f)
        assertEquals(small.last().bottom * 2f, big.last().bottom, 1e-3f)
    }

    @Test
    fun destroy_marksBlockDead_andCountDrops() {
        val blocks = listOf(block(), Block(0f, 0f, 10f, 10f))
        assertEquals(2, countAliveBlocks(blocks))
        blocks[0].destroy()
        assertFalse(blocks[0].isAlive)
        assertEquals(1, countAliveBlocks(blocks))
        blocks[1].destroy()
        assertEquals(0, countAliveBlocks(blocks))
    }

    @Test
    fun hitFromBelow_bouncesDown() {
        val ball = ballAt(450f, 440f + 10f, 50f, -300f)
        assertTrue(ball.checkBlockCollision(block()))
        assertEquals(300f, ball.vy, 1e-4f)
        assertEquals(50f, ball.vx, 1e-4f)
    }

    @Test
    fun hitFromAbove_bouncesUp() {
        val ball = ballAt(450f, 400f - 10f, 50f, 300f)
        assertTrue(ball.checkBlockCollision(block()))
        assertEquals(-300f, ball.vy, 1e-4f)
        assertEquals(50f, ball.vx, 1e-4f)
    }

    @Test
    fun hitFromLeftSide_bouncesLeft() {
        val ball = ballAt(400f - 10f, 420f, 300f, -50f)
        assertTrue(ball.checkBlockCollision(block()))
        assertEquals(-300f, ball.vx, 1e-4f)
        assertEquals(-50f, ball.vy, 1e-4f)
    }

    @Test
    fun hitFromRightSide_bouncesRight() {
        val ball = ballAt(500f + 10f, 420f, -300f, -50f)
        assertTrue(ball.checkBlockCollision(block()))
        assertEquals(300f, ball.vx, 1e-4f)
    }

    @Test
    fun hitCornerDiagonally_bouncesInBothAxes() {
        // Míček se dotkne levého spodního rohu stejně hluboko v obou osách.
        val ball = Ball(screenW, screenH)
        val d = ball.radius * 0.6f
        ball.x = 400f - d; ball.y = 440f + d; ball.vx = 300f; ball.vy = -300f
        assertTrue(ball.checkBlockCollision(block()))
        assertEquals(-300f, ball.vx, 1e-4f)
        assertEquals(300f, ball.vy, 1e-4f)
    }

    @Test
    fun deadBlock_isIgnored() {
        val b = block().also { it.destroy() }
        val ball = ballAt(450f, 450f, 0f, -300f)
        assertFalse(ball.checkBlockCollision(b))
        assertEquals(-300f, ball.vy, 0f)
    }

    @Test
    fun missingBlock_isIgnored() {
        val ball = ballAt(300f, 300f, 0f, -300f)
        assertFalse(ball.checkBlockCollision(block()))
    }
}

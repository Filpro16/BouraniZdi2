package com.example.bouranizdi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Testy pravidel hry: stavy, životy, skóre, výhra. */
class GameRulesTest {

    @Test
    fun tap_movesThroughStates() {
        assertEquals(GameState.PLAYING, stateAfterTap(GameState.READY))
        assertEquals(GameState.PLAYING, stateAfterTap(GameState.PLAYING))
        assertEquals(GameState.READY, stateAfterTap(GameState.LIFE_LOST))
        assertEquals(GameState.READY, stateAfterTap(GameState.GAME_OVER))
        assertEquals(GameState.READY, stateAfterTap(GameState.WIN))
    }

    @Test
    fun onlyGameOverAndWin_finishTheGame() {
        assertTrue(isGameFinished(GameState.GAME_OVER))
        assertTrue(isGameFinished(GameState.WIN))
        assertFalse(isGameFinished(GameState.READY))
        assertFalse(isGameFinished(GameState.PLAYING))
        assertFalse(isGameFinished(GameState.LIFE_LOST))
    }

    @Test
    fun ballLost_withLivesLeft_isLifeLost() {
        assertEquals(GameState.LIFE_LOST, stateAfterBallLost(2))
        assertEquals(GameState.LIFE_LOST, stateAfterBallLost(1))
    }

    @Test
    fun ballLost_onLastLife_isGameOver() {
        assertEquals(GameState.GAME_OVER, stateAfterBallLost(0))
        assertEquals(GameState.GAME_OVER, stateAfterBallLost(-1))
    }

    @Test
    fun threeLives_meansThirdLossIsGameOver() {
        var lives = GameView.START_LIVES
        val states = List(3) { lives -= 1; stateAfterBallLost(lives) }
        assertEquals(listOf(GameState.LIFE_LOST, GameState.LIFE_LOST, GameState.GAME_OVER), states)
    }

    @Test
    fun blockDestroyed_lastBlockWins() {
        assertEquals(GameState.PLAYING, stateAfterBlockDestroyed(5))
        assertEquals(GameState.PLAYING, stateAfterBlockDestroyed(1))
        assertEquals(GameState.WIN, stateAfterBlockDestroyed(0))
    }

    @Test
    fun destroyingAllBlocks_winsWithFullScore() {
        val blocks = createBlockGrid(1000f, 2000f)
        var score = 0
        var state = GameState.PLAYING
        for (block in blocks) {
            assertEquals(GameState.PLAYING, state)
            block.destroy()
            score = scoreAfterBlockDestroyed(score)
            state = stateAfterBlockDestroyed(countAliveBlocks(blocks))
        }
        assertEquals(GameState.WIN, state)
        assertEquals(blocks.size * GameView.POINTS_PER_BLOCK, score)
    }

    @Test
    fun score_addsTenPerBlock() {
        assertEquals(10, scoreAfterBlockDestroyed(0))
        assertEquals(60, scoreAfterBlockDestroyed(50))
    }

    @Test
    fun messages_matchState() {
        assertTrue(stateMessage(GameState.PLAYING, 3, 0).isEmpty())
        assertEquals("Konec hry", stateMessage(GameState.GAME_OVER, 0, 120).first())
        assertTrue(stateMessage(GameState.GAME_OVER, 0, 120).contains("Skóre: 120"))
        assertTrue(stateMessage(GameState.LIFE_LOST, 2, 30).contains("Zbývá životů: 2"))
        assertEquals("Vyhrál jsi!", stateMessage(GameState.WIN, 1, 350).first())
    }
}

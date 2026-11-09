package com.example.bouranizdi

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView

/**
 * Herní plocha: SurfaceView s vlastním vláknem herní smyčky.
 * Smyčka opakuje update (posun herních objektů o delta time) a draw (vykreslení na Canvas).
 */
class GameView(
    context: Context,
    private val tiltController: TiltController
) : SurfaceView(context), SurfaceHolder.Callback {

    companion object {
        /** Barva pozadí herní plochy. */
        const val BACKGROUND_COLOR = Color.BLACK

        /** Maximální délka jednoho kroku v sekundách (ochrana proti skoku po pauze / zaseknutí). */
        const val MAX_DELTA_SECONDS = 0.05f

        /** Zobrazit počítadlo snímků za sekundu (ladění). */
        const val SHOW_FPS = true

        /** Velikost textu FPS jako podíl šířky obrazovky. */
        const val FPS_TEXT_SIZE_RATIO = 0.04f

        /** Počet životů na začátku hry. */
        const val START_LIVES = 3

        /** Body za jeden zničený blok. */
        const val POINTS_PER_BLOCK = 10

        /** Velikost textu skóre, životů a podtitulků zpráv jako podíl šířky obrazovky. */
        const val HUD_TEXT_SIZE_RATIO = 0.05f

        /** Velikost nadpisu zprávy (např. „Konec hry“) jako podíl šířky obrazovky. */
        const val TITLE_TEXT_SIZE_RATIO = 0.08f

        /** Svislá pozice řádku se skóre a životy jako podíl výšky obrazovky (pod případným výřezem kamery). */
        const val HUD_Y_RATIO = 0.065f

        /** Svislá pozice zprávy o stavu hry jako podíl výšky obrazovky. */
        const val MESSAGE_Y_RATIO = 0.5f
    }

    /** Vlákno herní smyčky; null, když hra neběží. */
    private var gameThread: GameThread? = null

    /**
     * Velikost plochy hlášená ze surfaceChanged (UI vlákno). Herní vlákno si ji převezme
     * a podle ní vytvoří herní objekty – všechny herní objekty tak mění jen herní vlákno.
     */
    @Volatile
    private var surfaceSize: Pair<Int, Int>? = null

    /** Rozměry plochy v pixelech, pro které jsou vytvořené herní objekty (0 = ještě nevytvořené). */
    private var screenWidth = 0
    private var screenHeight = 0

    /** Herní objekty; vzniknou v createGameObjects(), až známe rozměry obrazovky. */
    private lateinit var paddle: Paddle
    private lateinit var ball: Ball
    private lateinit var blocks: List<Block>

    /** Stav hry, životy a skóre; mění je jen herní vlákno. */
    private var state = GameState.READY
    private var lives = START_LIVES
    private var score = 0

    /** Hráč klepl na obrazovku (nastaví UI vlákno, zpracuje herní vlákno). */
    @Volatile
    private var tapRequested = false

    /** Aktuální odhad FPS pro ladicí výpis. */
    private var fps = 0f

    private val textPaint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
    }

    /** Text skóre, životů a podtitulků zpráv; zarovnání se nastavuje při kreslení. */
    private val hudPaint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
    }

    /** Nadpis zprávy o stavu hry, vycentrovaný. */
    private val titlePaint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }

    init {
        holder.addCallback(this)
        // Výchozí formát SurfaceView je 16bitový RGB_565, který se na některých
        // zařízeních / emulátorech zobrazí jako černá plocha. 32bitový formát funguje všude.
        holder.setFormat(PixelFormat.RGBA_8888)
    }

    // ---------- Řízení životního cyklu (volá MainActivity) ----------

    /** Spustí herní smyčku. Volat z Activity.onResume. */
    fun resume() {
        if (gameThread != null) return
        gameThread = GameThread(holder).also { it.start() }
    }

    /** Čistě zastaví herní smyčku a počká na ukončení vlákna. Volat z Activity.onPause. */
    fun pause() {
        val thread = gameThread ?: return
        thread.requestStop()
        joinQuietly(thread)
        gameThread = null
    }

    /** Počká na ukončení vlákna; přerušení jen předá dál. */
    private fun joinQuietly(thread: Thread) {
        try {
            thread.join()
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    // ---------- SurfaceHolder.Callback ----------

    override fun surfaceCreated(holder: SurfaceHolder) {
        // Vlákno se spouští v resume(); kreslí až ve chvíli, kdy je surface platný.
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        surfaceSize = Pair(width, height)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // Surface zaniká – smyčka musí skončit dřív, než se pokusí kreslit.
        pause()
    }

    // ---------- Dotyk ----------

    /** Klepnutí jen zaznamená; zpracuje ho herní vlákno v příštím update (žádné souběžné změny stavu). */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            tapRequested = true
            performClick()
        }
        return true
    }

    override fun performClick(): Boolean = super.performClick()

    // ---------- Herní logika ----------

    /** Posune hru o dt sekund. */
    private fun update(dt: Float) {
        updateFps(dt)
        if (!applySurfaceSize()) return
        handleTap()
        movePaddle(dt)
        when (state) {
            GameState.READY -> ball.placeOnPaddle(paddle)
            GameState.PLAYING -> moveBall(dt)
            else -> Unit // LIFE_LOST / GAME_OVER / WIN: míček stojí
        }
    }

    /** Zpracuje klepnutí, pokud nějaké přišlo. */
    private fun handleTap() {
        if (!tapRequested) return
        tapRequested = false
        onTap()
    }

    /** Klepnutí: READY → odpálí míček, LIFE_LOST → READY, GAME_OVER / WIN → nová hra. */
    private fun onTap() {
        val previous = state
        state = stateAfterTap(previous)
        if (isGameFinished(previous)) resetGame()
        if (previous == GameState.READY) ball.launch()
    }

    /** Nová hra: plné životy, nulové skóre, nová zeď. Míček se v READY položí na pálku. */
    private fun resetGame() {
        lives = START_LIVES
        score = 0
        blocks = createBlockGrid(screenWidth.toFloat(), screenHeight.toFloat())
    }

    /**
     * Když se změnila velikost plochy, vytvoří herní objekty pro novou velikost.
     * Při návratu z pozadí (stejná velikost) se hra neresetuje.
     * Vrací false, dokud velikost plochy ještě neznáme.
     */
    private fun applySurfaceSize(): Boolean {
        val (width, height) = surfaceSize ?: return false
        if (width != screenWidth || height != screenHeight) createGameObjects(width, height)
        return true
    }

    /** Vytvoří herní objekty relativně k rozměrům obrazovky. */
    private fun createGameObjects(width: Int, height: Int) {
        screenWidth = width
        screenHeight = height
        textPaint.textSize = width * FPS_TEXT_SIZE_RATIO
        hudPaint.textSize = width * HUD_TEXT_SIZE_RATIO
        titlePaint.textSize = width * TITLE_TEXT_SIZE_RATIO
        paddle = Paddle(width.toFloat(), height.toFloat())
        ball = Ball(width.toFloat(), height.toFloat())
        resetGame()
        state = GameState.READY
    }

    /** Posune pálku podle aktuálního náklonu telefonu. */
    private fun movePaddle(dt: Float) {
        paddle.move(tiltController.tilt, dt)
    }

    /** Posune míček o dt; pohyb rozdělí na malé kroky, aby míček neproskočil skrz objekty. */
    private fun moveBall(dt: Float) {
        val steps = substepCount(ball.speed * dt, ball.radius)
        val stepDt = dt / steps
        for (i in 0 until steps) {
            if (state != GameState.PLAYING) return // míček spadl nebo padl poslední blok
            updateBallStep(stepDt)
        }
    }

    /** Jeden malý krok míčku: pohyb, odrazy od stěn, pálky a bloků, kontrola pádu pod spodní okraj. */
    private fun updateBallStep(dt: Float) {
        ball.move(dt)
        ball.checkWallCollision(screenWidth.toFloat())
        ball.checkPaddleCollision(paddle)
        checkBlockCollisions()
        if (state == GameState.PLAYING && ball.isBelowBottom(screenHeight.toFloat())) loseLife()
    }

    /** Míček spadl: ubere život a přejde do LIFE_LOST, nebo při posledním životě do GAME_OVER. */
    private fun loseLife() {
        lives -= 1
        state = stateAfterBallLost(lives)
        ball.stop()
    }

    /** Zničí první blok, do kterého míček narazil (v jednom kroku nejvýš jeden, aby se odraz nezrušil dvojím otočením). */
    private fun checkBlockCollisions() {
        val hitBlock = blocks.firstOrNull { ball.checkBlockCollision(it) } ?: return
        hitBlock.destroy()
        score = scoreAfterBlockDestroyed(score)
        state = stateAfterBlockDestroyed(countAliveBlocks(blocks))
        if (state == GameState.WIN) ball.stop()
    }

    /** Vyhlazený odhad FPS z délky snímku. */
    private fun updateFps(dt: Float) {
        if (dt > 0f) fps = fps * 0.9f + (1f / dt) * 0.1f
    }

    // ---------- Kreslení ----------

    /** Vykreslí celý snímek. */
    private fun drawGame(canvas: Canvas) {
        canvas.drawColor(BACKGROUND_COLOR)
        if (screenWidth > 0) {
            drawObjects(canvas)
            drawHud(canvas)
            drawStateMessage(canvas)
        }
        if (SHOW_FPS) drawFps(canvas)
    }

    /** Vykreslí herní objekty. */
    private fun drawObjects(canvas: Canvas) {
        drawBlocks(canvas)
        paddle.draw(canvas)
        ball.draw(canvas)
    }

    /** Vykreslí všechny bloky (zničené se samy přeskočí). */
    private fun drawBlocks(canvas: Canvas) {
        for (block in blocks) block.draw(canvas)
    }

    /** Vypíše skóre doprostřed a životy vpravo nahoru. */
    private fun drawHud(canvas: Canvas) {
        val y = screenHeight * HUD_Y_RATIO
        hudPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("Skóre: $score", screenWidth / 2f, y, hudPaint)
        hudPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Životy: $lives", screenWidth - hudPaint.textSize / 2f, y, hudPaint)
    }

    /** Vypíše zprávu pro aktuální stav (první řádek jako nadpis, další menším písmem). */
    private fun drawStateMessage(canvas: Canvas) {
        val lines = stateMessage(state, lives, score)
        if (lines.isEmpty()) return
        val centerX = screenWidth / 2f
        var y = screenHeight * MESSAGE_Y_RATIO
        canvas.drawText(lines.first(), centerX, y, titlePaint)
        hudPaint.textAlign = Paint.Align.CENTER
        for (line in lines.drop(1)) {
            y += hudPaint.textSize * 1.6f
            canvas.drawText(line, centerX, y, hudPaint)
        }
    }

    /** Vypíše FPS do levého horního rohu. */
    private fun drawFps(canvas: Canvas) {
        canvas.drawText("FPS: ${fps.toInt()}", textPaint.textSize, textPaint.textSize * 2, textPaint)
    }

    // ---------- Vlákno herní smyčky ----------

    /** Vlákno, které opakovaně volá update a draw, dokud není zastaveno. */
    private inner class GameThread(private val surfaceHolder: SurfaceHolder) : Thread("GameThread") {

        @Volatile
        private var running = true

        fun requestStop() {
            running = false
        }

        override fun run() {
            var lastTimeNs = System.nanoTime()
            while (running) {
                val nowNs = System.nanoTime()
                val dt = computeDeltaSeconds(lastTimeNs, nowNs, MAX_DELTA_SECONDS)
                lastTimeNs = nowNs
                update(dt)
                drawFrame()
            }
        }

        /** Zamkne canvas, vykreslí snímek a odešle ho na obrazovku. */
        private fun drawFrame() {
            if (!surfaceHolder.surface.isValid) return
            val canvas = surfaceHolder.lockCanvas() ?: return
            try {
                drawGame(canvas)
            } finally {
                surfaceHolder.unlockCanvasAndPost(canvas)
            }
        }
    }
}

/**
 * Spočítá delta time v sekundách mezi dvěma časy v nanosekundách.
 * Výsledek je omezen na rozsah 0..maxSeconds, aby hra po pauze neudělala obří skok.
 */
fun computeDeltaSeconds(lastNs: Long, nowNs: Long, maxSeconds: Float): Float {
    val seconds = (nowNs - lastNs) / 1_000_000_000f
    return seconds.coerceIn(0f, maxSeconds)
}

// ---------- Pravidla hry (čisté funkce, testovatelné bez telefonu) ----------

/** Stavy hry. */
enum class GameState { READY, PLAYING, LIFE_LOST, GAME_OVER, WIN }

/** Stav po klepnutí: READY → PLAYING, LIFE_LOST / GAME_OVER / WIN → READY, při hraní se nic nemění. */
fun stateAfterTap(state: GameState): GameState = when (state) {
    GameState.READY -> GameState.PLAYING
    GameState.PLAYING -> GameState.PLAYING
    GameState.LIFE_LOST, GameState.GAME_OVER, GameState.WIN -> GameState.READY
}

/** True, když hra skončila a další klepnutí začne novou hru. */
fun isGameFinished(state: GameState): Boolean =
    state == GameState.GAME_OVER || state == GameState.WIN

/** Stav po pádu míčku podle počtu zbývajících životů. */
fun stateAfterBallLost(livesLeft: Int): GameState =
    if (livesLeft <= 0) GameState.GAME_OVER else GameState.LIFE_LOST

/** Stav po zničení bloku: když už žádný nezbývá, hráč vyhrál. */
fun stateAfterBlockDestroyed(aliveBlocks: Int): GameState =
    if (aliveBlocks <= 0) GameState.WIN else GameState.PLAYING

/** Nové skóre po zničení jednoho bloku. */
fun scoreAfterBlockDestroyed(score: Int, points: Int = GameView.POINTS_PER_BLOCK): Int = score + points

/** Řádky zprávy pro daný stav (první je nadpis); při hraní žádná zpráva. */
fun stateMessage(state: GameState, lives: Int, score: Int): List<String> = when (state) {
    GameState.READY -> listOf("Připraven?", "Klepni pro odpálení")
    GameState.PLAYING -> emptyList()
    GameState.LIFE_LOST -> listOf("Míček spadl!", "Zbývá životů: $lives", "Klepni pro pokračování")
    GameState.GAME_OVER -> listOf("Konec hry", "Skóre: $score", "Klepni pro novou hru")
    GameState.WIN -> listOf("Vyhrál jsi!", "Skóre: $score", "Klepni pro novou hru")
}

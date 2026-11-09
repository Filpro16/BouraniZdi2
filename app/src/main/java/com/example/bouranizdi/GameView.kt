package com.example.bouranizdi

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
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

    /** Aktuální odhad FPS pro ladicí výpis. */
    private var fps = 0f

    private val textPaint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
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

    // ---------- Herní logika ----------

    /** Posune hru o dt sekund. */
    private fun update(dt: Float) {
        updateFps(dt)
        if (!applySurfaceSize()) return
        movePaddle(dt)
        moveBall(dt)
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
        paddle = Paddle(width.toFloat(), height.toFloat())
        ball = Ball(width.toFloat(), height.toFloat())
        resetBall()
    }

    /** Posune pálku podle aktuálního náklonu telefonu. */
    private fun movePaddle(dt: Float) {
        paddle.move(tiltController.tilt, dt)
    }

    /** Položí míček na pálku a odpálí ho. */
    private fun resetBall() {
        ball.placeOnPaddle(paddle)
        ball.launch()
    }

    /** Posune míček o dt; pohyb rozdělí na malé kroky, aby míček neproskočil skrz objekty. */
    private fun moveBall(dt: Float) {
        val steps = substepCount(ball.speed * dt, ball.radius)
        val stepDt = dt / steps
        repeat(steps) { updateBallStep(stepDt) }
    }

    /** Jeden malý krok míčku: pohyb, odrazy od stěn a pálky, kontrola pádu pod spodní okraj. */
    private fun updateBallStep(dt: Float) {
        ball.move(dt)
        ball.checkWallCollision(screenWidth.toFloat())
        ball.checkPaddleCollision(paddle)
        if (ball.isBelowBottom(screenHeight.toFloat())) resetBall()
    }

    /** Vyhlazený odhad FPS z délky snímku. */
    private fun updateFps(dt: Float) {
        if (dt > 0f) fps = fps * 0.9f + (1f / dt) * 0.1f
    }

    // ---------- Kreslení ----------

    /** Vykreslí celý snímek. */
    private fun drawGame(canvas: Canvas) {
        canvas.drawColor(BACKGROUND_COLOR)
        if (screenWidth > 0) drawObjects(canvas)
        if (SHOW_FPS) drawFps(canvas)
    }

    /** Vykreslí herní objekty. */
    private fun drawObjects(canvas: Canvas) {
        paddle.draw(canvas)
        ball.draw(canvas)
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

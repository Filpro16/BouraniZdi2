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

    /** Rozměry plochy v pixelech (nastaví se v surfaceChanged). */
    private var screenWidth = 0
    private var screenHeight = 0

    /** Pálka; vznikne, až známe rozměry obrazovky (surfaceChanged). */
    @Volatile
    private var paddle: Paddle? = null

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
        screenWidth = width
        screenHeight = height
        textPaint.textSize = width * FPS_TEXT_SIZE_RATIO
        paddle = Paddle(width.toFloat(), height.toFloat())
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // Surface zaniká – smyčka musí skončit dřív, než se pokusí kreslit.
        pause()
    }

    // ---------- Herní logika ----------

    /** Posune hru o dt sekund. */
    private fun update(dt: Float) {
        updateFps(dt)
        movePaddle(dt)
    }

    /** Posune pálku podle aktuálního náklonu telefonu. */
    private fun movePaddle(dt: Float) {
        paddle?.move(tiltController.tilt, dt)
    }

    /** Vyhlazený odhad FPS z délky snímku. */
    private fun updateFps(dt: Float) {
        if (dt > 0f) fps = fps * 0.9f + (1f / dt) * 0.1f
    }

    // ---------- Kreslení ----------

    /** Vykreslí celý snímek. */
    private fun drawGame(canvas: Canvas) {
        canvas.drawColor(BACKGROUND_COLOR)
        paddle?.draw(canvas)
        if (SHOW_FPS) drawFps(canvas)
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

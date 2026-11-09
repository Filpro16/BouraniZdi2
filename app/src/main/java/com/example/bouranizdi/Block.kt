package com.example.bouranizdi

import android.graphics.Canvas
import android.graphics.Paint

/**
 * Jeden blok ve zdi. Pozice a rozměry jsou v pixelech (spočítané relativně k obrazovce v [createBlockGrid]).
 */
class Block(val left: Float, val top: Float, val width: Float, val height: Float) {

    companion object {
        /** Počet řad bloků. */
        const val ROWS = 5

        /** Počet bloků v jedné řadě. */
        const val COLUMNS = 7

        /** Horní hrana první řady jako podíl výšky obrazovky (nad ní je místo pro skóre a životy). */
        const val GRID_TOP_RATIO = 0.10f

        /** Výška bloku jako podíl výšky obrazovky. */
        const val HEIGHT_RATIO = 0.03f

        /** Mezera mezi bloky jako podíl šířky obrazovky. */
        const val GAP_RATIO = 0.012f

        /** Okraj zdi od levé a pravé stěny jako podíl šířky obrazovky. */
        const val SIDE_MARGIN_RATIO = 0.02f

        /** Barva bloku. */
        const val COLOR = 0xFFE65A3C.toInt()
    }

    val right = left + width
    val bottom = top + height
    val centerX = left + width / 2f
    val centerY = top + height / 2f

    /** False, když byl blok zničen. */
    var isAlive = true
        private set

    /** Paint se vytvoří až při prvním kreslení (unit testy kreslení nevolají). */
    private val paint by lazy { Paint().apply { color = COLOR; isAntiAlias = true } }

    /** Zničí blok. */
    fun destroy() {
        isAlive = false
    }

    /** Vykreslí blok (zničený blok se nekreslí). */
    fun draw(canvas: Canvas) {
        if (isAlive) canvas.drawRect(left, top, right, bottom, paint)
    }
}

/** Vytvoří zeď [Block.ROWS] × [Block.COLUMNS] bloků rozmístěnou relativně k velikosti obrazovky. */
fun createBlockGrid(screenWidth: Float, screenHeight: Float): List<Block> {
    val gap = screenWidth * Block.GAP_RATIO
    val margin = screenWidth * Block.SIDE_MARGIN_RATIO
    val blockWidth = (screenWidth - 2f * margin - (Block.COLUMNS - 1) * gap) / Block.COLUMNS
    val blockHeight = screenHeight * Block.HEIGHT_RATIO
    val gridTop = screenHeight * Block.GRID_TOP_RATIO
    val blocks = ArrayList<Block>(Block.ROWS * Block.COLUMNS)
    for (row in 0 until Block.ROWS) {
        for (column in 0 until Block.COLUMNS) {
            val left = margin + column * (blockWidth + gap)
            val top = gridTop + row * (blockHeight + gap)
            blocks.add(Block(left, top, blockWidth, blockHeight))
        }
    }
    return blocks
}

/** Počet ještě nezničených bloků. */
fun countAliveBlocks(blocks: List<Block>): Int = blocks.count { it.isAlive }

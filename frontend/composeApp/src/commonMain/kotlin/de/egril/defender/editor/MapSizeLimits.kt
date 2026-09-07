package de.egril.defender.editor

object MapSizeLimits {
    const val MAX_WIDTH = 500
    const val MAX_HEIGHT = 500
    const val MAX_TILE_COUNT = MAX_WIDTH * MAX_HEIGHT

    fun isWithinLimits(
        width: Int,
        height: Int,
    ): Boolean =
        width > 0 &&
            height > 0 &&
            width <= MAX_WIDTH &&
            height <= MAX_HEIGHT &&
            width.toLong() * height.toLong() <= MAX_TILE_COUNT.toLong()
}

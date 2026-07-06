package de.egril.defender.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import de.egril.defender.model.AttackerType
import de.egril.defender.model.HexDirection
import de.egril.defender.ui.settings.AppSettings
import defender_of_egril.composeapp.generated.resources.Res
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Provides directional sprite painters for enemy units.
 *
 * ## Spritesheet convention
 * Each enemy type has a single spritesheet PNG stored at:
 *
 * `composeResources/files/sprites/{key}.png`
 *
 * where `{key}` is `sprite_` followed by a canonical lower-case name for the type (see [spriteKey]).
 * The sheet is a **3 × 3 grid** of equally-sized frames laid out as:
 *
 * ```
 *  SE  |  S   |  SW
 *  E   | Ctr  |  W
 *  NE  |  N   |  NW
 * ```
 *
 * Pass a [HexDirection] to [frameBounds] / [rememberEnemySpritePainter] to select the matching
 * directional cell.  Pass `null` to select the **Center** cell, which is the neutral portrait used
 * in the enemy info area.
 *
 * The provider degrades gracefully: it returns `null` when sprites are disabled in settings or when
 * the spritesheet for the requested enemy is not present, letting callers fall back to the drawn
 * vector icon.
 */
object EnemySpriteProvider {
    /** Number of columns in each spritesheet grid. */
    const val GRID_COLS: Int = 3

    /** Number of rows in each spritesheet grid. */
    const val GRID_ROWS: Int = 3

    // Cache of decoded spritesheets keyed by sprite key. `null` means "already tried, not available".
    private val sheetCache = mutableMapOf<String, ImageBitmap?>()

    /**
     * Maps an [AttackerType] to the sprite file key (the PNG file name without extension).
     *
     * The key is `sprite_` followed by a canonical lower-case name.  Most types use the lower-cased
     * [AttackerType] name directly, but a few have dedicated filenames that predate the enum names.
     */
    fun spriteKey(type: AttackerType): String =
        "sprite_" +
            when (type) {
                AttackerType.ORK -> "orc"
                AttackerType.EVIL_WIZARD -> "evil_mage"
                else -> type.name.lowercase()
            }

    /**
     * Loads and decodes the spritesheet for the given key, caching the result (including misses).
     *
     * The bytes are looked up under `files/sprites/` (the cross-platform raw-resource location used
     * elsewhere in the app). `files/sprites/` is the correct long-term location. For backward
     * compatibility, the Gradle build rewrites any legacy prepared `drawable/sprites/` resources
     * into `files/sprites/` before Compose resource accessor generation runs.
     */
    private suspend fun loadSheet(key: String): ImageBitmap? {
        if (sheetCache.containsKey(key)) return sheetCache[key]

        val bitmap: ImageBitmap? =
            try {
                val bytes = Res.readBytes("files/sprites/$key.png")
                MapImageProvider.decodeImageBitmap(bytes)
            } catch (_: Exception) {
                null
            }
        sheetCache[key] = bitmap
        return bitmap
    }

    /**
     * Computes the source rectangle (offset + size) of the frame within a spritesheet of the given
     * dimensions.
     *
     * The sheet is a [GRID_COLS] × [GRID_ROWS] (3 × 3) grid:
     * ```
     *  SE  |  S   |  SW
     *  E   | Ctr  |  W
     *  NE  |  N   |  NW
     * ```
     * Pass a [HexDirection] to select the corresponding cell.  Pass `null` to select the **Center**
     * cell (column 1, row 1), which is the neutral portrait used in the enemy info area.
     *
     * Returns the full-image bounds when the sheet is too small to contain a valid grid, so a
     * malformed sheet degrades to showing the whole image rather than crashing.
     */
    fun frameBounds(
        sheetWidth: Int,
        sheetHeight: Int,
        direction: HexDirection?,
    ): Pair<IntOffset, IntSize> {
        val frameWidth = sheetWidth / GRID_COLS
        val frameHeight = sheetHeight / GRID_ROWS
        if (frameWidth <= 0 || frameHeight <= 0) {
            return IntOffset.Zero to IntSize(sheetWidth, sheetHeight)
        }
        val (col, row) =
            when (direction) {
                null -> 1 to 1
                HexDirection.SE -> 0 to 0
                HexDirection.E -> 0 to 1
                HexDirection.NE -> 0 to 2
                HexDirection.NW -> 2 to 2
                HexDirection.W -> 2 to 1
                HexDirection.SW -> 2 to 0
            }
        return IntOffset(col * frameWidth, row * frameHeight) to IntSize(frameWidth, frameHeight)
    }

    /**
     * Builds a [Painter] that renders only the frame of [sheet] for the given [direction].
     * Pass `null` for [direction] to render the center (neutral portrait) frame.
     */
    private fun framePainter(
        sheet: ImageBitmap,
        direction: HexDirection?,
    ): Painter {
        val (srcOffset, srcSize) = frameBounds(sheet.width, sheet.height, direction)
        return BitmapPainter(sheet, srcOffset = srcOffset, srcSize = srcSize)
    }

    /**
     * Composable that resolves the directional sprite [Painter] for an enemy [type].
     *
     * Pass a [HexDirection] to display the frame for that travel direction.  Pass `null` to display
     * the center (neutral portrait) frame, which is suitable for the enemy info area.
     *
     * Returns `null` when enemy sprites are disabled or when no spritesheet is available for the
     * enemy, so callers can fall back to the drawn icon.
     */
    @Composable
    fun rememberEnemySpritePainter(
        type: AttackerType,
        direction: HexDirection?,
    ): Painter? {
        val useSprites = AppSettings.useSprites.value
        val key = spriteKey(type)

        var sheet by remember(key, useSprites) {
            mutableStateOf(
                if (useSprites) {
                    sheetCache[key]
                } else {
                    null
                },
            )
        }

        LaunchedEffect(key, useSprites) {
            if (!useSprites) {
                sheet = null
            } else {
                val cached = sheetCache[key]
                if (cached != null || sheetCache.containsKey(key)) {
                    sheet = cached
                } else {
                    sheet = withContext(Dispatchers.Default) { loadSheet(key) }
                }
            }
        }

        val currentSheet = sheet
        return if (useSprites && currentSheet != null) {
            remember(currentSheet, direction) { framePainter(currentSheet, direction) }
        } else {
            null
        }
    }
}
